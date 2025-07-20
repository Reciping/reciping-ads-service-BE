package com.three.recipingadsservicebe.ad.entity;

import com.three.recipingadsservicebe.ad.dto.AdUpdateRequest;
import com.three.recipingadsservicebe.ad.enums.*;
import com.three.recipingadsservicebe.advertiser.entity.Advertiser;
import com.three.recipingadsservicebe.targeting.enums.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE ads SET deleted_at = now() WHERE id = ?")
@SQLRestriction(value = "deleted_at IS NULL")
@Entity
@Table(name = "ads")
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", length = 20)
    private AdType adType;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_position", length = 100)
    private AdPosition preferredPosition;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AdStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", length = 20)
    private BillingType billingType;

    private Long budget;

    @Column(name = "spent_amount")
    private Long spentAmount;

    private Float score;

    @Column(name = "click_count")
    private Long clickCount = 0L;

    @Column(name = "impression_count")
    private Long impressionCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "ab_test_group", length = 10)
    private AbTestGroup abTestGroup;

    @Column(name = "scenario_code")
    private String scenarioCode;

    // 🔧 새로운 행동태그 기반 타겟팅 필드들
    @Enumerated(EnumType.STRING)
    @Column(name = "target_demographic_segment", length = 50)
    private DemographicSegment targetDemographicSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_engagement_level", length = 50)
    private EngagementLevel targetEngagementLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_cooking_style", length = 50)
    private CookingStylePreference targetCookingStyle;

    // 공통 메타데이터
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertiser_id", nullable = false)
    private Advertiser advertiser;


    public void updateFrom(AdUpdateRequest request) {
        if (request.getTitle() != null) this.title = request.getTitle();
        if (request.getAdType() != null) this.adType = request.getAdType();
        if (request.getImageUrl() != null) this.imageUrl = request.getImageUrl();
        if (request.getTargetUrl() != null) this.targetUrl = request.getTargetUrl();
        if (request.getPreferredPosition() != null) this.preferredPosition = request.getPreferredPosition();
        if (request.getStartAt() != null) this.startAt = request.getStartAt();
        if (request.getEndAt() != null) this.endAt = request.getEndAt();
        if (request.getBillingType() != null) this.billingType = request.getBillingType();
        if (request.getBudget() != null) this.budget = request.getBudget();

        this.modifiedAt = LocalDateTime.now();
    }

    public void changeStatus(AdStatus status) {
        this.status = status;
        this.modifiedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }


    public void increaseClick() {
        this.clickCount = (this.clickCount == null ? 1 : this.clickCount + 1);
    }

    public void increaseImpression() {
        this.impressionCount = (this.impressionCount == null ? 1 : this.impressionCount + 1);
    }

    public void increaseSpentAmount(Long amount) {
        this.spentAmount = (this.spentAmount == null ? amount : this.spentAmount + amount);
    }

    public float calculateCTR() {
        if (impressionCount == null || impressionCount == 0) return 0f;
        return (float) clickCount / impressionCount;
    }

    // 🔧 새로운 타겟팅 매치 검증 메서드
    public boolean isTargetingMatched(DemographicSegment userDemographic,
                                      EngagementLevel userEngagement,
                                      CookingStylePreference userCookingStyle) {
        // 타겟팅이 설정되지 않은 경우 전체 대상
        if (targetDemographicSegment == null && targetEngagementLevel == null && targetCookingStyle == null) {
            return true;
        }

        // 각 타겟팅 조건 확인 (설정된 것만)
        boolean demographicMatch = (targetDemographicSegment == null) ||
                targetDemographicSegment.equals(userDemographic);
        boolean engagementMatch = (targetEngagementLevel == null) ||
                targetEngagementLevel.equals(userEngagement);
        boolean cookingStyleMatch = (targetCookingStyle == null) ||
                targetCookingStyle.equals(userCookingStyle);

        return demographicMatch && engagementMatch && cookingStyleMatch;
    }
}

