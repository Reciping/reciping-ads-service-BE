package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.dto.AdCreateRequest;
import com.three.recipingadsservicebe.ad.dto.AdResponse;
import com.three.recipingadsservicebe.ad.dto.AdStatusUpdateRequest;
import com.three.recipingadsservicebe.ad.dto.AdUpdateRequest;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.advertiser.entity.Advertiser;
import com.three.recipingadsservicebe.advertiser.repository.AdvertiserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j(topic = "AdService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdService {

    private final AdRepository adRepository;
    private final AdvertiserRepository advertiserRepository;

    // --------------------------------------
    // [1] 광고 등록
    // --------------------------------------
    public Long createAd(AdCreateRequest request) {
        // 광고주 존재 여부 확인
        Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
                .orElseThrow(() -> new EntityNotFoundException("광고주가 존재하지 않습니다."));

        // 광고 객체 생성 (초기 상태는 ACTIVE + spentAmount = 0)
        Ad ad = new Ad(
                null,
                request.getTitle(),
                request.getAdType(),
                request.getImageUrl(),
                request.getTargetUrl(),
                request.getPreferredPosition(),
                request.getStartAt(),
                request.getEndAt(),
                AdStatus.ACTIVE,
                request.getBillingType(),
                request.getBudget(),
                0L,            // spentAmount 초기값
                0f,            // score 초기값
                LocalDateTime.now(), null, null, false,
                advertiser
        );

        return adRepository.save(ad).getId();
    }

    // --------------------------------------
    // [2] 광고 수정 (부분 수정 가능)
    // --------------------------------------
    public void updateAd(Long adId, AdUpdateRequest request) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));

        ad.updateFrom(request);
    }

    // --------------------------------------
    // [3] 광고 상태 변경
    // --------------------------------------
    public void updateAdStatus(Long adId, AdStatusUpdateRequest request) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));
        ad.changeStatus(request.getStatus());
    }

    // --------------------------------------
    // [4] 광고 단건 조회
    // --------------------------------------
    public AdResponse getAd(Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));
        return mapToResponse(ad);
    }

    // --------------------------------------
    // [5] 광고 전체 목록 조회
    // --------------------------------------
    public List<AdResponse> getAllAds() {
        return adRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --------------------------------------
    // [6] 광고 삭제 (soft delete 방식)
    // --------------------------------------
    public void deleteAd(Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));
        ad.softDelete();
    }

    // --------------------------------------
    // [7] 광고 노출 API (위치 기준 + 조건 필터링)
    // --------------------------------------
    public List<AdResponse> serveAdsByPosition(String position) {
        AdPosition pos = AdPosition.valueOf(position.toUpperCase());  // 문자열을 Enum으로
        List<Ad> ads = adRepository.findTop3ByPreferredPositionAndStatusAndStartAtBeforeAndEndAtAfterOrderByScoreDesc(
                pos, AdStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );
        return ads.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // --------------------------------------
    // [공통] Ad → AdResponse 변환
    // --------------------------------------
    private AdResponse mapToResponse(Ad ad) {
        return AdResponse.builder()
                .id(ad.getId())
                .advertiserId(ad.getAdvertiser().getId())
                .title(ad.getTitle())
                .adType(ad.getAdType())
                .imageUrl(ad.getImageUrl())
                .targetUrl(ad.getTargetUrl())
                .preferredPosition(ad.getPreferredPosition())
                .startAt(ad.getStartAt())
                .endAt(ad.getEndAt())
                .billingType(ad.getBillingType())
                .budget(ad.getBudget())
                .spentAmount(ad.getSpentAmount())
                .status(ad.getStatus())
                .score(ad.getScore())
                .build();
    }


}
