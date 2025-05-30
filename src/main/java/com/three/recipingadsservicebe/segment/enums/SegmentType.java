package com.three.recipingadsservicebe.segment.enums;

/**
 * 사용자 세그먼트 타입
 * Phase 1: 파이프라인 검증을 위해 3개 세그먼트로 축소
 * Phase 2: 성과 검증 후 전체 8개 세그먼트로 확장 예정
 */
public enum SegmentType {

    // 🎯 기본/범용 세그먼트 (CONTROL용)
    GENERAL_ALL("SEG_000", "전체 사용자", true),

    // 🎯 Phase 1 활성화: 파이프라인 검증용 핵심 세그먼트
    DIET_FEMALE_ALL("SEG_002", "다이어트 관심 여성", true),
    MALE_COOK_STARTER("SEG_006", "요리 입문 남성", true),
    ACTIVE_MOM("SEG_003", "활동적인 엄마", true),

    // 🚫 Phase 2 확장용: 주석 처리 (데이터베이스 호환성 위해 enum 유지)
    SOLO_YOUNG("SEG_001", "1인 가구 젊은층", false),
    FINE_DINING_MATURE("SEG_004", "고급 요리 선호 성인", false),
    CONSCIOUS_MZ_FEMALE("SEG_005", "의식적 소비 MZ 여성", false),
    DIGITAL_NATIVE_YOUNG("SEG_007", "디지털 네이티브 젊은층", false),
    PRACTICAL_ADULT("SEG_008", "실용적 성인", false);

    private final String segmentId;
    private final String description;
    private final boolean isActive;  // Phase 1에서 활성화 여부

    SegmentType(String segmentId, String description, boolean isActive) {
        this.segmentId = segmentId;
        this.description = description;
        this.isActive = isActive;
    }

    public String getSegmentId() { return segmentId; }
    public String getDescription() { return description; }
    public boolean isActive() { return isActive; }

    // 🎯 활성화된 세그먼트만 반환
    public static SegmentType[] getActiveSegments() {
        return new SegmentType[]{DIET_FEMALE_ALL, MALE_COOK_STARTER, ACTIVE_MOM};
    }
}