package com.three.recipingadsservicebe.log.dto;

public enum LogType {
    // 기존 타입들
    VIEW,          // 일반적인 페이지 조회
    CLICK,         // 클릭 이벤트
    LIKE,          // 좋아요
    BOOKMARK,      // 북마크
    LOGIN,         // 로그인
    SIGNUP,        // 회원가입
    LOGOUT,        // 로그아웃
    JOIN_EVENT,    // 이벤트 참여
    EXPOSE,        // 콘텐츠 노출
    EXIT_INTENT,   // 이탈 징후
    VAR_VIEW,      // AB TEST

    // 검색 관련
    SEARCH_MENU,
    SEARCH_INGREDIENT,
    SEARCH_NATURAL,
    SEARCH_TAGS,

    // 광고 도메인 전용 추가
    AD_IMPRESSION,     // 광고 노출
    AD_CLICK,          // 광고 클릭
    AD_SERVE,          // 광고 제공 (서버 응답)
    AD_FALLBACK,       // 광고 폴백 발생
    AD_BUDGET_EXHAUST, // 예산 소진
    AD_CREATE,         // 광고 생성
    AD_UPDATE,         // 광고 수정
    AD_DELETE,         // 광고 삭제
    AD_STATUS_CHANGE,  // 광고 상태 변경
    AD_BILLING,        // 광고 과금 발생
    AD_PERFORMANCE     // 광고 성과 집계
}
