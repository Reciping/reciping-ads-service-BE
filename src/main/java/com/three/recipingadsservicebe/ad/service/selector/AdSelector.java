package com.three.recipingadsservicebe.ad.service.selector;

import com.three.recipingadsservicebe.abtest.entity.AbTestScenario;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.AbTestScenarioType;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import com.three.recipingadsservicebe.segment.service.ABTestManager;
import com.three.recipingadsservicebe.segment.service.SegmentCalculatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdSelector {

    private final ABTestManager abTestManager;
    private final SegmentCalculatorUtil segmentCalculator;
    private final AdRepository adRepository;

    public Map<String, List<Ad>> getAllAdsForUser(UserInfoDto userInfo) {
        Map<String, List<Ad>> result = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : SegmentType.GENERAL_ALL;

        log.debug("광고 선택 시작: 사용자 세그먼트={}", userSegment.name());

        for (AdPosition position : AdPosition.getActivePositions()) {
            try {
                List<Ad> ads = selectAdsForPosition(userInfo, userSegment, position);
                result.put(position.name(), ads);

                log.debug("위치 [{}]: {}개 광고 선택 완료", position.name(), ads.size());

            } catch (Exception e) {
                log.warn("위치 [{}] 광고 선택 실패: {}", position.name(), e.getMessage());
                result.put(position.name(), List.of());
            }
        }

        int totalAds = result.values().stream().mapToInt(List::size).sum();
        log.debug("전체 광고 선택 완료: {}개 위치, 총 {}개 광고", result.size(), totalAds);

        return result;
    }

    private List<Ad> selectAdsForPosition(UserInfoDto userInfo, SegmentType userSegment, AdPosition position) {
        // ✅ 1단계: Enum에서 시나리오 할당
        AbTestScenarioType scenarioType = abTestManager.assignScenario(userInfo, position);

        // ✅ 2단계: 시나리오 코드로 광고 조회 (메서드명 수정)
        List<Ad> candidateAds = adRepository.findByScenarioCodeAndPosition(
                scenarioType.getScenarioCode(), position
        );

        log.trace("위치 [{}] 후보 광고: {}개, 시나리오: {}",
                position.name(), candidateAds.size(), scenarioType.getScenarioCode());

        // ✅ 3단계: 세그먼트 매칭 및 선택
        List<Ad> selectedAds = selectBestAds(candidateAds, userSegment, position);

        // ✅ 4단계: 개선된 다단계 Fallback 처리
        if (selectedAds.isEmpty()) {
            selectedAds = performFallbackStrategy(userSegment, position, scenarioType.getScenarioCode());
        }

        return selectedAds;
    }

    private List<Ad> selectBestAds(List<Ad> candidateAds, SegmentType userSegment, AdPosition position) {
        if (candidateAds.isEmpty()) {
            return List.of();
        }

        // ✅ 세그먼트 매칭 필터링 (개선된 로직)
        List<Ad> matchedAds = candidateAds.stream()
                .filter(ad -> isSegmentMatched(ad, userSegment))
                .sorted(Comparator.comparing(Ad::getScore).reversed())
                .limit(position.getSlotCount())
                .toList();

        // 매칭된 광고가 없으면 전체에서 선택 (기존 로직 유지)
        if (matchedAds.isEmpty()) {
            log.debug("세그먼트 [{}] 매칭 실패 - 전체 후보에서 선택", userSegment.name());
            return candidateAds.stream()
                    .sorted(Comparator.comparing(Ad::getScore).reversed())
                    .limit(position.getSlotCount())
                    .toList();
        }

        return matchedAds;
    }

    // ✅ 개선: 다단계 Fallback 전략
    private List<Ad> performFallbackStrategy(SegmentType userSegment, AdPosition position, String originalScenario) {
        log.debug("Fallback 전략 시작 - 세그먼트: {}, 위치: {}, 원본 시나리오: {}",
                userSegment.name(), position.name(), originalScenario);

        // 1차 Fallback: CONTROL 시나리오
        List<Ad> controlAds = fallbackToControlScenario(userSegment, position);
        if (!controlAds.isEmpty()) {
            log.debug("CONTROL 시나리오 Fallback 성공 - {}개 광고", controlAds.size());
            return controlAds;
        }

        // 2차 Fallback: 같은 세그먼트의 다른 시나리오들
        List<Ad> segmentAds = fallbackToSegmentAds(userSegment, position);
        if (!segmentAds.isEmpty()) {
            log.debug("세그먼트 기반 Fallback 성공 - {}개 광고", segmentAds.size());
            return segmentAds;
        }

        // 3차 Fallback: 해당 위치의 모든 광고
        List<Ad> positionAds = fallbackToPositionAds(position);
        if (!positionAds.isEmpty()) {
            log.debug("위치 기반 최종 Fallback 성공 - {}개 광고", positionAds.size());
            return positionAds;
        }

        // 최종: 빈 리스트 (서비스 중단 방지)
        log.warn("모든 Fallback 실패 - 위치: {}, 세그먼트: {}", position.name(), userSegment.name());
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

    // ✅ 새로 추가: 세그먼트 기반 Fallback
    private List<Ad> fallbackToSegmentAds(SegmentType userSegment, AdPosition position) {
        try {
            // GENERAL_ALL이 아닌 경우에만 세그먼트 기반 조회
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

    // ✅ 새로 추가: 위치 기반 최종 Fallback
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

    // ✅ 개선: 세그먼트 매칭 로직 명확화
    private boolean isSegmentMatched(Ad ad, SegmentType userSegment) {
        // 1. 광고의 타겟 세그먼트가 GENERAL_ALL이면 모든 사용자와 매칭
        if (ad.getTargetSegment() == SegmentType.GENERAL_ALL) {
            return true;
        }

        // 2. 광고의 타겟 세그먼트가 null이면 모든 사용자와 매칭 (레거시 호환)
        if (ad.getTargetSegment() == null) {
            return true;
        }

        // 3. 사용자 세그먼트가 null이면 매칭 안됨
        if (userSegment == null) {
            return false;
        }

        // 4. 정확한 세그먼트 매칭
        boolean matched = ad.getTargetSegment().equals(userSegment);

        log.trace("세그먼트 매칭: 광고타겟={}, 사용자={}, 결과={}",
                ad.getTargetSegment().name(), userSegment.name(), matched);

        return matched;
    }

    // ✅ 추가: 디버깅/모니터링을 위한 메서드
    public Map<String, Object> getSelectionStats(UserInfoDto userInfo) {
        Map<String, Object> stats = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : SegmentType.GENERAL_ALL;

        stats.put("userSegment", userSegment.name());
        stats.put("activePositions", AdPosition.getActivePositions().length);

        // 각 위치별 사용 가능한 광고 수 체크
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