package com.three.recipingadsservicebe.ad.dto;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdServeResponse {
    private List<AdResponse> ads;
    private AbTestGroup abTestGroup;
}
