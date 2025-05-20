package com.three.recipingadsservicebe.ad.repository;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdRepository extends JpaRepository<Ad, Long> {


    // 광고 테이블에서 AB 그룹에 해당하는 광고만 가져오기
    List<Ad> findTop3ByPreferredPositionAndStatusAndAbTestGroupAndStartAtBeforeAndEndAtAfterOrderByScoreDesc(
            AdPosition preferredPosition,
            AdStatus status,
            AbTestGroup abTestGroup,
            LocalDateTime start,
            LocalDateTime end
    );



}
