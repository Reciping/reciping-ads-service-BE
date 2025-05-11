package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.AdStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AdStatusUpdateRequest {

    @NotNull
    private AdStatus status;
}