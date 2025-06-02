package com.three.recipingadsservicebe.ad.repository;

import com.three.recipingadsservicebe.abtest.entity.AbTestScenario;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface AdRepository extends JpaRepository<Ad, Long> {
// AdRepository.java - 성능 최적화를 위한 배치 쿼리 메서드들

    /**
     * 🔥 개선: 여러 시나리오 코드로 한 번에 조회
     */
    @Query("SELECT a FROM Ad a WHERE a.scenarioCode IN :scenarioCodes AND a.status = 'ACTIVE' " +
            "ORDER BY a.score DESC, a.createdAt DESC")
    List<Ad> findByScenarioCodesIn(@Param("scenarioCodes") Set<String> scenarioCodes);

    /**
     * 🔥 개선: 시나리오별로 그룹화해서 반환
     */
    default Map<String, List<Ad>> findByScenarioCodesGrouped(Set<String> scenarioCodes) {
        List<Ad> ads = findByScenarioCodesIn(scenarioCodes);
        return ads.stream().collect(Collectors.groupingBy(Ad::getScenarioCode));
    }

    /**
     * 기존 메서드들 개선
     */
    @Query("SELECT a FROM Ad a WHERE a.scenarioCode = :scenarioCode " +
            "AND a.preferredPosition = :position AND a.status = 'ACTIVE' " +
            "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
            "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP) " +
            "AND (a.budget IS NULL OR a.spentAmount < a.budget) " +
            "ORDER BY a.score DESC")
    List<Ad> findByScenarioCodeAndPosition(@Param("scenarioCode") String scenarioCode,
                                           @Param("position") AdPosition position);

    @Query("SELECT a FROM Ad a WHERE a.targetSegment = :segment " +
            "AND a.preferredPosition = :position AND a.status = 'ACTIVE' " +
            "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
            "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP) " +
            "AND (a.budget IS NULL OR a.spentAmount < a.budget) " +
            "ORDER BY a.score DESC")
    List<Ad> findBySegmentAndPosition(@Param("segment") SegmentType segment,
                                      @Param("position") AdPosition position);

    @Query("SELECT a FROM Ad a WHERE a.preferredPosition = :position AND a.status = 'ACTIVE' " +
            "AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP) " +
            "AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP) " +
            "AND (a.budget IS NULL OR a.spentAmount < a.budget) " +
            "ORDER BY a.score DESC")
    List<Ad> findByPositionOnly(@Param("position") AdPosition position);


}