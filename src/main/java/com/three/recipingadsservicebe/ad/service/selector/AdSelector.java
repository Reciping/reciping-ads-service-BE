package com.three.recipingadsservicebe.ad.service.selector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.log.dto.LogType;
import com.three.recipingadsservicebe.log.logger.AdLogger;
import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.AbTestScenarioType;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import com.three.recipingadsservicebe.segment.service.ABTestManager;
import com.three.recipingadsservicebe.segment.service.SegmentCalculatorUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdSelector {

    private final ABTestManager abTestManager;
    private final SegmentCalculatorUtil segmentCalculator;
    private final AdRepository adRepository;
    private final ObjectMapper objectMapper;

    /**
     * 🔧 새 메서드: 폴백 정보를 포함한 상세 결과 반환
     */
    public AdSelectionResult getAllAdsForUserWithDetails(UserInfoDto userInfo) {
        Map<String, List<Ad>> adsByPosition = new HashMap<>();
        Map<String, AdSelectionResult.PositionSelectionInfo> selectionInfo = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : SegmentType.GENERAL_ALL;

        log.info("🎯 광고 선택 시작: userId={}, 세그먼트={}",
                userInfo != null ? userInfo.getUserId() : "GUEST", userSegment.name());

        Map<AdPosition, AbTestScenarioType> positionScenarios = calculateAllPositionScenarios(userInfo);

        for (AdPosition position : AdPosition.getActivePositions()) {
            try {
                long startTime = System.currentTimeMillis();
                AbTestScenarioType originalScenario = positionScenarios.get(position);

                // 🔧 폴백 추적을 위한 변수들
                String finalScenarioCode = originalScenario.getScenarioCode();
                boolean isFallback = false;
                int fallbackLevel = 0;
                List<String> fallbackSteps = new ArrayList<>();
                fallbackSteps.add(originalScenario.getScenarioCode()); // 원본 시나리오 추가

                // 원본 시나리오로 광고 조회
                List<Ad> candidateAds = adRepository.findByScenarioCodeAndPosition(
                        originalScenario.getScenarioCode(), position
                );

                log.info("📊 광고 조회 결과 - Position: {}, Scenario: {}, 후보 광고 수: {}",
                        position, originalScenario.getScenarioCode(), candidateAds.size());

                List<Ad> selectedAds = selectBestAdsOptimized(candidateAds, userSegment, position);

                log.info("✅ 필터링 결과 - Position: {}, 선택된 광고 수: {}",
                        position, selectedAds.size());

                // 🔧 폴백 처리 (상세 정보 추적)
                if (selectedAds.isEmpty()) {
                    log.warn("⚠️ Fallback 시작 - Position: {}, Original Scenario: {}",
                            position, originalScenario.getScenarioCode());

                    AdFallbackResult fallbackResult = performFallbackStrategyDetailed(
                            userSegment, position, originalScenario.getScenarioCode());

                    selectedAds = fallbackResult.getSelectedAds();
                    finalScenarioCode = fallbackResult.getFinalScenarioCode();
                    isFallback = fallbackResult.isUsedFallback();
                    fallbackLevel = fallbackResult.getFallbackLevel();
                    fallbackSteps.addAll(fallbackResult.getFallbackSteps());
                }

                long endTime = System.currentTimeMillis();
                adsByPosition.put(position.name(), selectedAds);

                // 🔧 상세 선택 정보 저장
                selectionInfo.put(position.name(), AdSelectionResult.PositionSelectionInfo.builder()
                        .position(position.name())
                        .originalScenario(originalScenario.getScenarioCode())
                        .finalScenario(finalScenarioCode)
                        .isFallback(isFallback)
                        .fallbackLevel(fallbackLevel)
                        .fallbackSteps(fallbackSteps)
                        .candidateCount(candidateAds.size())
                        .selectedCount(selectedAds.size())
                        .selectionTimeMs(endTime - startTime)
                        .build());

                log.info("📌 위치 [{}] 완료: {}개 광고 선택 ({}ms), 시나리오: {} → {}, Fallback: {}",
                        position.name(), selectedAds.size(), endTime - startTime,
                        originalScenario.getScenarioCode(), finalScenarioCode, isFallback);

            } catch (Exception e) {
                log.error("❌ 위치 [{}] 광고 선택 실패", position.name(), e);
                adsByPosition.put(position.name(), List.of());

                // 에러 정보도 포함
                selectionInfo.put(position.name(), AdSelectionResult.PositionSelectionInfo.builder()
                        .position(position.name())
                        .originalScenario("ERROR")
                        .finalScenario("ERROR")
                        .isFallback(false)
                        .fallbackLevel(0)
                        .fallbackSteps(List.of("ERROR: " + e.getMessage()))
                        .candidateCount(0)
                        .selectedCount(0)
                        .selectionTimeMs(0)
                        .build());

                logSelectionError(position, userSegment, e);
            }
        }

        // 전체 통계 로깅
        Map<String, Object> selectionStats = new HashMap<>();
        selectionStats.put("userSegment", userSegment.name());
        selectionStats.put("userId", userInfo != null ? userInfo.getUserId() : "GUEST");
        selectionStats.put("totalAds", adsByPosition.values().stream().mapToInt(List::size).sum());
        selectionStats.put("positions", selectionInfo);

        log.info("🎉 광고 선택 완료 - 전체 통계: {}", selectionStats);
        logSelectionResult(selectionStats);

        return AdSelectionResult.builder()
                .adsByPosition(adsByPosition)
                .selectionInfo(selectionInfo)
                .userSegment(userSegment)
                .totalAds(adsByPosition.values().stream().mapToInt(List::size).sum())
                .build();
    }

    /**
     * 🔧 개선된 폴백 전략 (상세 정보 반환)
     */
    private AdFallbackResult performFallbackStrategyDetailed(SegmentType userSegment,
                                                             AdPosition position,
                                                             String originalScenario) {
        log.info("🔄 Fallback 전략 시작 - 세그먼트: {}, 위치: {}, 원본 시나리오: {}",
                userSegment.name(), position.name(), originalScenario);

        List<String> fallbackSteps = new ArrayList<>();
        int fallbackLevel = 0;

        try {
            AbTestScenarioType defaultScenario = AbTestScenarioType.getDefaultScenario();
            String defaultScenarioCode = defaultScenario.getScenarioCode();

            // 이미 기본 시나리오라면 더 이상 폴백할 곳이 없음
            if (originalScenario.equals(defaultScenarioCode)) {
                log.warn("⚠️ 이미 기본 시나리오임 - 더 이상 폴백 불가");
                return AdFallbackResult.builder()
                        .selectedAds(List.of())
                        .finalScenarioCode(originalScenario)
                        .usedFallback(false)
                        .fallbackLevel(0)
                        .fallbackSteps(List.of())
                        .build();
            }

            fallbackLevel = 1;
            fallbackSteps.add(defaultScenarioCode);

            log.debug("기본 시나리오로 Fallback 시도: {}", defaultScenarioCode);

            List<Ad> defaultAds = adRepository.findByScenarioCodeAndPosition(defaultScenarioCode, position);
            log.debug("기본 시나리오 광고 조회 결과: {}개", defaultAds.size());

            List<Ad> selectedAds = selectBestAdsOptimized(defaultAds, userSegment, position);

            Map<String, Object> fallbackInfo = new HashMap<>();
            fallbackInfo.put("userSegment", userSegment.name());
            fallbackInfo.put("position", position.name());
            fallbackInfo.put("originalScenario", originalScenario);
            fallbackInfo.put("fallbackSteps", fallbackSteps);
            fallbackInfo.put("fallbackLevel", fallbackLevel);

            if (!selectedAds.isEmpty()) {
                fallbackInfo.put("result", "SUCCESS");
                fallbackInfo.put("finalScenario", defaultScenarioCode);
                fallbackInfo.put("selectedCount", selectedAds.size());
                log.info("✅ Fallback 성공: {}개 광고 선택됨", selectedAds.size());

                logFallbackEvent(fallbackInfo, "SUCCESS");

                return AdFallbackResult.builder()
                        .selectedAds(selectedAds)
                        .finalScenarioCode(defaultScenarioCode)
                        .usedFallback(true)
                        .fallbackLevel(fallbackLevel)
                        .fallbackSteps(fallbackSteps)
                        .build();
            } else {
                log.warn("⚠️ Fallback 실패: 기본 시나리오에서도 광고를 찾지 못함");
                fallbackInfo.put("result", "FAILED");
                logFallbackEvent(fallbackInfo, "FAILED");
            }

        } catch (Exception e) {
            log.error("❌ 기본 시나리오 Fallback 실패", e);
        }

        log.warn("❌ 최종 Fallback 실패 - 광고 없음");
        return AdFallbackResult.builder()
                .selectedAds(List.of())
                .finalScenarioCode(originalScenario)
                .usedFallback(false)
                .fallbackLevel(fallbackLevel)
                .fallbackSteps(fallbackSteps)
                .build();
    }

    /**
     * 🔧 기존 메서드 유지 (하위 호환성)
     */
    public Map<String, List<Ad>> getAllAdsForUser(UserInfoDto userInfo) {
        AdSelectionResult result = getAllAdsForUserWithDetails(userInfo);
        return result.getAdsByPosition();
    }

    // 🔧 DTO 클래스들 추가
    public static class AdSelectionResult {
        private Map<String, List<Ad>> adsByPosition;
        private Map<String, PositionSelectionInfo> selectionInfo;
        private SegmentType userSegment;
        private int totalAds;

        // Builder 패턴
        public static AdSelectionResultBuilder builder() {
            return new AdSelectionResultBuilder();
        }

        // Getters
        public Map<String, List<Ad>> getAdsByPosition() { return adsByPosition; }
        public Map<String, PositionSelectionInfo> getSelectionInfo() { return selectionInfo; }
        public SegmentType getUserSegment() { return userSegment; }
        public int getTotalAds() { return totalAds; }

        public static class AdSelectionResultBuilder {
            private Map<String, List<Ad>> adsByPosition;
            private Map<String, PositionSelectionInfo> selectionInfo;
            private SegmentType userSegment;
            private int totalAds;

            public AdSelectionResultBuilder adsByPosition(Map<String, List<Ad>> adsByPosition) {
                this.adsByPosition = adsByPosition;
                return this;
            }

            public AdSelectionResultBuilder selectionInfo(Map<String, PositionSelectionInfo> selectionInfo) {
                this.selectionInfo = selectionInfo;
                return this;
            }

            public AdSelectionResultBuilder userSegment(SegmentType userSegment) {
                this.userSegment = userSegment;
                return this;
            }

            public AdSelectionResultBuilder totalAds(int totalAds) {
                this.totalAds = totalAds;
                return this;
            }

            public AdSelectionResult build() {
                AdSelectionResult result = new AdSelectionResult();
                result.adsByPosition = this.adsByPosition;
                result.selectionInfo = this.selectionInfo;
                result.userSegment = this.userSegment;
                result.totalAds = this.totalAds;
                return result;
            }
        }

        public static class PositionSelectionInfo {
            private String position;
            private String originalScenario;
            private String finalScenario;
            private boolean isFallback;
            private int fallbackLevel;
            private List<String> fallbackSteps;
            private int candidateCount;
            private int selectedCount;
            private long selectionTimeMs;

            public static PositionSelectionInfoBuilder builder() {
                return new PositionSelectionInfoBuilder();
            }

            // Getters
            public String getPosition() { return position; }
            public String getOriginalScenario() { return originalScenario; }
            public String getFinalScenario() { return finalScenario; }
            public boolean isFallback() { return isFallback; }
            public int getFallbackLevel() { return fallbackLevel; }
            public List<String> getFallbackSteps() { return fallbackSteps; }
            public int getCandidateCount() { return candidateCount; }
            public int getSelectedCount() { return selectedCount; }
            public long getSelectionTimeMs() { return selectionTimeMs; }

            @Override
            public String toString() {
                return String.format(
                        "PositionSelectionInfo{position='%s', originalScenario='%s', finalScenario='%s', " +
                                "isFallback=%s, fallbackLevel=%d, candidateCount=%d, selectedCount=%d, selectionTimeMs=%d}",
                        position, originalScenario, finalScenario, isFallback, fallbackLevel,
                        candidateCount, selectedCount, selectionTimeMs
                );
            }

            public static class PositionSelectionInfoBuilder {
                private String position;
                private String originalScenario;
                private String finalScenario;
                private boolean isFallback;
                private int fallbackLevel;
                private List<String> fallbackSteps;
                private int candidateCount;
                private int selectedCount;
                private long selectionTimeMs;

                public PositionSelectionInfoBuilder position(String position) {
                    this.position = position;
                    return this;
                }

                public PositionSelectionInfoBuilder originalScenario(String originalScenario) {
                    this.originalScenario = originalScenario;
                    return this;
                }

                public PositionSelectionInfoBuilder finalScenario(String finalScenario) {
                    this.finalScenario = finalScenario;
                    return this;
                }

                public PositionSelectionInfoBuilder isFallback(boolean isFallback) {
                    this.isFallback = isFallback;
                    return this;
                }

                public PositionSelectionInfoBuilder fallbackLevel(int fallbackLevel) {
                    this.fallbackLevel = fallbackLevel;
                    return this;
                }

                public PositionSelectionInfoBuilder fallbackSteps(List<String> fallbackSteps) {
                    this.fallbackSteps = fallbackSteps;
                    return this;
                }

                public PositionSelectionInfoBuilder candidateCount(int candidateCount) {
                    this.candidateCount = candidateCount;
                    return this;
                }

                public PositionSelectionInfoBuilder selectedCount(int selectedCount) {
                    this.selectedCount = selectedCount;
                    return this;
                }

                public PositionSelectionInfoBuilder selectionTimeMs(long selectionTimeMs) {
                    this.selectionTimeMs = selectionTimeMs;
                    return this;
                }

                public PositionSelectionInfo build() {
                    PositionSelectionInfo info = new PositionSelectionInfo();
                    info.position = this.position;
                    info.originalScenario = this.originalScenario;
                    info.finalScenario = this.finalScenario;
                    info.isFallback = this.isFallback;
                    info.fallbackLevel = this.fallbackLevel;
                    info.fallbackSteps = this.fallbackSteps;
                    info.candidateCount = this.candidateCount;
                    info.selectedCount = this.selectedCount;
                    info.selectionTimeMs = this.selectionTimeMs;
                    return info;
                }
            }
        }
    }

    private static class AdFallbackResult {
        private List<Ad> selectedAds;
        private String finalScenarioCode;
        private boolean usedFallback;
        private int fallbackLevel;
        private List<String> fallbackSteps;

        public static AdFallbackResultBuilder builder() {
            return new AdFallbackResultBuilder();
        }

        // Getters
        public List<Ad> getSelectedAds() { return selectedAds; }
        public String getFinalScenarioCode() { return finalScenarioCode; }
        public boolean isUsedFallback() { return usedFallback; }
        public int getFallbackLevel() { return fallbackLevel; }
        public List<String> getFallbackSteps() { return fallbackSteps; }

        public static class AdFallbackResultBuilder {
            private List<Ad> selectedAds;
            private String finalScenarioCode;
            private boolean usedFallback;
            private int fallbackLevel;
            private List<String> fallbackSteps;

            public AdFallbackResultBuilder selectedAds(List<Ad> selectedAds) {
                this.selectedAds = selectedAds;
                return this;
            }

            public AdFallbackResultBuilder finalScenarioCode(String finalScenarioCode) {
                this.finalScenarioCode = finalScenarioCode;
                return this;
            }

            public AdFallbackResultBuilder usedFallback(boolean usedFallback) {
                this.usedFallback = usedFallback;
                return this;
            }

            public AdFallbackResultBuilder fallbackLevel(int fallbackLevel) {
                this.fallbackLevel = fallbackLevel;
                return this;
            }

            public AdFallbackResultBuilder fallbackSteps(List<String> fallbackSteps) {
                this.fallbackSteps = fallbackSteps;
                return this;
            }

            public AdFallbackResult build() {
                AdFallbackResult result = new AdFallbackResult();
                result.selectedAds = this.selectedAds;
                result.finalScenarioCode = this.finalScenarioCode;
                result.usedFallback = this.usedFallback;
                result.fallbackLevel = this.fallbackLevel;
                result.fallbackSteps = this.fallbackSteps;
                return result;
            }
        }
    }

    // 나머지 기존 메서드들은 그대로 유지...
    private Map<AdPosition, AbTestScenarioType> calculateAllPositionScenarios(UserInfoDto userInfo) {
        Map<AdPosition, AbTestScenarioType> scenarios = new HashMap<>();
        for (AdPosition position : AdPosition.getActivePositions()) {
            AbTestScenarioType scenario = abTestManager.assignScenario(userInfo, position);
            scenarios.put(position, scenario);
        }
        return scenarios;
    }

    private List<Ad> selectBestAdsOptimized(List<Ad> candidateAds, SegmentType userSegment, AdPosition position) {
        if (candidateAds.isEmpty()) {
            log.debug("후보 광고가 없음 - Position: {}", position);
            return List.of();
        }

        log.debug("🔍 광고 필터링 시작 - 후보: {}개, 사용자 세그먼트: {}, 위치: {}",
                candidateAds.size(), userSegment, position);

        List<Ad> segmentFiltered = candidateAds.stream()
                .filter(ad -> isSegmentMatchedOptimized(ad, userSegment))
                .collect(Collectors.toList());

        List<Ad> eligibleAds = segmentFiltered.stream()
                .filter(this::isAdEligible)
                .collect(Collectors.toList());

        List<Ad> selectedAds = eligibleAds.stream()
                .sorted(this::compareAds)
                .limit(position.getSlotCount())
                .collect(Collectors.toList());

        log.info("✨ 필터링 완료 - 최종 선택: {}개 (from {} → {} → {} → {})",
                selectedAds.size(), candidateAds.size(), segmentFiltered.size(),
                eligibleAds.size(), selectedAds.size());

        return selectedAds;
    }

    private boolean isAdEligible(Ad ad) {
        if (ad.getStatus() != AdStatus.ACTIVE) return false;

        if (ad.getBudget() != null && ad.getSpentAmount() != null) {
            if (ad.getSpentAmount() >= ad.getBudget()) return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (ad.getStartAt() != null && now.isBefore(ad.getStartAt())) return false;
        if (ad.getEndAt() != null && now.isAfter(ad.getEndAt())) return false;

        return true;
    }

    private int compareAds(Ad a1, Ad a2) {
        int scoreCompare = Float.compare(
                a2.getScore() != null ? a2.getScore() : 0f,
                a1.getScore() != null ? a1.getScore() : 0f
        );
        if (scoreCompare != 0) return scoreCompare;

        int ctrCompare = Float.compare(a2.calculateCTR(), a1.calculateCTR());
        if (ctrCompare != 0) return ctrCompare;

        return a2.getCreatedAt().compareTo(a1.getCreatedAt());
    }

    private boolean isSegmentMatchedOptimized(Ad ad, SegmentType userSegment) {
        SegmentType adTargetSegment = ad.getTargetSegment();
        if (adTargetSegment == null || adTargetSegment == SegmentType.GENERAL_ALL) return true;
        if (userSegment == null) userSegment = SegmentType.GENERAL_ALL;
        return adTargetSegment.equals(userSegment);
    }

    // 기존 로깅 메서드들 유지...
    private void logFallbackEvent(Map<String, Object> fallbackInfo, String result) {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                fallbackInfo.put("result", result);
                fallbackInfo.put("timestamp", System.currentTimeMillis());

                AdLogger.track(
                        log,
                        LogType.AD_FALLBACK,
                        request.getRequestURI(),
                        request.getMethod(),
                        null,
                        null,
                        null,
                        objectMapper.writeValueAsString(fallbackInfo),
                        request
                );
            }
        } catch (Exception e) {
            log.warn("Fallback 이벤트 로깅 실패: {}", e.getMessage());
        }
    }

    private void logSelectionResult(Map<String, Object> stats) {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                AdLogger.track(
                        log,
                        LogType.AD_PERFORMANCE,
                        request.getRequestURI(),
                        request.getMethod(),
                        null,
                        null,
                        null,
                        objectMapper.writeValueAsString(stats),
                        request
                );
            }
        } catch (Exception e) {
            log.warn("선택 결과 로깅 실패: {}", e.getMessage());
        }
    }

    private void logSelectionError(AdPosition position, SegmentType userSegment, Exception error) {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("position", position.name());
                errorInfo.put("userSegment", userSegment.name());
                errorInfo.put("error", error.getMessage());
                errorInfo.put("errorType", error.getClass().getSimpleName());

                AdLogger.track(
                        log,
                        LogType.AD_FALLBACK,
                        request.getRequestURI(),
                        request.getMethod(),
                        null,
                        null,
                        null,
                        objectMapper.writeValueAsString(errorInfo),
                        request
                );
            }
        } catch (Exception e) {
            log.warn("에러 로깅 실패: {}", e.getMessage());
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getSelectionStats(UserInfoDto userInfo) {
        Map<String, Object> stats = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : SegmentType.GENERAL_ALL;

        stats.put("userSegment", userSegment.name());
        stats.put("activePositions", AdPosition.getActivePositions().length);

        Map<String, Object> positionAdCounts = new HashMap<>();
        for (AdPosition position : AdPosition.getActivePositions()) {
            AbTestScenarioType scenarioType = abTestManager.assignScenario(userInfo, position);
            List<Ad> ads = adRepository.findByScenarioCodeAndPosition(
                    scenarioType.getScenarioCode(), position
            );

            Map<String, Object> posInfo = new HashMap<>();
            posInfo.put("scenario", scenarioType.getScenarioCode());
            posInfo.put("totalCount", ads.size());
            posInfo.put("activeCount", ads.stream()
                    .filter(ad -> ad.getStatus() == AdStatus.ACTIVE)
                    .count());

            positionAdCounts.put(position.name(), posInfo);
        }
        stats.put("positionDetails", positionAdCounts);

        return stats;
    }
}