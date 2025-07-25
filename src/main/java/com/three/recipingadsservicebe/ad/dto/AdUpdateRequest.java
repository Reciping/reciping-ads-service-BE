package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdType;
import com.three.recipingadsservicebe.ad.enums.BillingType;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdUpdateRequest {

    @Size(max = 255, message = "광고 제목은 255자를 초과할 수 없습니다.")
    private String title;

    private AdType adType;

    @Size(max = 500, message = "이미지 URL은 500자를 초과할 수 없습니다.")
    private String imageUrl;

    @Size(max = 500, message = "타겟 URL은 500자를 초과할 수 없습니다.")
    private String targetUrl;

    private AdPosition preferredPosition;

    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    private BillingType billingType;

    @Min(value = 0, message = "예산은 0 이상이어야 합니다.")
    private Long budget;

    @DecimalMin(value = "0.0", message = "광고 점수는 0.0 이상이어야 합니다.")
    @DecimalMax(value = "100.0", message = "광고 점수는 100.0 이하여야 합니다.")
    private Float score;

    // 🔧 행동태그 타겟팅 필드들 (수정 가능)
    private DemographicSegment targetDemographicSegment;
    private EngagementLevel targetEngagementLevel;
    private CookingStylePreference targetCookingStyle;

    // 🔧 A/B 테스트 관련 필드들 (일반적으로 수정하지 않지만 필요시 포함)
    // private AbTestGroup abTestGroup;  // 보통 생성 후 변경하지 않음
    // private String scenarioCode;      // 보통 생성 후 변경하지 않음

    /**
     * 날짜 유효성 검증
     */
    public boolean isValidDateRange() {
        if (startAt == null || endAt == null) {
            return true; // null인 경우는 다른 곳에서 처리
        }
        return !startAt.isAfter(endAt);
    }

    /**
     * 최소 하나의 필드가 업데이트되는지 확인
     */
    public boolean hasAnyFieldToUpdate() {
        return title != null ||
                adType != null ||
                imageUrl != null ||
                targetUrl != null ||
                preferredPosition != null ||
                startAt != null ||
                endAt != null ||
                billingType != null ||
                budget != null ||
                score != null ||
                targetDemographicSegment != null ||
                targetEngagementLevel != null ||
                targetCookingStyle != null;
    }
}