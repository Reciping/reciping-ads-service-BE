package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdType;
import com.three.recipingadsservicebe.ad.enums.BillingType;
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
}
