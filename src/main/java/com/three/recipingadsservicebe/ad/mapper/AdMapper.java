package com.three.recipingadsservicebe.ad.mapper;

import com.three.recipingadsservicebe.ad.dto.AdResponse;
import com.three.recipingadsservicebe.ad.entity.Ad;

public class AdMapper {
    public static AdResponse toResponse(Ad ad) {
        if (ad == null) return null;

        return AdResponse.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .adType(ad.getAdType())
                .imageUrl(ad.getImageUrl())
                .targetUrl(ad.getTargetUrl())
                .preferredPosition(ad.getPreferredPosition())
                .status(ad.getStatus())
                .billingType(ad.getBillingType())
                .budget(ad.getBudget())
                .spentAmount(ad.getSpentAmount())
                .score(ad.getScore())
                .clickCount(ad.getClickCount())
                .impressionCount(ad.getImpressionCount())
                .ctr(ad.calculateCTR())
                // 🔧 새로운 A/B 테스트 필드들
                .abTestGroup(ad.getAbTestGroup())
                .scenarioCode(ad.getScenarioCode())
                // 🔧 새로운 행동태그 타겟팅 필드들
                .targetDemographicSegment(ad.getTargetDemographicSegment())
                .targetEngagementLevel(ad.getTargetEngagementLevel())
                .targetCookingStyle(ad.getTargetCookingStyle())
                .advertiserName(ad.getAdvertiser() != null ? ad.getAdvertiser().getName() : null)
                .createdAt(ad.getCreatedAt())
                .build();
    }
}
