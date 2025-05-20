package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.dto.AdCreateRequest;
import com.three.recipingadsservicebe.ad.dto.AdResponse;
import com.three.recipingadsservicebe.ad.dto.AdStatusUpdateRequest;
import com.three.recipingadsservicebe.ad.dto.AdUpdateRequest;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.enums.TargetKeyword;
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
    @Transactional
    public Long createAd(AdCreateRequest request) {
        // 광고주 존재 여부 확인
        Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
                .orElseThrow(() -> new EntityNotFoundException("광고주가 존재하지 않습니다."));

        // 광고 객체 생성
        Ad ad = Ad.builder()
                .title(request.getTitle())
                .adType(request.getAdType())
                .imageUrl(request.getImageUrl())
                .targetUrl(request.getTargetUrl())
                .preferredPosition(request.getPreferredPosition())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(AdStatus.ACTIVE)
                .billingType(request.getBillingType())
                .budget(request.getBudget())
                .spentAmount(0L)
                .score(0f)
                .clickCount(0L)
                .impressionCount(0L)
                .abTestGroup(request.getAbTestGroup() != null ? request.getAbTestGroup() : AbTestGroup.CONTROL)
                .targetKeyword(request.getTargetKeyword() != null ? request.getTargetKeyword() : TargetKeyword.GENERAL)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .advertiser(advertiser)
                .build();

        return adRepository.save(ad).getId();
    }

    // --------------------------------------
    // [2] 광고 수정 (부분 수정 가능)
    // --------------------------------------
    @Transactional
    public void updateAd(Long adId, AdUpdateRequest request) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));

        ad.updateFrom(request);
    }

    // --------------------------------------
    // [3] 광고 상태 변경
    // --------------------------------------
    @Transactional
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
    @Transactional
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
