package com.three.recipingadsservicebe.abtest.enums;


import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;

public enum AbTestScenario {
    TREATMENT("TREATMENT", AbTestGroup.TREATMENT, "행동태그 기반 타겟팅"),
    CONTROL("CONTROL", AbTestGroup.CONTROL, "랜덤 광고 서빙");

    private final String scenarioCode;
    private final AbTestGroup group;
    private final String description;

    AbTestScenario(String scenarioCode, AbTestGroup group, String description) {
        this.scenarioCode = scenarioCode;
        this.group = group;
        this.description = description;
    }

    public String getScenarioCode() { return scenarioCode; }
    public AbTestGroup getGroup() { return group; }
    public String getDescription() { return description; }

    // MVP: 간단한 50:50 분할
    public static AbTestScenario assignForUser(Long userId) {
        return (userId % 2 == 0) ? TREATMENT : CONTROL;
    }
}