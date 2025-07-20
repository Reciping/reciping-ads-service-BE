package com.three.recipingadsservicebe.targeting.enums;

public enum EngagementLevel {
    HIGH_ACTIVE("고활성 사용자", "주 5회 이상 접속"),
    REGULAR_USER("일반 사용자", "주 2-4회 접속"),
    CASUAL_USER("캐주얼 사용자", "주 1회 이하 접속"),
    DORMANT_USER("휴면 사용자", "30일 이상 미접속");

    private final String description;
    private final String criteria;

    EngagementLevel(String description, String criteria) {
        this.description = description;
        this.criteria = criteria;
    }

    public String getDescription() { return description; }
    public String getCriteria() { return criteria; }
}
