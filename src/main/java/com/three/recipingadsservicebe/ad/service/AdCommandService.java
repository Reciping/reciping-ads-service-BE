package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.dto.AdCreateRequest;
import com.three.recipingadsservicebe.ad.dto.AdStatusUpdateRequest;
import com.three.recipingadsservicebe.ad.dto.AdUpdateRequest;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.advertiser.entity.Advertiser;
import com.three.recipingadsservicebe.advertiser.repository.AdvertiserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Slf4j(topic = "AdCommandService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdCommandService {

    private final AdRepository adRepository;
    private final AdvertiserRepository advertiserRepository;

    @Transactional
    public Long createAd(AdCreateRequest request) {
        Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
                .orElseThrow(() -> new IllegalArgumentException("광고주를 찾을 수 없습니다."));

        Ad ad = Ad.builder()
                .title(request.getTitle())
                .adType(request.getAdType())
                .imageUrl(request.getImageUrl())
                .targetUrl(request.getTargetUrl())
                .preferredPosition(request.getPreferredPosition())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(com.three.recipingadsservicebe.ad.enums.AdStatus.ACTIVE)
                .billingType(request.getBillingType())
                .budget(request.getBudget())
                .score(request.getScore())
                .spentAmount(0L)
                .clickCount(0L)
                .impressionCount(0L)
                .isDeleted(false)
                .advertiser(advertiser)
                // 🔧 새로운 A/B 테스트 필드들
                .abTestGroup(request.getAbTestGroup())
                .scenarioCode(request.getScenarioCode())
                // 🔧 새로운 행동태그 타겟팅 필드들
                .targetDemographicSegment(request.getTargetDemographicSegment())
                .targetEngagementLevel(request.getTargetEngagementLevel())
                .targetCookingStyle(request.getTargetCookingStyle())
                .createdAt(LocalDateTime.now())
                .build();

        Ad savedAd = adRepository.save(ad);
        log.info("광고 생성 완료 - adId: {}, title: {}", savedAd.getId(), savedAd.getTitle());

        return savedAd.getId();
    }

    @Transactional
    public void updateAd(Long adId, AdUpdateRequest request) {
        // 1. 요청 유효성 검증
        if (!request.hasAnyFieldToUpdate()) {
            throw new IllegalArgumentException("수정할 필드가 없습니다.");
        }

        if (!request.isValidDateRange()) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }

        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("광고를 찾을 수 없습니다."));

        // 2. 광고 상태 확인 (ACTIVE 상태에서만 수정 가능하도록)
        if (ad.getStatus() != AdStatus.ACTIVE) {
            log.warn("비활성 광고 수정 시도 - adId: {}, status: {}", adId, ad.getStatus());
            // 필요에 따라 예외 처리 또는 경고 로그만
        }

        ad.updateFrom(request);
        log.info("광고 수정 완료 - adId: {}", adId);
    }

    @Transactional
    public void deleteAd(Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("광고를 찾을 수 없습니다."));

        ad.softDelete();
        log.info("광고 삭제 완료 - adId: {}", adId);
    }




}
