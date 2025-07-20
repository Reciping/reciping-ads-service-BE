package com.three.recipingadsservicebe.ad.repository;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface AdRepository extends JpaRepository<Ad, Long> {
    /**
     * 행동태그 완전 매치 광고 검색
     */
    @Query("""
        SELECT a FROM Ad a 
        WHERE a.preferredPosition = :position 
        AND a.status = 'ACTIVE' 
        AND a.isDeleted = false
        AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP)
        AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP)
        AND (a.budget IS NULL OR a.spentAmount IS NULL OR a.spentAmount < a.budget)
        AND a.scenarioCode = :scenarioCode
        AND a.targetDemographicSegment = :demographic
        AND a.targetEngagementLevel = :engagement
        AND a.targetCookingStyle = :cookingStyle
        ORDER BY a.score DESC, a.createdAt DESC
        """)
    List<Ad> findByBehaviorTargeting(
            @Param("position") AdPosition position,
            @Param("scenarioCode") String scenarioCode,
            @Param("demographic") DemographicSegment demographic,
            @Param("engagement") EngagementLevel engagement,
            @Param("cookingStyle") CookingStylePreference cookingStyle);

    /**
     * 행동태그 부분 매치 광고 검색
     */
    @Query("""
        SELECT a FROM Ad a 
        WHERE a.preferredPosition = :position 
        AND a.status = 'ACTIVE' 
        AND a.isDeleted = false
        AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP)
        AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP)
        AND (a.budget IS NULL OR a.spentAmount IS NULL OR a.spentAmount < a.budget)
        AND a.scenarioCode = :scenarioCode
        AND (
            a.targetCookingStyle = :cookingStyle OR
            a.targetEngagementLevel = :engagement OR
            a.targetDemographicSegment = :demographic OR
            (a.targetDemographicSegment IS NULL AND a.targetEngagementLevel IS NULL AND a.targetCookingStyle IS NULL)
        )
        ORDER BY 
            CASE 
                WHEN a.targetCookingStyle = :cookingStyle THEN 1
                WHEN a.targetEngagementLevel = :engagement THEN 2  
                WHEN a.targetDemographicSegment = :demographic THEN 3
                ELSE 4
            END,
            a.score DESC, 
            a.createdAt DESC
        """)
    List<Ad> findByPartialBehaviorTargeting(
            @Param("position") AdPosition position,
            @Param("scenarioCode") String scenarioCode,
            @Param("demographic") DemographicSegment demographic,
            @Param("engagement") EngagementLevel engagement,
            @Param("cookingStyle") CookingStylePreference cookingStyle);

    /**
     * 시나리오 코드별 활성 광고 조회 (랜덤 서빙용)
     */
    @Query("""
        SELECT a FROM Ad a 
        WHERE a.preferredPosition = :position 
        AND a.status = 'ACTIVE' 
        AND a.isDeleted = false
        AND (a.startAt IS NULL OR a.startAt <= CURRENT_TIMESTAMP)
        AND (a.endAt IS NULL OR a.endAt >= CURRENT_TIMESTAMP)
        AND (a.budget IS NULL OR a.spentAmount IS NULL OR a.spentAmount < a.budget)
        AND a.scenarioCode = :scenarioCode
        ORDER BY a.score DESC, a.createdAt DESC
        """)
    List<Ad> findByScenarioCodeAndPosition(
            @Param("scenarioCode") String scenarioCode,
            @Param("position") AdPosition position);

}