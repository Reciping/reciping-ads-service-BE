package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.*;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdResponse {
    private Long id;
    private String title;
    private AdType adType;
    private String imageUrl;
    private String targetUrl;
    private AdPosition preferredPosition;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private AdStatus status;
    private BillingType billingType;
    private Long budget;
    private Long spentAmount;
    private Float score;
    private Long clickCount;
    private Long impressionCount;
    private Float ctr;

    // A/B 테스트 정보
    private AbTestGroup abTestGroup;
    private String scenarioCode;

    // 타겟팅 정보
    private DemographicSegment targetDemographicSegment;
    private EngagementLevel targetEngagementLevel;
    private CookingStylePreference targetCookingStyle;

    // 광고주 정보
    private String advertiserName;
    private Long advertiserId;

    // 메타데이터
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    /**
     * 계산된 필드들
     */
    public boolean isActive() {
        return AdStatus.ACTIVE.equals(status);
    }

    public boolean isBudgetExhausted() {
        return budget != null && spentAmount != null && spentAmount >= budget;
    }

    public Double getBudgetUtilization() {
        if (budget == null || spentAmount == null || budget == 0) {
            return 0.0;
        }
        return (double) spentAmount / budget * 100;
    }

    public String getPerformanceLevel() {
        if (ctr == null) return "UNKNOWN";
        if (ctr >= 0.05f) return "EXCELLENT";
        if (ctr >= 0.03f) return "GOOD";
        if (ctr >= 0.015f) return "AVERAGE";
        if (ctr >= 0.005f) return "POOR";
        return "VERY_POOR";
    }
}

