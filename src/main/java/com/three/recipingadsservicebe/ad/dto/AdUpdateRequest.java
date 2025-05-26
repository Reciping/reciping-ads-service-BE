package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdType;
import com.three.recipingadsservicebe.ad.enums.BillingType;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdUpdateRequest {

    private String title;

    private AdType adType;

    private String imageUrl;

    private String targetUrl;

    private AdPosition preferredPosition;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private BillingType billingType;

    private Long budget;

    private SegmentType targetSegment;
}
