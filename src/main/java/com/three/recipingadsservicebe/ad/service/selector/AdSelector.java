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
     * 🔥 개선점 1: 성능 최적화
     * 현재 문제: 각 위치마다 DB 쿼리를 별도로 실행
     * 해결책: 배치 쿼리로 한 번에 조회
     */
    public Map<String, List<Ad>> getAllAdsForUser(UserInfoDto userInfo) {
        Map<String, List<Ad>> result = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : SegmentType.GENERAL_ALL;

        log.debug("광고 선택 시작: 사용자 세그먼트={}", userSegment.name());

        // 🔥 개선: 모든 활성 위치의 시나리오를 한 번에 계산
        Map<AdPosition, AbTestScenarioType> positionScenarios = calculateAllPositionScenarios(userInfo);

        // 🔥 개선: 필요한 모든 시나리오 코드를 배치로 조회
        Set<String> scenarioCodes = positionScenarios.values().stream()
                .map(AbTestScenarioType::getScenarioCode)
                .collect(Collectors.toSet());

        // 배치 쿼리가 없는 경우 기존 방식 사용
        Map<String, List<Ad>> adsByScenario = getAdsByScenarioCodes(scenarioCodes);

        // 통계 수집용
        Map<String, Object> selectionStats = new HashMap<>();
        selectionStats.put("userSegment", userSegment.name());
        selectionStats.put("userId", userInfo != null ? userInfo.getUserId() : "GUEST");
        Map<String, Object> positionStats = new HashMap<>();

        for (AdPosition position : AdPosition.getActivePositions()) {
            try {
                long startTime = System.currentTimeMillis();

                AbTestScenarioType scenario = positionScenarios.get(position);
                List<Ad> candidateAds = adsByScenario.getOrDefault(scenario.getScenarioCode(), List.of());

                // 🔥 개선: 필터링 로직 최적화
                List<Ad> selectedAds = selectBestAdsOptimized(candidateAds, userSegment, position);

                // Fallback 처리
                if (selectedAds.isEmpty()) {
                    selectedAds = performFallbackStrategy(userSegment, position, scenario.getScenarioCode());
                }

                long endTime = System.currentTimeMillis();
                result.put(position.name(), selectedAds);

                // 위치별 통계
                Map<String, Object> stats = new HashMap<>();
                stats.put("adCount", selectedAds.size());
                stats.put("selectionTime", endTime - startTime);
                stats.put("scenario", scenario.getScenarioCode());
                stats.put("hasFallback", selectedAds.stream().anyMatch(ad ->
                        !ad.getScenarioCode().equals(scenario.getScenarioCode())));
                positionStats.put(position.name(), stats);

                log.debug("위치 [{}]: {}개 광고 선택 완료 ({}ms), 시나리오: {}",
                        position.name(), selectedAds.size(), endTime - startTime, scenario.getScenarioCode());

            } catch (Exception e) {
                log.warn("위치 [{}] 광고 선택 실패: {}", position.name(), e.getMessage());
                result.put(position.name(), List.of());
                logSelectionError(position, userSegment, e);
            }
        }

        selectionStats.put("positions", positionStats);
        selectionStats.put("totalAds", result.values().stream().mapToInt(List::size).sum());
        logSelectionResult(selectionStats);

        return result;
    }

    /**
     * 🔥 개선점 2: 시나리오 계산 최적화
     */
    private Map<AdPosition, AbTestScenarioType> calculateAllPositionScenarios(UserInfoDto userInfo) {
        Map<AdPosition, AbTestScenarioType> scenarios = new HashMap<>();

        for (AdPosition position : AdPosition.getActivePositions()) {
            AbTestScenarioType scenario = abTestManager.assignScenario(userInfo, position);
            scenarios.put(position, scenario);
        }

        return scenarios;
    }

    /**
     * 배치 쿼리 대체 메서드 (AdRepository에 배치 메서드가 없는 경우)
     */
    private Map<String, List<Ad>> getAdsByScenarioCodes(Set<String> scenarioCodes) {
        Map<String, List<Ad>> result = new HashMap<>();

        for (String scenarioCode : scenarioCodes) {
            // 각 시나리오별로 모든 위치의 광고를 조회
            List<Ad> ads = new ArrayList<>();
            for (AdPosition position : AdPosition.getActivePositions()) {
                List<Ad> positionAds = adRepository.findByScenarioCodeAndPosition(scenarioCode, position);
                ads.addAll(positionAds);
            }
            result.put(scenarioCode, ads);
        }

        return result;
    }

    /**
     * 🔥 개선점 3: 선택 알고리즘 최적화
     */
    private List<Ad> selectBestAdsOptimized(List<Ad> candidateAds, SegmentType userSegment, AdPosition position) {
        if (candidateAds.isEmpty()) {
            return List.of();
        }

        // Phase 1 특화: 간단하고 명확한 선택 로직
        return candidateAds.stream()
                .filter(ad -> isSegmentMatchedOptimized(ad, userSegment))
                .filter(this::isAdEligible) // 🔥 추가: 광고 자격 검증
                .sorted(this::compareAds) // 🔥 개선: 정렬 로직 최적화
                .limit(position.getSlotCount())
                .collect(Collectors.toList());
    }

    /**
     * 🔥 개선점 4: 광고 자격 검증 추가
     */
    private boolean isAdEligible(Ad ad) {
        // 활성 상태 체크
        if (ad.getStatus() != AdStatus.ACTIVE) {
            return false;
        }

        // 예산 체크
        if (ad.getBudget() != null && ad.getSpentAmount() != null) {
            if (ad.getSpentAmount() >= ad.getBudget()) {
                log.debug("광고 ID {} 예산 소진: spent={}, budget={}",
                        ad.getId(), ad.getSpentAmount(), ad.getBudget());
                return false;
            }
        }

        // 기간 체크
        LocalDateTime now = LocalDateTime.now();
        if (ad.getStartAt() != null && now.isBefore(ad.getStartAt())) {
            return false;
        }
        if (ad.getEndAt() != null && now.isAfter(ad.getEndAt())) {
            return false;
        }

        return true;
    }

    /**
     * 🔥 개선점 5: 정렬 로직 개선
     */
    private int compareAds(Ad a1, Ad a2) {
        // 1순위: 스코어 (높은 순)
        int scoreCompare = Float.compare(
                a2.getScore() != null ? a2.getScore() : 0f,
                a1.getScore() != null ? a1.getScore() : 0f
        );
        if (scoreCompare != 0) return scoreCompare;

        // 2순위: CTR (높은 순)
        int ctrCompare = Float.compare(a2.calculateCTR(), a1.calculateCTR());
        if (ctrCompare != 0) return ctrCompare;

        // 3순위: 생성일 (최신 순)
        return a2.getCreatedAt().compareTo(a1.getCreatedAt());
    }

    /**
     * 🔥 개선점 6: 세그먼트 매칭 최적화
     */
    private boolean isSegmentMatchedOptimized(Ad ad, SegmentType userSegment) {
        SegmentType adTargetSegment = ad.getTargetSegment();

        // null이거나 GENERAL_ALL이면 모든 사용자에게 노출
        if (adTargetSegment == null || adTargetSegment == SegmentType.GENERAL_ALL) {
            return true;
        }

        // 사용자 세그먼트가 null이면 GENERAL_ALL로 처리
        if (userSegment == null) {
            userSegment = SegmentType.GENERAL_ALL;
        }

        boolean matched = adTargetSegment.equals(userSegment);

        log.trace("세그먼트 매칭: 광고타겟={}, 사용자={}, 결과={}",
                adTargetSegment.name(), userSegment.name(), matched);

        return matched;
    }

    /**
     * 🔥 개선점 7: Fallback 전략 단순화 (Phase 1에 맞춤)
     */
    private List<Ad> performFallbackStrategy(SegmentType userSegment, AdPosition position, String originalScenario) {
        log.debug("Fallback 전략 시작 - 세그먼트: {}, 위치: {}", userSegment.name(), position.name());

        Map<String, Object> fallbackInfo = new HashMap<>();
        fallbackInfo.put("userSegment", userSegment.name());
        fallbackInfo.put("position", position.name());
        fallbackInfo.put("originalScenario", originalScenario);

        // Phase 1 단순화: 기본 시나리오로 바로 fallback
        try {
            AbTestScenarioType defaultScenario = AbTestScenarioType.getDefaultScenario();
            List<Ad> defaultAds = adRepository.findByScenarioCodeAndPosition(
                    defaultScenario.getScenarioCode(), position
            );

            List<Ad> selectedAds = selectBestAdsOptimized(defaultAds, userSegment, position);

            if (!selectedAds.isEmpty()) {
                fallbackInfo.put("result", "SUCCESS");
                fallbackInfo.put("fallbackScenario", defaultScenario.getScenarioCode());
                logFallbackEvent(fallbackInfo, "SUCCESS");
                return selectedAds;
            }
        } catch (Exception e) {
            log.error("기본 시나리오 Fallback 실패: {}", e.getMessage());
        }

        // 최종: 빈 리스트
        fallbackInfo.put("result", "FAILED");
        logFallbackEvent(fallbackInfo, "FAILED");
        return List.of();
    }

    // ========== 로깅 헬퍼 메서드들 ==========

    /**
     * Fallback 이벤트 로깅
     */
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

    /**
     * 선택 결과 로깅
     */
    private void logSelectionResult(Map<String, Object> stats) {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                log.info("광고 선택 완료 - 통계: {}", objectMapper.writeValueAsString(stats));

                // 성능 통계 로깅 (선택적)
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

    /**
     * 선택 에러 로깅
     */
    private void logSelectionError(AdPosition position, SegmentType userSegment, Exception error) {
        try {
            HttpServletRequest request = getCurrentHttpRequest();
            if (request != null) {
                Map<String, Object> errorInfo = new HashMap<>();
                errorInfo.put("position", position.name());
                errorInfo.put("userSegment", userSegment.name());
                errorInfo.put("error", error.getMessage());
                errorInfo.put("errorType", error.getClass().getSimpleName());

                log.error("광고 선택 에러 - {}", objectMapper.writeValueAsString(errorInfo));

                // 에러 추적을 위한 로깅
                AdLogger.track(
                        log,
                        LogType.AD_FALLBACK, // 에러도 fallback으로 분류
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

    /**
     * 현재 HTTP 요청 획득
     */
    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 디버깅/모니터링을 위한 메서드
     */
    public Map<String, Object> getSelectionStats(UserInfoDto userInfo) {
        Map<String, Object> stats = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : SegmentType.GENERAL_ALL;

        stats.put("userSegment", userSegment.name());
        stats.put("activePositions", AdPosition.getActivePositions().length);

        Map<String, Integer> positionAdCounts = new HashMap<>();
        for (AdPosition position : AdPosition.getActivePositions()) {
            AbTestScenarioType scenarioType = abTestManager.assignScenario(userInfo, position);
            List<Ad> ads = adRepository.findByScenarioCodeAndPosition(
                    scenarioType.getScenarioCode(), position
            );
            positionAdCounts.put(position.name(), ads.size());
        }
        stats.put("positionAdCounts", positionAdCounts);

        return stats;
    }
}