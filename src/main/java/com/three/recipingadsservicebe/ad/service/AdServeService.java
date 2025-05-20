package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.enums.BillingType;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
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

    @Transactional
    public List<Ad> serveAdsByPosition(String position) {
        AdPosition pos = AdPosition.valueOf(position.toUpperCase());
        List<Ad> ads = adRepository.findTop3ByPreferredPositionAndStatusAndStartAtBeforeAndEndAtAfterOrderByScoreDesc(
                pos, AdStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );

        for (Ad ad : ads) {
            ad.increaseImpression();
            if (ad.getBillingType() == BillingType.CPM) {
                ad.increaseSpentAmount(1L);
                if (ad.getSpentAmount() >= ad.getBudget()) {
                    ad.changeStatus(AdStatus.INACTIVE);
                }
            }
        }

        return ads;
    }
}
