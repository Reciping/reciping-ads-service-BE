package com.three.recipingadsservicebe.ad.repository;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdRepository extends JpaRepository<Ad, Long> {

    // ✅ 사용자 광고 노출용 쿼리 (score 기반 상위 광고 3개 노출)
    List<Ad> findTop3ByPreferredPositionAndStatusAndStartAtBeforeAndEndAtAfterOrderByScoreDesc(
            AdPosition preferredPosition,
            AdStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

}
