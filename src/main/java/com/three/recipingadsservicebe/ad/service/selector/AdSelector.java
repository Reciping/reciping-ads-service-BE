package com.three.recipingadsservicebe.ad.service.selector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.three.recipingadsservicebe.abtest.entity.AbTestScenario;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
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

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdSelector {

    private final ABTestManager abTestManager;
    private final SegmentCalculatorUtil segmentCalculator;
    private final AdRepository adRepository;
    private final ObjectMapper objectMapper;

    public Map<String, List<Ad>> getAllAdsForUser(UserInfoDto userInfo) {
        Map<String, List<Ad>> result = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : SegmentType.GENERAL_ALL;

        log.debug("광고 선택 시작: 사용자 세그먼트={}", userSegment.name());

        // 통계 수집용
        Map<String, Object> selectionStats = new HashMap<>();
        selectionStats.put("userSegment", userSegment.name());
        selectionStats.put("userId", userInfo != null ? userInfo.getUserId() : "GUEST");

        Map<String, Object> positionStats = new HashMap<>();

        for (AdPosition position : AdPosition.getActivePositions()) {
            try {
                long startTime = System.currentTimeMillis();
                List<Ad> ads = selectAdsForPosition(userInfo, userSegment, position);
                long endTime = System.currentTimeMillis();

                result.put(position.name(), ads);

                // 위치별 통계
                Map<String, Object> stats = new HashMap<>();
                stats.put("adCount", ads.size());
                stats.put("selectionTime", endTime - startTime);
                stats.put("hasFallback", ads.stream().anyMatch(ad -> ad.getScenarioCode().equals("CONTROL")));
                positionStats.put(position.name(), stats);

                log.debug("위치 [{}]: {}개 광고 선택 완료 ({}ms)",
                        position.name(), ads.size(), endTime - startTime);

            } catch (Exception e) {
                log.warn("위치 [{}] 광고 선택 실패: {}", position.name(), e.getMessage());
                result.put(position.name(), List.of());

                // 에러 로깅
                logSelectionError(position, userSegment, e);
            }
        }

        selectionStats.put("positions", positionStats);
        selectionStats.put("totalAds", result.values().stream().mapToInt(List::size).sum());

        // 전체 선택 결과 로깅
        logSelectionResult(selectionStats);

        return result;
    }

    private List<Ad> selectAdsForPosition(UserInfoDto userInfo, SegmentType userSegment, AdPosition position) {
        // 1단계: Enum에서 시나리오 할당
        AbTestScenarioType scenarioType = abTestManager.assignScenario(userInfo, position);

        // 2단계: 시나리오 코드로 광고 조회
        List<Ad> candidateAds = adRepository.findByScenarioCodeAndPosition(
                scenarioType.getScenarioCode(), position
        );

        log.trace("위치 [{}] 후보 광고: {}개, 시나리오: {}",
                position.name(), candidateAds.size(), scenarioType.getScenarioCode());

        // 3단계: 세그먼트 매칭 및 선택
        List<Ad> selectedAds = selectBestAds(candidateAds, userSegment, position);

        // 4단계: 개선된 다단계 Fallback 처리
        if (selectedAds.isEmpty()) {
            selectedAds = performFallbackStrategy(userSegment, position, scenarioType.getScenarioCode());
        }

        return selectedAds;
    }

    private List<Ad> selectBestAds(List<Ad> candidateAds, SegmentType userSegment, AdPosition position) {
        if (candidateAds.isEmpty()) {
            return List.of();
        }

        // 세그먼트 매칭 필터링
        List<Ad> matchedAds = candidateAds.stream()
                .filter(ad -> isSegmentMatched(ad, userSegment))
                .sorted(Comparator.comparing(Ad::getScore).reversed())
                .limit(position.getSlotCount())
                .toList();

        // 매칭된 광고가 없으면 전체에서 선택
        if (matchedAds.isEmpty()) {
            log.debug("세그먼트 [{}] 매칭 실패 - 전체 후보에서 선택", userSegment.name());
            return candidateAds.stream()
                    .sorted(Comparator.comparing(Ad::getScore).reversed())
                    .limit(position.getSlotCount())
                    .toList();
        }

        return matchedAds;
    }

    // 개선: 다단계 Fallback 전략
    private List<Ad> performFallbackStrategy(SegmentType userSegment, AdPosition position, String originalScenario) {
        log.debug("Fallback 전략 시작 - 세그먼트: {}, 위치: {}, 원본 시나리오: {}",
                userSegment.name(), position.name(), originalScenario);

        // Fallback 로깅을 위한 정보 수집
        Map<String, Object> fallbackInfo = new HashMap<>();
        fallbackInfo.put("userSegment", userSegment.name());
        fallbackInfo.put("position", position.name());
        fallbackInfo.put("originalScenario", originalScenario);
        fallbackInfo.put("fallbackSteps", new ArrayList<>());

        List<Map<String, Object>> fallbackSteps = (List<Map<String, Object>>) fallbackInfo.get("fallbackSteps");

        // 1차 Fallback: CONTROL 시나리오
        List<Ad> controlAds = fallbackToControlScenario(userSegment, position);
        Map<String, Object> step1 = new HashMap<>();
        step1.put("level", 1);
        step1.put("strategy", "CONTROL_SCENARIO");
        step1.put("success", !controlAds.isEmpty());
        step1.put("adCount", controlAds.size());
        fallbackSteps.add(step1);

        if (!controlAds.isEmpty()) {
            log.debug("CONTROL 시나리오 Fallback 성공 - {}개 광고", controlAds.size());
            logFallbackEvent(fallbackInfo, "SUCCESS");
            return controlAds;
        }

        // 2차 Fallback: 같은 세그먼트의 다른 시나리오들
        List<Ad> segmentAds = fallbackToSegmentAds(userSegment, position);
        Map<String, Object> step2 = new HashMap<>();
        step2.put("level", 2);
        step2.put("strategy", "SEGMENT_BASED");
        step2.put("success", !segmentAds.isEmpty());
        step2.put("adCount", segmentAds.size());
        fallbackSteps.add(step2);

        if (!segmentAds.isEmpty()) {
            log.debug("세그먼트 기반 Fallback 성공 - {}개 광고", segmentAds.size());
            logFallbackEvent(fallbackInfo, "SUCCESS");
            return segmentAds;
        }

        // 3차 Fallback: 해당 위치의 모든 광고
        List<Ad> positionAds = fallbackToPositionAds(position);
        Map<String, Object> step3 = new HashMap<>();
        step3.put("level", 3);
        step3.put("strategy", "POSITION_BASED");
        step3.put("success", !positionAds.isEmpty());
        step3.put("adCount", positionAds.size());
        fallbackSteps.add(step3);

        if (!positionAds.isEmpty()) {
            log.debug("위치 기반 최종 Fallback 성공 - {}개 광고", positionAds.size());
            logFallbackEvent(fallbackInfo, "SUCCESS");
            return positionAds;
        }

        // 최종: 빈 리스트
        log.warn("모든 Fallback 실패 - 위치: {}, 세그먼트: {}", position.name(), userSegment.name());
        logFallbackEvent(fallbackInfo, "FAILED");
        return List.of();
    }

    private List<Ad> fallbackToControlScenario(SegmentType userSegment, AdPosition position) {
        try {
            AbTestScenarioType controlScenario = AbTestScenarioType.getDefaultScenario();
            List<Ad> controlAds = adRepository.findByScenarioCodeAndPosition(
                    controlScenario.getScenarioCode(), position
            );

            return selectBestAds(controlAds, userSegment, position);

        } catch (Exception e) {
            log.error("CONTROL 시나리오 Fallback 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Ad> fallbackToSegmentAds(SegmentType userSegment, AdPosition position) {
        try {
            if (userSegment != SegmentType.GENERAL_ALL) {
                List<Ad> segmentAds = adRepository.findBySegmentAndPosition(userSegment, position);
                return segmentAds.stream()
                        .limit(position.getSlotCount())
                        .toList();
            }
            return List.of();

        } catch (Exception e) {
            log.error("세그먼트 기반 Fallback 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Ad> fallbackToPositionAds(AdPosition position) {
        try {
            List<Ad> positionAds = adRepository.findByPositionOnly(position);
            return positionAds.stream()
                    .limit(position.getSlotCount())
                    .toList();

        } catch (Exception e) {
            log.error("위치 기반 Fallback 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isSegmentMatched(Ad ad, SegmentType userSegment) {
        if (ad.getTargetSegment() == SegmentType.GENERAL_ALL) {
            return true;
        }

        if (ad.getTargetSegment() == null) {
            return true;
        }

        if (userSegment == null) {
            return false;
        }

        boolean matched = ad.getTargetSegment().equals(userSegment);

        log.trace("세그먼트 매칭: 광고타겟={}, 사용자={}, 결과={}",
                ad.getTargetSegment().name(), userSegment.name(), matched);

        return matched;
    }

    // 로깅 헬퍼 메서드들
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
                log.info("광고 선택 완료 - 통계: {}", objectMapper.writeValueAsString(stats));
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

                log.error("광고 선택 에러 - {}", objectMapper.writeValueAsString(errorInfo));
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

    // 디버깅/모니터링을 위한 메서드
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