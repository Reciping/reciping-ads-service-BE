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

public interface AdRepository extends JpaRepository<Ad, Long> {

    // ========== 🎯 Phase 1 핵심 메서드들 ==========

    /**
     * 1. 시나리오 코드 + 위치로 활성 광고 조회 (가장 중요한 메서드)
     * AdSelector에서 가장 많이 사용
     */
    @Query("SELECT a FROM Ad a WHERE " +
            "a.scenarioCode = :scenarioCode " +
            "AND a.preferredPosition = :position " +
            "AND a.status = 'ACTIVE' " +
            "AND a.startAt <= CURRENT_TIMESTAMP " +
            "AND a.endAt >= CURRENT_TIMESTAMP " +
            "ORDER BY a.score DESC")
    List<Ad> findByScenarioCodeAndPosition(
            @Param("scenarioCode") String scenarioCode,
            @Param("position") AdPosition position
    );

    /**
     * 2. 세그먼트 + 위치로 활성 광고 조회 (Fallback용)
     * 시나리오 매칭 실패 시 사용
     */
    @Query("SELECT a FROM Ad a WHERE " +
            "a.targetSegment = :segment " +
            "AND a.preferredPosition = :position " +
            "AND a.status = 'ACTIVE' " +
            "AND a.startAt <= CURRENT_TIMESTAMP " +
            "AND a.endAt >= CURRENT_TIMESTAMP " +
            "ORDER BY a.score DESC")
    List<Ad> findBySegmentAndPosition(
            @Param("segment") SegmentType segment,
            @Param("position") AdPosition position
    );

    /**
     * 3. 위치별 모든 활성 광고 조회 (최종 Fallback용)
     * 세그먼트 매칭도 실패 시 사용
     */
    @Query("SELECT a FROM Ad a WHERE " +
            "a.preferredPosition = :position " +
            "AND a.status = 'ACTIVE' " +
            "AND a.startAt <= CURRENT_TIMESTAMP " +
            "AND a.endAt >= CURRENT_TIMESTAMP " +
            "ORDER BY a.score DESC")
    List<Ad> findByPositionOnly(@Param("position") AdPosition position);

    // ========== 📊 Phase 1 모니터링/분석 메서드들 ==========

    /**
     * 4. Phase 1 활성 시나리오별 광고 개수 확인 (헬스체크용)
     */
    @Query("SELECT a.scenarioCode, COUNT(a) FROM Ad a WHERE " +
            "a.scenarioCode IN ('SC_DIET_EMO_A', 'SC_DIET_FUN_B', 'SC_COOK_EMO_A', " +
            "'SC_COOK_FUN_B', 'SC_MOM_EMO_A', 'SC_MOM_FUN_B', 'SC_DEFAULT_GENERAL') " +
            "AND a.status = 'ACTIVE' " +
            "AND a.startAt <= CURRENT_TIMESTAMP " +
            "AND a.endAt >= CURRENT_TIMESTAMP " +
            "GROUP BY a.scenarioCode")
    List<Object[]> countPhase1ActiveAds();

    /**
     * 5. A/B 테스트 성과 비교를 위한 메서드 (CTR 계산용)
     */
    @Query("SELECT a.scenarioCode, a.abTestGroup, " +
            "SUM(a.impressionCount), SUM(a.clickCount), " +
            "CASE WHEN SUM(a.impressionCount) > 0 " +
            "THEN CAST(SUM(a.clickCount) AS DOUBLE) / SUM(a.impressionCount) " +
            "ELSE 0 END as ctr " +
            "FROM Ad a WHERE " +
            "a.scenarioCode IN ('SC_DIET_EMO_A', 'SC_DIET_FUN_B', 'SC_COOK_EMO_A', " +
            "'SC_COOK_FUN_B', 'SC_MOM_EMO_A', 'SC_MOM_FUN_B') " +
            "AND a.status = 'ACTIVE' " +
            "GROUP BY a.scenarioCode, a.abTestGroup")
    List<Object[]> getAbTestPerformance();

    // ========== 🔍 단순 조회 메서드들 (JPA 메서드명 사용) ==========

    /**
     * 6. 시나리오별 모든 광고 조회 (단순 조건이므로 JPA 메서드명 사용)
     */
    List<Ad> findByScenarioCodeAndStatus(String scenarioCode, AdStatus status);

    /**
     * 7. A/B 그룹별 광고 조회
     */
    List<Ad> findByAbTestGroupAndStatus(AbTestGroup abTestGroup, AdStatus status);

    /**
     * 8. 세그먼트별 광고 조회
     */
    List<Ad> findByTargetSegmentAndStatus(SegmentType targetSegment, AdStatus status);

    // ========== 🚀 Phase 2 준비용 메서드들 (미래 확장) ==========

    /**
     * 9. 특정 시나리오 목록의 광고들 조회 (Phase 2에서 활용 예정)
     */
    @Query("SELECT a FROM Ad a WHERE " +
            "a.scenarioCode IN :scenarioCodes " +
            "AND a.status = 'ACTIVE' " +
            "AND a.startAt <= CURRENT_TIMESTAMP " +
            "AND a.endAt >= CURRENT_TIMESTAMP " +
            "ORDER BY a.scenarioCode, a.score DESC")
    List<Ad> findByScenarioCodes(@Param("scenarioCodes") List<String> scenarioCodes);

    /**
     * 10. 성과 기반 광고 순위 조회 (Phase 2 개인화에서 활용)
     */
    @Query("SELECT a FROM Ad a WHERE " +
            "a.preferredPosition = :position " +
            "AND a.status = 'ACTIVE' " +
            "AND a.impressionCount > 100 " +
            "ORDER BY (CAST(a.clickCount AS DOUBLE) / a.impressionCount) DESC")
    List<Ad> findTopPerformingAdsByPosition(@Param("position") AdPosition position);

}