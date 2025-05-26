package com.three.recipingadsservicebe.abtest.repository;

import com.three.recipingadsservicebe.abtest.entity.AbTestScenario;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AbTestScenarioRepository extends JpaRepository<AbTestScenario, Long> {

    Optional<AbTestScenario> findBySegmentAndGroupAndIsActiveTrue(
            SegmentType segment, AbTestGroup group
    );

    // 기본 광고 시나리오 조회용 메서드
    Optional<AbTestScenario> findByScenarioCodeAndIsActiveTrue(String scenarioCode);



}
