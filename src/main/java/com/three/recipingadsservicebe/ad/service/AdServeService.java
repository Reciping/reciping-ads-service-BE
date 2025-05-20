package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.enums.BillingType;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.global.util.AbTestAssigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j(topic = "AdServeService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdServeService {

    private final AdRepository adRepository;

    // 광고 노출 서비스 (AB 그룹별로 필터링된 광고 3개 제공)
    @Transactional
    public List<Ad> serveAdsByAbGroup(String position, Long userId) {
        AdPosition pos = AdPosition.valueOf(position.toUpperCase());
        AbTestGroup group = AbTestAssigner.assign(userId.toString());

        LocalDateTime now = LocalDateTime.now();

        // AB 그룹에 맞는 광고만 조회
        List<Ad> ads = adRepository.findTop3ByPreferredPositionAndStatusAndAbTestGroupAndStartAtBeforeAndEndAtAfterOrderByScoreDesc(
                pos, AdStatus.ACTIVE, group, now, now
        );

        for (Ad ad : ads) {
            ad.increaseImpression();

            if (ad.getBillingType() == BillingType.CPM) {
                ad.increaseSpentAmount(1L);

                // 예산 초과 시 광고 상태 변경
                if (ad.getSpentAmount() >= ad.getBudget()) {
                    ad.changeStatus(AdStatus.INACTIVE);
                }
            }
        }

        return ads;
    }
}
