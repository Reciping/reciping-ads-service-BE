package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.*;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdResponse {
    private Long id;
    private String title;
    private AdType adType;
    private String imageUrl;
    private String targetUrl;
    private AdPosition preferredPosition;
    private AdStatus status;
    private BillingType billingType;
    private Long budget;
    private Long spentAmount;
    private Float score;
    private Long clickCount;
    private Long impressionCount;
    private Float ctr;

    // 🔧 A/B 테스트 관련 필드
    private AbTestGroup abTestGroup;
    private String scenarioCode;

    // 🔧 행동태그 타겟팅 필드
    private DemographicSegment targetDemographicSegment;
    private EngagementLevel targetEngagementLevel;
    private CookingStylePreference targetCookingStyle;

    private String advertiserName;
    private LocalDateTime createdAt;

}

