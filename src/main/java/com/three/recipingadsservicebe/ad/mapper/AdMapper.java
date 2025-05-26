package com.three.recipingadsservicebe.ad.mapper;

import com.three.recipingadsservicebe.ad.dto.AdResponse;
import com.three.recipingadsservicebe.ad.entity.Ad;

public class AdMapper {
    public static AdResponse toResponse(Ad ad) {
        return AdResponse.builder()
                .id(ad.getId())
                .advertiserId(ad.getAdvertiser().getId())
                .title(ad.getTitle())
                .adType(ad.getAdType())
                .imageUrl(ad.getImageUrl())
                .targetUrl(ad.getTargetUrl())
                .preferredPosition(ad.getPreferredPosition())
                .startAt(ad.getStartAt())
                .endAt(ad.getEndAt())
                .billingType(ad.getBillingType())
                .budget(ad.getBudget())
                .spentAmount(ad.getSpentAmount())
                .status(ad.getStatus())
                .score(ad.getScore())
                .targetSegment(ad.getTargetSegment())
                .build();
    }
}
