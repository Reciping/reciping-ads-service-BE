package com.three.recipingadsservicebe.abtest.repository;

import com.three.recipingadsservicebe.abtest.entity.AbTestEvent;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AbTestEventRepository extends JpaRepository<AbTestEvent, Long> {

    /**
     * 기간별 A/B 테스트 결과 조회
     */
    List<AbTestEvent> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 사용자별 A/B 테스트 이력 조회
     */
    List<AbTestEvent> findByUserIdAndCreatedAtBetween(
            Long userId, LocalDateTime start, LocalDateTime end);

    /**
     * 간단한 CTR 통계 조회
     */
    @Query("""
        SELECT 
            e.abTestGroup as abGroup,
            e.eventType as eventType,
            COUNT(e) as eventCount
        FROM AbTestEvent e 
        WHERE e.createdAt BETWEEN :start AND :end
        GROUP BY e.abTestGroup, e.eventType
        """)
    List<AbTestStatistics> findCTRStatisticsByPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    interface AbTestStatistics {
        AbTestGroup getAbGroup();
        String getEventType();
        Long getEventCount();
    }


}
