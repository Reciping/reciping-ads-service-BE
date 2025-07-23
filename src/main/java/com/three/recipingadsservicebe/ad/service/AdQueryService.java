package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j(topic = "AdQueryService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdQueryService {

    private final AdRepository adRepository;

    /**
     * 광고 단건 조회
     */
    public Ad getAdById(Long adId) {
        return adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("광고를 찾을 수 없습니다. ID: " + adId));
    }

    /**
     * 광고 목록 조회 (페이징 + 필터링)
     */
    public Page<Ad> getAds(Pageable pageable, String status, Long advertiserId) {
        // 상태 필터가 있는 경우
        if (status != null && !status.isEmpty()) {
            try {
                AdStatus adStatus = AdStatus.valueOf(status.toUpperCase());

                if (advertiserId != null) {
                    return adRepository.findByStatusAndAdvertiserIdOrderByCreatedAtDesc(
                            adStatus, advertiserId, pageable);
                } else {
                    return adRepository.findByStatusOrderByCreatedAtDesc(adStatus, pageable);
                }
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 광고 상태 필터: {}", status);
                // 잘못된 상태값인 경우 전체 조회로 fallback
            }
        }

        // 광고주 필터만 있는 경우
        if (advertiserId != null) {
            return adRepository.findByAdvertiserIdOrderByCreatedAtDesc(advertiserId, pageable);
        }

        // 필터 없이 전체 조회
        return adRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 광고주별 광고 수 조회
     */
    public long countAdsByAdvertiser(Long advertiserId) {
        return adRepository.countByAdvertiserId(advertiserId);
    }

    /**
     * 활성 광고 수 조회
     */
    public long countActiveAds() {
        return adRepository.countByStatus(AdStatus.ACTIVE);
    }

    /**
     * 특정 광고주의 활성 광고 조회
     */
    public Page<Ad> getActiveAdsByAdvertiser(Long advertiserId, Pageable pageable) {
        return adRepository.findByStatusAndAdvertiserIdOrderByCreatedAtDesc(
                AdStatus.ACTIVE, advertiserId, pageable);
    }

}
