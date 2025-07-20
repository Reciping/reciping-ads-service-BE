package com.three.recipingadsservicebe.ad.repository;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 페이징 조회 메서드들
     */
    Page<Ad> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Ad> findByStatusOrderByCreatedAtDesc(AdStatus status, Pageable pageable);

    Page<Ad> findByAdvertiserIdOrderByCreatedAtDesc(Long advertiserId, Pageable pageable);

    Page<Ad> findByStatusAndAdvertiserIdOrderByCreatedAtDesc(
            AdStatus status, Long advertiserId, Pageable pageable);

    /**
     * 카운트 메서드들
     */
    long countByAdvertiserId(Long advertiserId);

    long countByStatus(AdStatus status);

    /**
     * 검색 메서드들
     */
    @Query("""
    SELECT a FROM Ad a 
    WHERE a.title LIKE %:keyword% 
    OR a.targetUrl LIKE %:keyword%
    ORDER BY a.createdAt DESC
    """)
    Page<Ad> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 성과 조회 메서드들
     */
    @Query("""
    SELECT a FROM Ad a 
    WHERE a.status = 'ACTIVE' 
    AND a.clickCount > 0 
    ORDER BY (CAST(a.clickCount AS float) / CAST(a.impressionCount AS float)) DESC
    """)
    List<Ad> findTopPerformingAds(Pageable pageable);

}