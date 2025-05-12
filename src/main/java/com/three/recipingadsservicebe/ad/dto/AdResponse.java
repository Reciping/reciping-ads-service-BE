package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.enums.AdType;
import com.three.recipingadsservicebe.ad.enums.BillingType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdResponse {
    private Long id;
    private Long advertiserId;
    private String title;
    private AdType adType;
    private String imageUrl;
    private String targetUrl;
    private AdPosition preferredPosition;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private BillingType billingType;
    private Long budget;
    private Long spentAmount;
    private AdStatus status;
    private Float score;
}

