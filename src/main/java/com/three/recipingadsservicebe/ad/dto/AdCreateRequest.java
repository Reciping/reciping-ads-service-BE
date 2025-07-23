package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.*;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdCreateRequest {
    private String title;
    private AdType adType;
    private String imageUrl;
    private String targetUrl;
    private AdPosition preferredPosition;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BillingType billingType;
    private Long budget;
    private Float score;
    private Long advertiserId;

    // 🔧 A/B 테스트 관련 필드
    private AbTestGroup abTestGroup;
    private String scenarioCode;

    // 🔧 행동태그 타겟팅 필드
    private DemographicSegment targetDemographicSegment;
    private EngagementLevel targetEngagementLevel;
    private CookingStylePreference targetCookingStyle;
}
