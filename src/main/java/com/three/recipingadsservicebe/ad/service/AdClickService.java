package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.enums.BillingType;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "AdClickService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdClickService {

    private final AdRepository adRepository;

    @Transactional
    public void handleClick(Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다"));

        ad.increaseClick();

        if (ad.getBillingType() == BillingType.CPC) {
            ad.increaseSpentAmount(1L);

            if (ad.getSpentAmount() >= ad.getBudget()) {
                ad.changeStatus(AdStatus.INACTIVE);
                log.info("광고 ID {}가 예산 초과로 INACTIVE 처리됨", adId);
            }
        }

        log.info("광고 클릭 처리 완료 - ID: {}, 클릭수: {}, 소진액: {}", adId, ad.getClickCount(), ad.getSpentAmount());
    }
}
