package com.three.recipingadsservicebe.segment.enums;

/**
 * A/B 테스트 메시지 유형 Enum
 * Phase 1: 감성 vs 기능적 메시지 2개로 축소하여 명확한 차이 검증
 * Phase 2: 6개 전체 메시지 타입으로 확장 예정
 */
public enum MessageType {
    // 🎯 Phase 1 활성화: 명확한 대조를 위한 핵심 메시지 타입
    /**
     * Emotional - 감성적 메시지 (공감, 분위기, 감동)
     * ex. "당신의 건강을 위한 따뜻한 식사"
     */
    EMO(true, "감성적"),

    /**
     * Functional - 기능 중심 메시지 (논리적, 효율성, 근거 기반)
     * ex. "칼로리 30% 감소, 체중 감량 효과 입증"
     */
    FUN(true, "기능적"),

    // 🚫 Phase 2 확장용: 비활성화
    /**
     * Economical - 경제성 어필 (절약, 가성비, 비용절감)
     */
    ECO(false, "경제성"),

    /**
     * Value-oriented - 가치소비 메시지 (환경, 윤리, 사회적 가치)
     */
    VAL(false, "가치소비"),

    /**
     * Social - 사회적 메시지 (커뮤니티, 소속감, 공유)
     */
    SOC(false, "사회적"),

    /**
     * Healthy - 건강 중심 메시지 (영양, 식단, 웰빙 강조)
     */
    HEALTHY(false, "건강");

    private final boolean isActive;
    private final String displayName;

    MessageType(boolean isActive, String displayName) {
        this.isActive = isActive;
        this.displayName = displayName;
    }

    public boolean isActive() { return isActive; }
    public String getDisplayName() { return displayName; }

    // 🎯 활성화된 메시지 타입만 반환
    public static MessageType[] getActiveTypes() {
        return new MessageType[]{EMO, FUN};
    }
}