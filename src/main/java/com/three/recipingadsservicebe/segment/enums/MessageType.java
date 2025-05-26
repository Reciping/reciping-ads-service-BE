package com.three.recipingadsservicebe.segment.enums;

/**
 * A/B 테스트 메시지 유형 Enum
 * 각 메시지는 사용자에게 전달하고자 하는 광고의 어필 방식입니다.
 */
public enum MessageType {

    /**
     * Emotional - 감성적 메시지 (공감, 분위기, 감동)
     * ex. "당신의 건강을 위한 따뜻한 식사"
     */
    EMO,

    /**
     * Functional - 기능 중심 메시지 (논리적, 효율성, 근거 기반)
     * ex. "칼로리 30% 감소, 체중 감량 효과 입증"
     */
    FUN,

    /**
     * Economical - 경제성 어필 (절약, 가성비, 비용절감)
     * ex. "한 달 식비 절감! 저렴한 간편식"
     */
    ECO,

    /**
     * Value-oriented - 가치소비 메시지 (환경, 윤리, 사회적 가치를 중시)
     * ex. "비건으로 지구를 지켜요. 제로웨이스트 키친"
     */
    VAL,

    /**
     * Social - 사회적 메시지 (커뮤니티, 소속감, 공유)
     * ex. "다같이 참여하는 레시피 챌린지"
     */
    SOC,

    /**
     * Healthy - 건강 중심 메시지 (영양, 식단, 웰빙 강조)
     * ex. "당신의 건강을 지켜줄 식단"
     */
    HEALTHY

}
