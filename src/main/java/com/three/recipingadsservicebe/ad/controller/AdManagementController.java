package com.three.recipingadsservicebe.ad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.three.recipingadsservicebe.ad.dto.*;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.mapper.AdMapper;
import com.three.recipingadsservicebe.ad.service.AdCommandService;
import com.three.recipingadsservicebe.ad.service.AdQueryService;
import com.three.recipingadsservicebe.global.security.UserDetailsImpl;
import com.three.recipingadsservicebe.log.dto.LogType;
import com.three.recipingadsservicebe.log.logger.AdLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ads/manage")
@RequiredArgsConstructor
public class AdManagementController {

    private final AdCommandService adCommandService;
    private final AdQueryService adQueryService;
    private final ObjectMapper objectMapper;

    /**
     * 🔧 광고 생성
     */
    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createAd(
            @Valid @RequestBody AdCreateRequest request,
            HttpServletRequest httpRequest) {

        Long currentUserId = getCurrentUserId();
        log.info("광고 생성 요청 - userId: {}, title: {}", currentUserId, request.getTitle());

        try {
            // 1. 광고 생성
            Long adId = adCommandService.createAd(request);

            // 2. 로깅
            logAdAction(LogType.AD_CREATE, httpRequest, currentUserId, adId.toString(),
                    Map.of("title", request.getTitle(),
                            "advertiserId", request.getAdvertiserId(),
                            "adType", request.getAdType().name(),
                            "preferredPosition", request.getPreferredPosition().name()));

            // 3. 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("adId", adId);
            response.put("message", "광고가 성공적으로 생성되었습니다.");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 생성 실패 - userId: {}, title: {}", currentUserId, request.getTitle(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "광고 생성에 실패했습니다: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 🔧 광고 조회 (단건)
     */
    @GetMapping("/{adId}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAd(
            @PathVariable Long adId,
            HttpServletRequest httpRequest) {

        Long currentUserId = getCurrentUserId();
        log.info("광고 조회 요청 - userId: {}, adId: {}", currentUserId, adId);

        try {
            Ad ad = adQueryService.getAdById(adId);
            AdResponse adResponse = AdMapper.toResponse(ad);

            // 로깅
            logAdAction(LogType.VIEW, httpRequest, currentUserId, adId.toString(),
                    Map.of("adTitle", ad.getTitle(), "adStatus", ad.getStatus().name()));

            Map<String, Object> response = new HashMap<>();
            response.put("ad", adResponse);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 조회 실패 - userId: {}, adId: {}", currentUserId, adId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "광고 조회에 실패했습니다: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 🔧 광고 목록 조회 (페이징)
     */
    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAds(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long advertiserId,
            HttpServletRequest httpRequest) {

        Long currentUserId = getCurrentUserId();
        log.info("광고 목록 조회 요청 - userId: {}, page: {}, status: {}",
                currentUserId, pageable.getPageNumber(), status);

        try {
            Page<Ad> adsPage = adQueryService.getAds(pageable, status, advertiserId);
            Page<AdResponse> responseAds = adsPage.map(AdMapper::toResponse);

            // 로깅
            logAdAction(LogType.VIEW, httpRequest, currentUserId, null,
                    Map.of("totalElements", adsPage.getTotalElements(),
                            "pageNumber", pageable.getPageNumber(),
                            "pageSize", pageable.getPageSize(),
                            "status", status != null ? status : "ALL"));

            Map<String, Object> response = new HashMap<>();
            response.put("ads", responseAds.getContent());
            response.put("totalElements", responseAds.getTotalElements());
            response.put("totalPages", responseAds.getTotalPages());
            response.put("currentPage", responseAds.getNumber());
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 목록 조회 실패 - userId: {}", currentUserId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "광고 목록 조회에 실패했습니다: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 🔧 광고 수정
     */
    @PutMapping("/{adId}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateAd(
            @PathVariable Long adId,
            @Valid @RequestBody AdUpdateRequest request,
            HttpServletRequest httpRequest) {

        Long currentUserId = getCurrentUserId();
        log.info("광고 수정 요청 - userId: {}, adId: {}", currentUserId, adId);

        try {
            // 1. 유효성 검증
            if (!request.hasAnyFieldToUpdate()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "수정할 필드가 없습니다.");
                errorResponse.put("success", false);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (!request.isValidDateRange()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "시작일은 종료일보다 이전이어야 합니다.");
                errorResponse.put("success", false);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 2. 광고 수정
            adCommandService.updateAd(adId, request);

            // 3. 로깅
            Map<String, Object> logPayload = new HashMap<>();
            if (request.getTitle() != null) logPayload.put("newTitle", request.getTitle());
            if (request.getAdType() != null) logPayload.put("newAdType", request.getAdType().name());
            if (request.getPreferredPosition() != null) logPayload.put("newPosition", request.getPreferredPosition().name());
            logPayload.put("adId", adId);

            logAdAction(LogType.AD_UPDATE, httpRequest, currentUserId, adId.toString(), logPayload);

            // 4. 응답
            Map<String, Object> response = new HashMap<>();
            response.put("message", "광고가 성공적으로 수정되었습니다.");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 수정 실패 - userId: {}, adId: {}", currentUserId, adId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "광고 수정에 실패했습니다: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 🔧 광고 상태 변경
     */
    @PatchMapping("/{adId}/status")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateAdStatus(
            @PathVariable Long adId,
            @Valid @RequestBody AdStatusUpdateRequest request,
            HttpServletRequest httpRequest) {

        Long currentUserId = getCurrentUserId();
        log.info("광고 상태 변경 요청 - userId: {}, adId: {}, status: {}",
                currentUserId, adId, request.getStatus());

        try {
            adCommandService.updateAdStatus(adId, request);

            // 로깅
            logAdAction(LogType.AD_STATUS_CHANGE, httpRequest, currentUserId, adId.toString(),
                    Map.of("newStatus", request.getStatus().name()));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "광고 상태가 성공적으로 변경되었습니다.");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 상태 변경 실패 - userId: {}, adId: {}", currentUserId, adId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "광고 상태 변경에 실패했습니다: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 🔧 광고 삭제 (소프트 삭제)
     */
    @DeleteMapping("/{adId}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteAd(
            @PathVariable Long adId,
            HttpServletRequest httpRequest) {

        Long currentUserId = getCurrentUserId();
        log.info("광고 삭제 요청 - userId: {}, adId: {}", currentUserId, adId);

        try {
            adCommandService.deleteAd(adId);

            // 로깅
            logAdAction(LogType.AD_DELETE, httpRequest, currentUserId, adId.toString(),
                    Map.of("deleteType", "SOFT_DELETE"));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "광고가 성공적으로 삭제되었습니다.");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 삭제 실패 - userId: {}, adId: {}", currentUserId, adId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "광고 삭제에 실패했습니다: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 광고 성과 조회
     */
    @GetMapping("/{adId}/performance")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdPerformance(
            @PathVariable Long adId,
            HttpServletRequest httpRequest) {

        Long currentUserId = getCurrentUserId();
        log.info("광고 성과 조회 요청 - userId: {}, adId: {}", currentUserId, adId);

        try {
            Ad ad = adQueryService.getAdById(adId);

            // 성과 데이터 구성
            Map<String, Object> performance = new HashMap<>();
            performance.put("impressionCount", ad.getImpressionCount());
            performance.put("clickCount", ad.getClickCount());
            performance.put("ctr", ad.calculateCTR());
            performance.put("spentAmount", ad.getSpentAmount());
            performance.put("budget", ad.getBudget());
            performance.put("budgetUtilization",
                    ad.getBudget() != null && ad.getSpentAmount() != null
                            ? (double) ad.getSpentAmount() / ad.getBudget() * 100
                            : 0.0);

            // 로깅
            logAdAction(LogType.AD_PERFORMANCE, httpRequest, currentUserId, adId.toString(),
                    Map.of("ctr", ad.calculateCTR(),
                            "impressions", ad.getImpressionCount(),
                            "clicks", ad.getClickCount()));

            Map<String, Object> response = new HashMap<>();
            response.put("performance", performance);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 성과 조회 실패 - userId: {}, adId: {}", currentUserId, adId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "광고 성과 조회에 실패했습니다: " + e.getMessage());
            errorResponse.put("success", false);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 현재 사용자 ID 추출
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                return userDetails.getUserId();
            }
        } catch (Exception e) {
            log.debug("사용자 인증 정보 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 광고 액션 로깅 헬퍼 메서드
     */
    private void logAdAction(LogType logType, HttpServletRequest request,
                             Long userId, String targetId, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            AdLogger.track(
                    log,
                    logType,
                    request.getRequestURI(),
                    request.getMethod(),
                    userId != null ? userId.toString() : null,
                    null,
                    targetId,
                    payloadJson,
                    request
            );
        } catch (Exception e) {
            log.warn("광고 액션 로깅 실패 - logType: {}, error: {}", logType, e.getMessage());
        }
    }
}
