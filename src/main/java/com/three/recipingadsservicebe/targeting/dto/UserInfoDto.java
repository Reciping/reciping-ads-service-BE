package com.three.recipingadsservicebe.targeting.dto;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDto {
    private Long userId;
    private String nickname;
    private String email;
    private SexType sex;
    private AgeType age;
    private RoleType role;

    // 🔧 User 도메인의 UserProfile 정보 (행동태그)
    private DemographicSegment demographicSegment;
    private EngagementLevel engagementLevel;
    private CookingStylePreference cookingStylePreference;
    private OffsetDateTime segmentCalculatedAt;
    private OffsetDateTime behaviorCalculatedAt;


    public enum SexType {
        MALE, FEMALE
    }

    public enum AgeType {
        TWENTIES, THIRTIES, FORTIES, FIFTIES, SIXTIES_PLUS
    }

    public enum RoleType {
        USER, ADMIN
    }
}
