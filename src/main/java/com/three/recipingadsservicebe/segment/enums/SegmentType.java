package com.three.recipingadsservicebe.segment.enums;

public enum SegmentType {
    SOLO_YOUNG("SEG_001"),
    DIET_FEMALE_ALL("SEG_002"),
    ACTIVE_MOM("SEG_003"),
    FINE_DINING_MATURE("SEG_004"),
    CONSCIOUS_MZ_FEMALE("SEG_005"),
    MALE_COOK_STARTER("SEG_006"),
    DIGITAL_NATIVE_YOUNG("SEG_007"),
    PRACTICAL_ADULT("SEG_008");

    private final String segmentId;

    SegmentType(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getSegmentId() {
        return segmentId;
    }
}