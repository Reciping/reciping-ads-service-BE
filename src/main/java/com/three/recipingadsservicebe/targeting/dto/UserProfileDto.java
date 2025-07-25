package com.three.recipingadsservicebe.targeting.dto;

import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class UserProfileDto {
    private Long userId;
    private DemographicSegment demographicSegment;
    private EngagementLevel engagementLevel;
    private CookingStylePreference cookingStylePreference;
    private OffsetDateTime segmentCalculatedAt;
    private OffsetDateTime behaviorCalculatedAt;

    // A/B 테스트 그룹 결정 편의 메서드
    public boolean isTreatmentGroup() {
        return userId != null && userId % 2 == 0;
    }

    public String getAbTestGroup() {
        return isTreatmentGroup() ? "TREATMENT" : "CONTROL";
    }
}
