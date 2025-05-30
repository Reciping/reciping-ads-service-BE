package com.three.recipingadsservicebe.ad.controller;

import com.three.recipingadsservicebe.abtest.repository.AbTestScenarioRepository;
import com.three.recipingadsservicebe.ad.dto.*;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.mapper.AdMapper;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.ad.service.AdClickService;
import com.three.recipingadsservicebe.ad.service.AdCommandService;
import com.three.recipingadsservicebe.ad.service.AdQueryService;
import com.three.recipingadsservicebe.ad.service.selector.AdSelector;
import com.three.recipingadsservicebe.feign.UserFeignClient;
import com.three.recipingadsservicebe.global.security.UserDetailsImpl;
import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.service.ABTestManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/api/v1/ads")
@RestController
public class AdController {

    private final AdCommandService adService;
    private final AdQueryService adQueryService;
    private final AdClickService adClickService;
    private final AdSelector adSelector;
    private final UserFeignClient userFeignClient;


    /**
     * 광고 등록
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Long> createAd(@Valid @RequestBody AdCreateRequest request) {
        Long adId = adService.createAd(request);
        return ResponseEntity.ok(adId);
    }

    /**
     * 광고 전체 목록 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AdResponse>> getAllAds() {
        return ResponseEntity.ok(adQueryService.getAllAds());
    }

    /**
     * 광고 단건 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{adId}")
    public ResponseEntity<AdResponse> getAd(@PathVariable Long adId) {
        return ResponseEntity.ok(adQueryService.getAd(adId));
    }

    /**
     * 광고 수정
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{adId}")
    public ResponseEntity<Void> updateAd(@PathVariable Long adId,
                                         @Valid @RequestBody AdUpdateRequest request) {
        adService.updateAd(adId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 광고 상태 변경
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{adId}/status")
    public ResponseEntity<Void> updateAdStatus(@PathVariable Long adId,
                                               @RequestBody AdStatusUpdateRequest request) {
        adService.updateAdStatus(adId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 광고 삭제 (soft delete)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{adId}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long adId) {
        adService.deleteAd(adId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 광고 노출 (위치 기반)
     */



    @GetMapping("/public/serve")
    public ResponseEntity<Map<String, List<AdResponse>>> serveAllAds() {
        Long userId = null;
        UserInfoDto userInfo = null;

        // ① 로그인 유저 정보 확인
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetailsImpl user) {
            userId = user.getUserId();
        }

        // ② 유저 정보가 있으면 조회
        if (userId != null) {
            userInfo = userFeignClient.getUserInfo(userId);
        }

        // ③ 위치별 광고 추천 (userInfo가 null이면 비회원 → 기본 CONTROL 광고)
        Map<String, List<Ad>> adsByPosition = adSelector.getAllAdsForUser(userInfo);

        // ④ 응답 변환
        Map<String, List<AdResponse>> result = new HashMap<>();
        for (Map.Entry<String, List<Ad>> entry : adsByPosition.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream().map(AdMapper::toResponse).toList());
        }

        return ResponseEntity.ok(result);
    }



    /**
     * 광고 클릭
     */

    @PostMapping("/{adId}/click")
    public ResponseEntity<Void> clickAd(@PathVariable Long adId) {
        adClickService.handleClick(adId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/test-feign")
    public ResponseEntity<UserInfoDto> testFeign() {
        Long testUserId = 1L;
        UserInfoDto userInfo = userFeignClient.getUserInfo(testUserId);
        return ResponseEntity.ok(userInfo);
    }


}
