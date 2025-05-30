package com.three.recipingadsservicebe.segment.enums;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A/B 테스트 시나리오 타입
 * Phase 1: 3개 세그먼트 × 2개 메시지 = 6개 시나리오 + 1개 기본
 * Phase 2: 8개 세그먼트 × 6개 메시지 = 48개 시나리오로 확장
 */
public enum AbTestScenarioType {
    // 🎯 Phase 1 활성화: 파이프라인 검증용 핵심 시나리오

    // 다이어트 여성: 감성 vs 기능
    SC_DIET_EMO_A(SegmentType.DIET_FEMALE_ALL, MessageType.EMO, AbTestGroup.A,
            "감성적 다이어트 어필", true),
    SC_DIET_FUN_B(SegmentType.DIET_FEMALE_ALL, MessageType.FUN, AbTestGroup.B,
            "기능적 다이어트 어필", true),

    // 요리 입문 남성: 감성 vs 기능
    SC_COOK_EMO_A(SegmentType.MALE_COOK_STARTER, MessageType.EMO, AbTestGroup.A,
            "감성적 요리 입문", true),
    SC_COOK_FUN_B(SegmentType.MALE_COOK_STARTER, MessageType.FUN, AbTestGroup.B,
            "기능적 요리 입문", true),

    // 활동적 엄마: 감성 vs 기능
    SC_MOM_EMO_A(SegmentType.ACTIVE_MOM, MessageType.EMO, AbTestGroup.A,
            "감성적 가족 요리", true),
    SC_MOM_FUN_B(SegmentType.ACTIVE_MOM, MessageType.FUN, AbTestGroup.B,
            "기능적 가족 요리", true),

    // ✅ 개선: 범용 기본 시나리오
    SC_DEFAULT_GENERAL(SegmentType.GENERAL_ALL, MessageType.EMO, AbTestGroup.CONTROL,
            "일반 기본 광고", true),

    // 🚫 Phase 2 확장용: 비활성화된 시나리오들
    SC_VEGAN_VAL_A(SegmentType.CONSCIOUS_MZ_FEMALE, MessageType.VAL, AbTestGroup.A,
            "가치소비 어필", false),
    SC_VEGAN_ECO_B(SegmentType.CONSCIOUS_MZ_FEMALE, MessageType.ECO, AbTestGroup.B,
            "경제적 실익 어필", false);
    // ... 나머지 비활성 시나리오들

    private final SegmentType segment;
    private final MessageType messageType;
    private final AbTestGroup group;
    private final String description;
    private final boolean isActive;

    AbTestScenarioType(SegmentType segment, MessageType messageType, AbTestGroup group,
                       String description, boolean isActive) {
        this.segment = segment;
        this.messageType = messageType;
        this.group = group;
        this.description = description;
        this.isActive = isActive;
    }

    // Getters
    public SegmentType getSegment() { return segment; }
    public MessageType getMessageType() { return messageType; }
    public AbTestGroup getGroup() { return group; }
    public String getDescription() { return description; }
    public boolean isActive() { return isActive; }
    public String getScenarioCode() { return this.name(); }

    // ✅ 활성화된 시나리오만 반환
    public static List<AbTestScenarioType> getActiveScenarios() {
        return Arrays.stream(values())
                .filter(AbTestScenarioType::isActive)
                .collect(Collectors.toList());
    }

    // ✅ 특정 세그먼트의 활성 시나리오 조회
    public static List<AbTestScenarioType> getBySegment(SegmentType segment) {
        return Arrays.stream(values())
                .filter(s -> s.segment == segment && s.isActive)
                .collect(Collectors.toList());
    }

    // ✅ 세그먼트 + 그룹으로 시나리오 찾기
    public static Optional<AbTestScenarioType> findBySegmentAndGroup(SegmentType segment, AbTestGroup group) {
        return Arrays.stream(values())
                .filter(s -> s.segment == segment && s.group == group && s.isActive)
                .findFirst();
    }

    // ✅ 기본 시나리오 반환
    public static AbTestScenarioType getDefaultScenario() {
        return SC_DEFAULT_GENERAL;
    }
}
