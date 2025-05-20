package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.*;
import com.three.recipingadsservicebe.advertiser.entity.Advertiser;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdCreateRequest {
    @NotNull
    private Long advertiserId;

    @NotBlank
    private String title;

    @NotNull
    private AdType adType;

    private String imageUrl;

    private String targetUrl;

    @NotNull
    private AdPosition preferredPosition;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @NotNull
    private BillingType billingType;

    @NotNull
    @Min(0)
    private Long budget;

    private AbTestGroup abTestGroup;
    private TargetKeyword targetKeyword;

    public Ad toEntity(Advertiser advertiser) {
        return Ad.builder()
                .title(title)
                .adType(adType)
                .imageUrl(imageUrl)
                .targetUrl(targetUrl)
                .preferredPosition(preferredPosition)
                .startAt(startAt)
                .endAt(endAt)
                .status(AdStatus.ACTIVE)
                .billingType(billingType)
                .budget(budget)
                .spentAmount(0L)
                .score(0f)
                .clickCount(0L)
                .impressionCount(0L)
                .abTestGroup(abTestGroup != null ? abTestGroup : AbTestGroup.CONTROL)
                .targetKeyword(targetKeyword != null ? targetKeyword : TargetKeyword.GENERAL)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .advertiser(advertiser)
                .build();
    }
}
