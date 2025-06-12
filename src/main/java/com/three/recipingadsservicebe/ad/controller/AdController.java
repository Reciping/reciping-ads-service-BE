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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
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
import java.util.concurrent.atomic.AtomicInteger;
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

    // ✅ 메트릭 수집을 위한 새로운 의존성들
    private final MeterRegistry meterRegistry;
    private final Timer adServeTimer;
    private final AtomicInteger activeAdsGauge;


    /**
     * 광고 등록 (메트릭 추가)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Long> createAd(@Valid @RequestBody AdCreateRequest request,
                                         HttpServletRequest httpRequest) {
        Long adId = adService.createAd(request);

        // 메트릭 수집
        meterRegistry.counter("ads_created_total", Tags.of(
                "ad_type", request.getAdType().toString(),
                "position", request.getPreferredPosition().toString(),
                "advertiser_id", request.getAdvertiserId().toString()
        )).increment();


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
     * 사용자 광고 노출 (수정된 메트릭)
     */
    /**
     * 🔧 완전 개선된 광고 서빙 메서드
     */
    @GetMapping("/public/serve")
    public ResponseEntity<Map<String, List<AdResponse>>> serveAllAds(HttpServletRequest httpRequest) {
        Timer.Sample timerSample = Timer.start(meterRegistry);

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

        // ③ 🔧 개선: 상세 정보가 포함된 광고 추천
        AdSelector.AdSelectionResult selectionResult = adSelector.getAllAdsForUserWithDetails(userInfo);
        Map<String, List<Ad>> adsByPosition = selectionResult.getAdsByPosition();
        SegmentType segment = selectionResult.getUserSegment();

        // ④ 응답 변환
        Map<String, List<AdResponse>> result = new HashMap<>();

        // ⑤ 🔧 완전 개선된 광고 서빙 로깅
        try {
            // 전체 서빙 정보
            Map<String, Object> servePayload = new HashMap<>();
            servePayload.put("userSegment", segment.name());
            servePayload.put("totalAds", selectionResult.getTotalAds());
            servePayload.put("hasUser", userId != null);

            // 위치별 선택 정보 요약
            Map<String, Object> positionSummary = new HashMap<>();
            for (Map.Entry<String, AdSelector.AdSelectionResult.PositionSelectionInfo> entry :
                    selectionResult.getSelectionInfo().entrySet()) {
                String position = entry.getKey();
                AdSelector.AdSelectionResult.PositionSelectionInfo info = entry.getValue();

                Map<String, Object> posInfo = new HashMap<>();
                posInfo.put("adCount", info.getSelectedCount());
                posInfo.put("originalScenario", info.getOriginalScenario());
                posInfo.put("finalScenario", info.getFinalScenario());
                posInfo.put("isFallback", info.isFallback());
                posInfo.put("fallbackLevel", info.getFallbackLevel());
                posInfo.put("selectionTimeMs", info.getSelectionTimeMs());

                positionSummary.put(position, posInfo);
            }
            servePayload.put("positionSummary", positionSummary);

            for (Map.Entry<String, List<Ad>> entry : adsByPosition.entrySet()) {
                String position = entry.getKey();
                List<Ad> ads = entry.getValue();
                AdSelector.AdSelectionResult.PositionSelectionInfo selectionInfo =
                        selectionResult.getSelectionInfo().get(position);

                // 개별 광고 노출 메트릭 및 로깅
                for (Ad ad : ads) {
                    // 메트릭 수집 (기존과 동일)
                    meterRegistry.counter("ads_impressions_total", Tags.of(
                            "position", position,
                            "segment", segment.name(),
                            "scenario", ad.getScenarioCode() != null ? ad.getScenarioCode() : "UNKNOWN",
                            "advertiser_id", ad.getAdvertiser().getId().toString(),
                            "is_fallback", String.valueOf(selectionInfo.isFallback())
                    )).increment();

                    if (ad.getAbTestGroup() != null && ad.getScenarioCode() != null) {
                        meterRegistry.counter("ads_abtest_impressions_total", Tags.of(
                                "ab_group", ad.getAbTestGroup().toString(),
                                "scenario", ad.getScenarioCode(),
                                "segment", segment.name(),
                                "is_fallback", String.valueOf(selectionInfo.isFallback())
                        )).increment();
                    }

                    // 🔧 핵심 개선: 완전한 노출 로그 페이로드 (폴백 정보 포함)
                    Map<String, Object> impressionPayload = new HashMap<>();

                    // 기본 광고 정보
                    impressionPayload.put("position", position);
                    impressionPayload.put("scenario", ad.getScenarioCode());
                    impressionPayload.put("targetSegment", ad.getTargetSegment());
                    impressionPayload.put("abGroup", ad.getAbTestGroup());
                    impressionPayload.put("billingType", ad.getBillingType());
                    impressionPayload.put("advertiserId", ad.getAdvertiser().getId());

                    // 🔧 Phase 1 검증 정보
                    impressionPayload.put("userSegment", segment.name());
                    impressionPayload.put("isActivePosition", isActivePhase1Position(position));
                    impressionPayload.put("isActiveSegment", isActivePhase1Segment(segment.name()));
                    impressionPayload.put("isActiveScenario", isActivePhase1Scenario(ad.getScenarioCode()));

                    // 🔧 폴백 정보 (이제 완전히 사용 가능!)
                    impressionPayload.put("originalScenario", selectionInfo.getOriginalScenario());
                    impressionPayload.put("finalScenario", selectionInfo.getFinalScenario());
                    impressionPayload.put("isFallback", selectionInfo.isFallback());
                    impressionPayload.put("fallbackLevel", selectionInfo.getFallbackLevel());
                    impressionPayload.put("fallbackSteps", selectionInfo.getFallbackSteps());

                    // 🔧 성능 및 상태 정보
                    impressionPayload.put("selectionTimeMs", selectionInfo.getSelectionTimeMs());
                    impressionPayload.put("candidateCount", selectionInfo.getCandidateCount());
                    impressionPayload.put("currentImpression", ad.getImpressionCount());
                    impressionPayload.put("currentCtr", ad.calculateCTR());

                    // CTR 분류
                    double ctr = ad.calculateCTR();
                    if (ctr >= 0.03) {
                        impressionPayload.put("ctrBucket", "HIGH");
                    } else if (ctr >= 0.015) {
                        impressionPayload.put("ctrBucket", "MEDIUM");
                    } else {
                        impressionPayload.put("ctrBucket", "LOW");
                    }

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

                    ad.increaseImpression();
                }

                result.put(position, ads.stream().map(AdMapper::toResponse).toList());
            }

            // 전체 서빙 로그
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
        } finally {
            timerSample.stop(adServeTimer);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 🔧 Phase 1 검증 헬퍼 메서드들 (AdLogger와 동일한 로직)
     */
    private boolean isActivePhase1Position(String position) {
        return "MAIN_TOP".equals(position) || "MAIN_MIDDLE".equals(position);
    }

    private boolean isActivePhase1Segment(String segment) {
        return segment != null && (
                segment.contains("DIET_FEMALE_ALL") ||
                        segment.contains("MALE_COOK_STARTER") ||
                        segment.contains("ACTIVE_MOM") ||
                        segment.contains("GENERAL_ALL")
        );
    }

    private boolean isActivePhase1Scenario(String scenario) {
        return scenario != null && (
                scenario.startsWith("SC_DIET_") ||
                        scenario.startsWith("SC_COOK_") ||
                        scenario.startsWith("SC_MOM_") ||
                        scenario.equals("SC_DEFAULT_GENERAL")
        );
    }

    /**
     * 🔧 개선: 클릭 로깅도 완전한 정보 포함
     */
    @PostMapping("/{adId}/click")
    public ResponseEntity<Void> clickAd(@PathVariable Long adId,
                                        HttpServletRequest httpRequest) {
        Ad ad = adQueryService.findById(adId);

        // 메트릭 수집 (기존과 동일)
        meterRegistry.counter("ads_clicks_total", Tags.of(
                "position", ad.getPreferredPosition().toString(),
                "scenario", ad.getScenarioCode() != null ? ad.getScenarioCode() : "UNKNOWN",
                "segment", ad.getTargetSegment() != null ? ad.getTargetSegment().toString() : "UNKNOWN",
                "advertiser_id", ad.getAdvertiser().getId().toString()
        )).increment();

        if (ad.getAbTestGroup() != null && ad.getScenarioCode() != null) {
            meterRegistry.counter("ads_abtest_clicks_total", Tags.of(
                    "ab_group", ad.getAbTestGroup().toString(),
                    "scenario", ad.getScenarioCode(),
                    "segment", ad.getTargetSegment() != null ? ad.getTargetSegment().toString() : "UNKNOWN"
            )).increment();
        }

        // 🔧 개선: 완전한 클릭 로그 페이로드
        try {
            Map<String, Object> clickPayload = new HashMap<>();

            // 기본 정보
            clickPayload.put("position", ad.getPreferredPosition());
            clickPayload.put("scenario", ad.getScenarioCode());
            clickPayload.put("targetSegment", ad.getTargetSegment());
            clickPayload.put("abGroup", ad.getAbTestGroup());
            clickPayload.put("billingType", ad.getBillingType());
            clickPayload.put("advertiserId", ad.getAdvertiser().getId());
            clickPayload.put("ctr", ad.calculateCTR());

            // 🔧 추가: Phase 1 검증 정보
            clickPayload.put("isActivePosition", isActivePhase1Position(ad.getPreferredPosition().toString()));
            clickPayload.put("isActiveScenario", isActivePhase1Scenario(ad.getScenarioCode()));
            if (ad.getTargetSegment() != null) {
                clickPayload.put("isActiveSegment", isActivePhase1Segment(ad.getTargetSegment().toString()));
            }

            // 🔧 추가: 클릭 시점 정보
            clickPayload.put("currentImpression", ad.getImpressionCount());
            clickPayload.put("currentClickCount", ad.getClickCount());

            // CTR 분류
            double ctr = ad.calculateCTR();
            if (ctr >= 0.03) {
                clickPayload.put("ctrBucket", "HIGH");
            } else if (ctr >= 0.015) {
                clickPayload.put("ctrBucket", "MEDIUM");
            } else {
                clickPayload.put("ctrBucket", "LOW");
            }

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

        adClickService.handleClick(adId);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetailsImpl user) {
            return user.getUserId().toString();
        }
        return null;
    }
}
