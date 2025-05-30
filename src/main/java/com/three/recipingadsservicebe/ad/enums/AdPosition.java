package com.three.recipingadsservicebe.ad.enums;

/**
 * 광고 노출 위치 ENUM
 * 파이프라인 검증을 위해 핵심 위치만 활성화
 * TODO: 2차 확장 시 주석 해제
 */
public enum AdPosition {
    // 🎯 활성화: 파이프라인 검증용 핵심 위치
    MAIN_TOP(1),        // 상단 프리미엄 위치 (높은 CTR 예상)
    MAIN_MIDDLE(1);    // 중간 배너 위치 (안정적 성과 예상)

    // 🚫 2차 확장용: 통계적 검정력 확보 후 활성화 예정
    // MAIN_BOTTOM(3),
    // MAIN_LEFT_SIDEBAR(1),
    // MAIN_RIGHT_SIDEBAR(1);


    private final int slotCount;

    AdPosition(int slotCount) {
        this.slotCount = slotCount;
    }

    public int getSlotCount() {
        return slotCount;
    }

    // 🎯 활성화된 위치만 반환 (AdSelector에서 사용)
    public static AdPosition[] getActivePositions() {
        return new AdPosition[]{MAIN_TOP, MAIN_MIDDLE};
    }

    // 검증용: 해당 위치가 활성화되었는지 확인
    public boolean isActive() {
        return this.slotCount > 0;
    }
}
