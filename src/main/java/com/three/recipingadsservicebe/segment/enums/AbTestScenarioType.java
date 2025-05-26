package com.three.recipingadsservicebe.segment.enums;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum AbTestScenarioType {

    SC_DIET_EMO_FUN_A(SegmentType.DIET_FEMALE_ALL, MessageType.EMO, AbTestGroup.A, "감성 다이어트 어필"),
    SC_DIET_EMO_FUN_B(SegmentType.DIET_FEMALE_ALL, MessageType.FUN, AbTestGroup.B, "기능적 다이어트 어필"),

    SC_VEGAN_VAL_ECO_A(SegmentType.CONSCIOUS_MZ_FEMALE, MessageType.VAL, AbTestGroup.A, "가치소비 어필"),
    SC_VEGAN_VAL_ECO_B(SegmentType.CONSCIOUS_MZ_FEMALE, MessageType.ECO, AbTestGroup.B, "경제적 실익 어필"),

    SC_SOLO_EMO_ECO_A(SegmentType.SOLO_YOUNG, MessageType.EMO, AbTestGroup.A, "라이프스타일 어필"),
    SC_SOLO_EMO_ECO_B(SegmentType.SOLO_YOUNG, MessageType.ECO, AbTestGroup.B, "경제성 어필"),

    SC_MOM_FUN_ECO_A(SegmentType.ACTIVE_MOM, MessageType.FUN, AbTestGroup.A, "전문성 어필"),
    SC_MOM_FUN_ECO_B(SegmentType.ACTIVE_MOM, MessageType.ECO, AbTestGroup.B, "편의성 어필"),

    SC_MALE_SOC_FUN_A(SegmentType.MALE_COOK_STARTER, MessageType.SOC, AbTestGroup.A, "요리 커뮤니티 소속감"),
    SC_MALE_SOC_FUN_B(SegmentType.MALE_COOK_STARTER, MessageType.FUN, AbTestGroup.B, "기능적 요리 입문"),

    SC_PREMIUM_FUN_VAL_A(SegmentType.FINE_DINING_MATURE, MessageType.FUN, AbTestGroup.A, "전문가 기반 요리 기능"),
    SC_PREMIUM_FUN_VAL_B(SegmentType.FINE_DINING_MATURE, MessageType.VAL, AbTestGroup.B, "가치 기반 프리미엄"),

    SC_DIGITAL_SOC_ECO_A(SegmentType.DIGITAL_NATIVE_YOUNG, MessageType.SOC, AbTestGroup.A, "요리 트렌드 참여감"),
    SC_DIGITAL_SOC_ECO_B(SegmentType.DIGITAL_NATIVE_YOUNG, MessageType.ECO, AbTestGroup.B, "경제적 요리 팁"),

    SC_PRACTICAL_HEA_ECO_A(SegmentType.PRACTICAL_ADULT, MessageType.HEALTHY, AbTestGroup.A, "건강한 실용 요리"),
    SC_PRACTICAL_HEA_ECO_B(SegmentType.PRACTICAL_ADULT, MessageType.ECO, AbTestGroup.B, "가성비 실용 요리"),

    SC_DEFAULT_GENERAL(SegmentType.PRACTICAL_ADULT, MessageType.HEALTHY, AbTestGroup.CONTROL, "일반 default 광고");

    private final SegmentType segment;
    private final MessageType messageType;
    private final AbTestGroup group;
    private final String description;

    AbTestScenarioType(SegmentType segment, MessageType messageType, AbTestGroup group, String description) {
        this.segment = segment;
        this.messageType = messageType;
        this.group = group;
        this.description = description;
    }

    public SegmentType getSegment() { return segment; }
    public MessageType getMessageType() { return messageType; }
    public AbTestGroup getGroup() { return group; }
    public String getDescription() { return description; }

    public String getScenarioCode() {
        return this.name();
    }

    public static List<AbTestScenarioType> getBySegment(SegmentType segment) {
        return Arrays.stream(values())
                .filter(s -> s.segment == segment)
                .collect(Collectors.toList());
    }
}
