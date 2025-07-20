package com.three.recipingadsservicebe.targeting.enums;

public enum DemographicSegment {
    // 성별 + 연령 조합 세그먼트
    FEMALE_TWENTIES("여성 20대"),
    FEMALE_THIRTIES("여성 30대"),
    FEMALE_FORTIES("여성 40대"),
    FEMALE_FIFTIES_PLUS("여성 50대 이상"),

    MALE_TWENTIES("남성 20대"),
    MALE_THIRTIES("남성 30대"),
    MALE_FORTIES("남성 40대"),
    MALE_FIFTIES_PLUS("남성 50대 이상"),

    UNKNOWN("미분류");

    private final String description;

    DemographicSegment(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
