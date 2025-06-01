package com.three.recipingadsservicebe.ad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.three.recipingadsservicebe.ad.dto.*;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.mapper.AdMapper;
import com.three.recipingadsservicebe.ad.service.AdClickService;
import com.three.recipingadsservicebe.ad.service.AdCommandService;
import com.three.recipingadsservicebe.ad.service.AdQueryService;
import com.three.recipingadsservicebe.ad.service.selector.AdSelector;
import com.three.recipingadsservicebe.feign.UserFeignClient;
import com.three.recipingadsservicebe.log.logger.AdLogger;
import com.three.recipingadsservicebe.log.dto.LogType;
import com.three.recipingadsservicebe.global.security.UserDetailsImpl;
import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import com.three.recipingadsservicebe.segment.service.SegmentCalculatorUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/ads")
@RestController
public class AdController {

    private final AdCommandService adService;
    private final AdQueryService adQueryService;
    private final AdClickService adClickService;
    private final AdSelector adSelector;
    private final UserFeignClient userFeignClient;
    private final ObjectMapper objectMapper;
    private final SegmentCalculatorUtil segmentCalculatorUtil;

    /**
     * 광고 등록
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Long> createAd(@Valid @RequestBody AdCreateRequest request,
                                         HttpServletRequest httpRequest) {
        Long adId = adService.createAd(request);

        // 광고 생성 로깅
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("advertiserId", request.getAdvertiserId());
            payloadMap.put("adType", request.getAdType());
            payloadMap.put("position", request.getPreferredPosition());
            payloadMap.put("budget", request.getBudget());
            payloadMap.put("billingType", request.getBillingType());

            AdLogger.track(
                    log,
                    LogType.AD_CREATE,
                    httpRequest.getRequestURI(),
                    httpRequest.getMethod(),
                    getCurrentUserId(),
                    null,
                    adId.toString(),
                    objectMapper.writeValueAsString(payloadMap),
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("광고 생성 로깅 실패: {}", e.getMessage());
        }

        return ResponseEntity.ok(adId);
    }

    /**
     * 광고 전체 목록 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AdResponse>> getAllAds(HttpServletRequest httpRequest) {
        List<AdResponse> ads = adQueryService.getAllAds();

        // 광고 목록 조회 로깅
        AdLogger.track(
                log,
                LogType.VIEW,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                getCurrentUserId(),
                null,
                null,
                String.format("{\"action\":\"list_ads\",\"count\":%d}", ads.size()),
                httpRequest
        );

        return ResponseEntity.ok(ads);
    }

    /**
     * 광고 단건 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{adId}")
    public ResponseEntity<AdResponse> getAd(@PathVariable Long adId,
                                            HttpServletRequest httpRequest) {
        AdResponse ad = adQueryService.getAd(adId);

        // 광고 상세 조회 로깅
        AdLogger.track(
                log,
                LogType.VIEW,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                getCurrentUserId(),
                null,
                adId.toString(),
                String.format("{\"action\":\"view_ad_detail\",\"adStatus\":\"%s\"}", ad.getStatus()),
                httpRequest
        );

        return ResponseEntity.ok(ad);
    }

    /**
     * 광고 수정
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{adId}")
    public ResponseEntity<Void> updateAd(@PathVariable Long adId,
                                         @Valid @RequestBody AdUpdateRequest request,
                                         HttpServletRequest httpRequest) {
        adService.updateAd(adId, request);

        // 광고 수정 로깅
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("action", "update_ad");
            payloadMap.put("fields", request.getClass().getDeclaredFields().length);

            AdLogger.track(
                    log,
                    LogType.AD_UPDATE,
                    httpRequest.getRequestURI(),
                    httpRequest.getMethod(),
                    getCurrentUserId(),
                    null,
                    adId.toString(),
                    objectMapper.writeValueAsString(payloadMap),
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("광고 수정 로깅 실패: {}", e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * 광고 상태 변경
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{adId}/status")
    public ResponseEntity<Void> updateAdStatus(@PathVariable Long adId,
                                               @RequestBody AdStatusUpdateRequest request,
                                               HttpServletRequest httpRequest) {
        adService.updateAdStatus(adId, request);

        // 광고 상태 변경 로깅
        AdLogger.track(
                log,
                LogType.AD_STATUS_CHANGE,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                getCurrentUserId(),
                null,
                adId.toString(),
                String.format("{\"oldStatus\":\"unknown\",\"newStatus\":\"%s\"}", request.getStatus()),
                httpRequest
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * 광고 삭제 (soft delete)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{adId}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long adId,
                                         HttpServletRequest httpRequest) {
        adService.deleteAd(adId);

        // 광고 삭제 로깅
        AdLogger.track(
                log,
                LogType.AD_DELETE,
                httpRequest.getRequestURI(),
                httpRequest.getMethod(),
                getCurrentUserId(),
                null,
                adId.toString(),
                "{\"action\":\"soft_delete\"}",
                httpRequest
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 광고 노출 (위치 기반)
     */
    @GetMapping("/public/serve")
    public ResponseEntity<Map<String, List<AdResponse>>> serveAllAds(HttpServletRequest httpRequest) {
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

        // ③ 위치별 광고 추천
        Map<String, List<Ad>> adsByPosition = adSelector.getAllAdsForUser(userInfo);

        // ④ 응답 변환
        Map<String, List<AdResponse>> result = new HashMap<>();

        // ⑤ 광고 서빙 로깅 (전체 서빙 정보)
        try {
            Map<String, Object> servePayload = new HashMap<>();
            SegmentType segment = userInfo != null ? segmentCalculatorUtil.calculate(userInfo) : SegmentType.GENERAL_ALL;
            servePayload.put("userSegment", segment.name());

            servePayload.put("totalAds", adsByPosition.values().stream().mapToInt(List::size).sum());

            Map<String, Object> positionInfo = new HashMap<>();
            for (Map.Entry<String, List<Ad>> entry : adsByPosition.entrySet()) {
                String position = entry.getKey();
                List<Ad> ads = entry.getValue();

                // 각 위치별 광고 정보
                List<Map<String, Object>> adInfoList = ads.stream().map(ad -> {
                    Map<String, Object> adInfo = new HashMap<>();
                    adInfo.put("adId", ad.getId());
                    adInfo.put("scenario", ad.getScenarioCode());
                    adInfo.put("targetSegment", ad.getTargetSegment());
                    adInfo.put("abGroup", ad.getAbTestGroup());
                    return adInfo;
                }).collect(Collectors.toList());

                positionInfo.put(position, adInfoList);

                // 개별 광고 노출 로깅
                for (Ad ad : ads) {
                    Map<String, Object> impressionPayload = new HashMap<>();
                    impressionPayload.put("position", position);
                    impressionPayload.put("scenario", ad.getScenarioCode());
                    impressionPayload.put("targetSegment", ad.getTargetSegment());
                    impressionPayload.put("abGroup", ad.getAbTestGroup());
                    impressionPayload.put("billingType", ad.getBillingType());
                    impressionPayload.put("advertiserId", ad.getAdvertiser().getId());

                    AdLogger.track(
                            log,
                            LogType.AD_IMPRESSION,
                            httpRequest.getRequestURI(),
                            httpRequest.getMethod(),
                            userId != null ? userId.toString() : null,
                            null,
                            ad.getId().toString(),
                            objectMapper.writeValueAsString(impressionPayload),
                            httpRequest
                    );

                    // 노출 카운트 증가 (비동기 처리 고려)
                    ad.increaseImpression();
                }

                result.put(position, ads.stream().map(AdMapper::toResponse).toList());
            }

            servePayload.put("positions", positionInfo);

            // 전체 서빙 로깅
            AdLogger.track(
                    log,
                    LogType.AD_SERVE,
                    httpRequest.getRequestURI(),
                    httpRequest.getMethod(),
                    userId != null ? userId.toString() : null,
                    null,
                    null,
                    objectMapper.writeValueAsString(servePayload),
                    httpRequest
            );

        } catch (Exception e) {
            log.warn("광고 서빙 로깅 실패: {}", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 광고 클릭
     */
    @PostMapping("/{adId}/click")
    public ResponseEntity<Void> clickAd(@PathVariable Long adId,
                                        HttpServletRequest httpRequest) {
        // 클릭 정보 조회 (position, scenario 등)
        Ad ad = adQueryService.findById(adId);

        // 광고 클릭 로깅
        try {
            Map<String, Object> clickPayload = new HashMap<>();
            clickPayload.put("position", ad.getPreferredPosition());
            clickPayload.put("scenario", ad.getScenarioCode());
            clickPayload.put("targetSegment", ad.getTargetSegment());
            clickPayload.put("abGroup", ad.getAbTestGroup());
            clickPayload.put("billingType", ad.getBillingType());
            clickPayload.put("advertiserId", ad.getAdvertiser().getId());
            clickPayload.put("ctr", ad.calculateCTR());

            AdLogger.track(
                    log,
                    LogType.AD_CLICK,
                    httpRequest.getRequestURI(),
                    httpRequest.getMethod(),
                    getCurrentUserId(),
                    null,
                    adId.toString(),
                    objectMapper.writeValueAsString(clickPayload),
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("광고 클릭 로깅 실패: {}", e.getMessage());
        }

        // 클릭 처리
        adClickService.handleClick(adId);

        return ResponseEntity.ok().build();
    }

    /**
     * 현재 로그인한 사용자 ID 조회
     */
    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetailsImpl user) {
            return user.getUserId().toString();
        }
        return null;
    }

}
