package com.three.recipingadsservicebe.ad.entity;

import com.three.recipingadsservicebe.ad.dto.AdUpdateRequest;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.enums.AdType;
import com.three.recipingadsservicebe.ad.enums.BillingType;
import com.three.recipingadsservicebe.advertiser.entity.Advertiser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ?")
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
}

