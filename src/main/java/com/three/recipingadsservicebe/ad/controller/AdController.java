package com.three.recipingadsservicebe.ad.controller;

import com.three.recipingadsservicebe.ad.dto.*;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.mapper.AdMapper;
import com.three.recipingadsservicebe.ad.service.AdClickService;
import com.three.recipingadsservicebe.ad.service.AdCommandService;
import com.three.recipingadsservicebe.ad.service.AdQueryService;
import com.three.recipingadsservicebe.ad.service.AdServeService;
import com.three.recipingadsservicebe.global.security.UserDetailsImpl;
import com.three.recipingadsservicebe.global.util.AbTestAssigner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/ads")
@RestController
public class AdController {

    private final AdServeService adServeService;
    private final AdCommandService adService;
    private final AdQueryService adQueryService;
    private final AdClickService adClickService;

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
    public ResponseEntity<AdServeResponse> serveAds(@RequestParam String position, @AuthenticationPrincipal UserDetailsImpl user) {

        Long userId = user.getUserId();
        AbTestGroup abGroup = AbTestAssigner .assign(userId.toString());

        List<Ad> ads = adServeService.serveAdsByAbGroup(position, userId);
        return ResponseEntity.ok(new AdServeResponse(
                ads.stream().map(AdMapper::toResponse).toList(),
                abGroup
        ));
    }

    /**
     * 광고 클릭
     */

    @PostMapping("/{adId}/click")
    public ResponseEntity<Void> clickAd(@PathVariable Long adId) {
        adClickService.handleClick(adId);
        return ResponseEntity.ok().build();
    }

}
