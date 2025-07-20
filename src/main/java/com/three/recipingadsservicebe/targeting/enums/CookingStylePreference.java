package com.three.recipingadsservicebe.targeting.enums;

public enum CookingStylePreference {
    HEALTH_CONSCIOUS("건강 중심", "다이어트, 저칼로리 요리 선호"),
    CONVENIENCE_SEEKER("편의 추구", "간편식, 빠른 조리 선호"),
    GOURMET_EXPLORER("미식 탐험", "새로운 요리, 고급 재료 선호"),
    FAMILY_ORIENTED("가족 중심", "아이 요리, 대용량 요리 선호"),
    DIVERSE_EXPLORER("다양성 추구", "다양한 스타일 시도");

    private final String description;
    private final String characteristics;

    CookingStylePreference(String description, String characteristics) {
        this.description = description;
        this.characteristics = characteristics;
    }

    public String getDescription() { return description; }
    public String getCharacteristics() { return characteristics; }
}
