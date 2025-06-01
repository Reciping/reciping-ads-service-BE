package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.dto.AdResponse;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.mapper.AdMapper;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j(topic = "AdQueryService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdQueryService {

    private final AdRepository adRepository;

    public AdResponse getAd(Long adId) {
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));
        return AdMapper.toResponse(ad);  // 매퍼 사용
    }

    public List<AdResponse> getAllAds() {
        return adRepository.findAll().stream()
                .map(AdMapper::toResponse)  // 매퍼 사용
                .collect(Collectors.toList());
    }

    public Ad findById(Long adId) {
        return adRepository.findById(adId)
                .orElseThrow(() -> new EntityNotFoundException("광고가 존재하지 않습니다."));
    }

}
