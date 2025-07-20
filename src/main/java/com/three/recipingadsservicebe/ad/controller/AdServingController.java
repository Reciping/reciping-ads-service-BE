package com.three.recipingadsservicebe.ad.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.mapper.AdMapper;
import com.three.recipingadsservicebe.ad.dto.AdResponse;
import com.three.recipingadsservicebe.ad.service.AdRecommendationService;
import com.three.recipingadsservicebe.log.dto.LogType;
import com.three.recipingadsservicebe.log.logger.AdLogger;
import com.three.recipingadsservicebe.global.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
public class AdServingController {

    private final AdRecommendationService adRecommendationService;
    private final ObjectMapper objectMapper;

    /**
     * 🎯 핵심 API: 사용자 맞춤 광고 서빙
     */
    @GetMapping("/serve")
    public ResponseEntity<Map<String, Object>> serveAds(HttpServletRequest request) {
        Long userId = getCurrentUserId();

        log.info("광고 서빙 요청 - userId: {}", userId);

        try {
            // 1. 광고 추천
            Map<String, List<Ad>> adsByPosition = adRecommendationService.recommendAdsForUser(userId);

            // 2. 응답 변환
            Map<String, List<AdResponse>> response = new HashMap<>();
            for (Map.Entry<String, List<Ad>> entry : adsByPosition.entrySet()) {
                List<AdResponse> adResponses = entry.getValue().stream()
                        .map(AdMapper::toResponse)
                        .toList();
                response.put(entry.getKey(), adResponses);
            }

            // 3. 총 광고 수 계산
            int totalAds = response.values().stream().mapToInt(List::size).sum();

            // 4. 최종 응답 구성
            Map<String, Object> result = new HashMap<>();
            result.put("ads", response);
            result.put("totalCount", totalAds);
            result.put("message", "광고 서빙 완료");

            // 5. 로깅
            try {
                Map<String, Object> logPayload = new HashMap<>();
                logPayload.put("totalAds", totalAds);
                logPayload.put("userId", userId);
                logPayload.put("positions", response.keySet());

                AdLogger.track(
                        log,
                        LogType.AD_SERVE,
                        request.getRequestURI(),
                        request.getMethod(),
                        userId != null ? userId.toString() : null,
                        null,
                        null,
                        objectMapper.writeValueAsString(logPayload),
                        request
                );
            } catch (Exception e) {
                log.warn("광고 서빙 로깅 실패: {}", e.getMessage());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("광고 서빙 중 오류 발생 - userId: {}", userId, e);

            // 에러 응답
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("ads", new HashMap<>());
            errorResponse.put("totalCount", 0);
            errorResponse.put("message", "광고 서빙 실패");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 광고 클릭 처리
     */
    @PostMapping("/{adId}/click")
    public ResponseEntity<Map<String, Object>> handleAdClick(
            @PathVariable Long adId,
            @RequestParam String position,
            HttpServletRequest request) {

        Long userId = getCurrentUserId();

        log.info("광고 클릭 처리 - userId: {}, adId: {}, position: {}", userId, adId, position);

        try {
            AdPosition adPosition = AdPosition.valueOf(position);
            adRecommendationService.handleAdClick(userId, adId, adPosition);

            // 클릭 로깅
            try {
                Map<String, Object> logPayload = new HashMap<>();
                logPayload.put("adId", adId);
                logPayload.put("position", position);
                logPayload.put("userId", userId);

                AdLogger.track(
                        log,
                        LogType.AD_CLICK,
                        request.getRequestURI(),
                        request.getMethod(),
                        userId != null ? userId.toString() : null,
                        null,
                        adId.toString(),
                        objectMapper.writeValueAsString(logPayload),
                        request
                );
            } catch (Exception e) {
                log.warn("광고 클릭 로깅 실패: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "클릭 처리 완료");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 클릭 처리 중 오류 - userId: {}, adId: {}", userId, adId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "클릭 처리 실패");
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.ok(errorResponse);
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
        return null; // 비로그인 사용자
    }
}
