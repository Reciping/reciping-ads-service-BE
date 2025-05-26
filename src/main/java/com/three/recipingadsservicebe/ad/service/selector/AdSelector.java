package com.three.recipingadsservicebe.ad.service.selector;

import com.three.recipingadsservicebe.abtest.entity.AbTestScenario;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import com.three.recipingadsservicebe.segment.service.ABTestManager;
import com.three.recipingadsservicebe.segment.service.SegmentCalculatorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AdSelector {

    private final ABTestManager abTestManager;
    private final SegmentCalculatorUtil segmentCalculator;
    private final AdRepository adRepository;

    public Map<String, List<Ad>> getAllAdsForUser(UserInfoDto userInfo) {
        Map<String, List<Ad>> result = new HashMap<>();
        SegmentType userSegment = (userInfo != null) ? segmentCalculator.calculate(userInfo) : null;

        for (AdPosition position : AdPosition.values()) {
            try {
                // 시나리오 할당 (예외 발생 시 fallback)
                AbTestScenario scenario = assignScenarioSafely(userInfo, position);
                List<Ad> candidateAds = adRepository.findByAbTestScenarioIdAndPreferredPosition(
                        scenario.getId(), position
                );

                // 1차: 세그먼트 정확 매칭
                List<Ad> filteredAds = candidateAds.stream()
                        .filter(ad -> userSegment == null || ad.getTargetSegment() == null || ad.getTargetSegment().equals(userSegment))
                        .sorted(Comparator.comparing(Ad::getScore).reversed())
                        .limit(position.getSlotCount())
                        .toList();

                // 2차: 세그먼트 무시
                if (filteredAds.isEmpty() && userSegment != null) {
                    filteredAds = candidateAds.stream()
                            .sorted(Comparator.comparing(Ad::getScore).reversed())
                            .limit(position.getSlotCount())
                            .toList();
                }

                // 3차: CONTROL 시나리오 fallback
                if (filteredAds.isEmpty()) {
                    AbTestScenario controlScenario = abTestManager.getDefaultScenario();
                    List<Ad> controlAds = adRepository.findByAbTestScenarioIdAndPreferredPosition(
                            controlScenario.getId(), position
                    );

                    filteredAds = controlAds.stream()
                            .filter(ad -> userSegment == null || ad.getTargetSegment() == null || ad.getTargetSegment().equals(userSegment))
                            .sorted(Comparator.comparing(Ad::getScore).reversed())
                            .limit(position.getSlotCount())
                            .toList();
                }

                result.put(position.name(), filteredAds);
            } catch (Exception e) {
                System.err.println("⚠️ [" + position.name() + "] 광고 추천 실패: " + e.getMessage());
                result.put(position.name(), List.of()); // 실패한 경우 빈 리스트
            }
        }

        return result;
    }

    /**
     * 유저 정보 기반 AB 테스트 시나리오 할당
     * 실패하거나 CONTROL 그룹이면 기본 시나리오 fallback
     */
    private AbTestScenario assignScenarioSafely(UserInfoDto userInfo, AdPosition position) {
        if (userInfo == null) return abTestManager.getDefaultScenario();

        try {
            AbTestScenario scenario = abTestManager.assignScenario(userInfo, position);
            if (scenario.getGroup() == AbTestGroup.CONTROL) {
                return abTestManager.getDefaultScenario();
            }
            return scenario;
        } catch (Exception e) {
            System.err.println("⚠️ AB 테스트 시나리오 할당 실패 → 기본 시나리오로 fallback: " + e.getMessage());
            return abTestManager.getDefaultScenario();
        }
    }
}
