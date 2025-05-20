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

@Slf4j(topic = "AdCommandService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdCommandService {

    private final AdRepository adRepository;
    private final AdvertiserRepository advertiserRepository;

    // --------------------------------------
    // [1] 광고 등록
    // --------------------------------------
    @Transactional
    public Long createAd(AdCreateRequest request) {
        Advertiser advertiser = advertiserRepository.findById(request.getAdvertiserId())
                .orElseThrow(() -> new EntityNotFoundException("광고주가 존재하지 않습니다."));

        Ad ad = request.toEntity(advertiser);
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
    // [6] 광고 삭제 (soft delete 방식)
    // --------------------------------------
    @Transactional
    public void deleteAd(Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));
        ad.softDelete();
    }




}
