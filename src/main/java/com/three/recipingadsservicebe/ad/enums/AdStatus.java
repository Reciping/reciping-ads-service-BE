package com.three.recipingadsservicebe.ad.enums;


public enum AdStatus {
    DRAFT("임시저장"),
    PENDING("승인 대기"),
    ACTIVE("활성"),
    PAUSED("일시정지"),
    EXPIRED("만료"),
    REJECTED("거부"),
    DELETED("삭제");

    private final String description;

    AdStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
