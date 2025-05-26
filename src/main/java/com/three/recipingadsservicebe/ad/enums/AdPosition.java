package com.three.recipingadsservicebe.ad.enums;

/**
 * 광고 노출 위치 ENUM
 * 페이지 상단, 하단 등 위치별 슬롯 구분
 */

public enum AdPosition {
    MAIN_TOP(1),
    MAIN_MIDDLE(2),
    MAIN_BOTTOM(3),
    MAIN_LEFT_SIDEBAR(1),
    MAIN_RIGHT_SIDEBAR(1);

    private final int slotCount;

    AdPosition(int slotCount) {
        this.slotCount = slotCount;
    }

    public int getSlotCount() {
        return slotCount;
    }
}
