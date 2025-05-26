package com.three.recipingadsservicebe.segment.service;

import com.three.recipingadsservicebe.abtest.entity.AbTestScenario;
import com.three.recipingadsservicebe.abtest.repository.AbTestScenarioRepository;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ABTestManager {

    private final SegmentCalculatorUtil segmentCalculator;
    private final AbTestScenarioRepository scenarioRepository;

    public AbTestScenario assignScenario(UserInfoDto userInfo, AdPosition position) {
        SegmentType segment = segmentCalculator.calculate(userInfo);
        AbTestGroup group = calculateGroup(userInfo.getUserId(), segment.name());

        return scenarioRepository.findBySegmentAndGroupAndIsActiveTrue(segment, group)
                .orElseThrow(() -> new IllegalStateException("No ABTestScenario found for segment=" + segment + ", group=" + group));
    }

    private AbTestGroup calculateGroup(Long userId, String segmentName) {
        int hash = Math.abs(Objects.hash(userId, segmentName));
        int mod = hash % 2; // A 또는 B만
        return (mod == 0) ? AbTestGroup.A : AbTestGroup.B;
    }


    public AbTestScenario getDefaultScenario() {
        return scenarioRepository.findByScenarioCodeAndIsActiveTrue("SC_DEFAULT_GENERAL")
                .orElseThrow(() -> new IllegalStateException("기본 시나리오(SC_DEFAULT_GENERAL)가 없습니다."));
    }

}

