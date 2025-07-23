package com.three.recipingadsservicebe.ad.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.mapper.AdMapper;
import com.three.recipingadsservicebe.ad.dto.AdResponse;
import com.three.recipingadsservicebe.ad.service.AdRecommendationService;
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
     * 사용자 맞춤 광고 서빙
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

            // 5. 🔧 개선된 로깅 - 간편한 메서드 사용
            Map<String, Object> servingData = new HashMap<>();
            servingData.put("totalAds", totalAds);
            servingData.put("userId", userId);
            servingData.put("positions", response.keySet());
            servingData.put("servingSuccess", totalAds > 0);

            // 첫 번째 광고에서 A/B 테스트 정보 추출 (있다면)
            response.values().stream()
                    .flatMap(List::stream)
                    .findFirst()
                    .ifPresent(adResponse -> {
                        if (adResponse.getAbTestGroup() != null) {
                            servingData.put("abGroup", adResponse.getAbTestGroup().name());
                        }
                        if (adResponse.getScenarioCode() != null) {
                            servingData.put("scenario", adResponse.getScenarioCode());
                        }
                    });

            AdLogger.logAdServing(log, request, userId != null ? userId.toString() : null, servingData);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("광고 서빙 중 오류 발생 - userId: {}", userId, e);

            // 에러 로깅
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorType", e.getClass().getSimpleName());
            errorData.put("errorMessage", e.getMessage());
            errorData.put("userId", userId);
            errorData.put("servingSuccess", false);

            AdLogger.logAdServing(log, request, userId != null ? userId.toString() : null, errorData);

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

            // 🔧 개선된 클릭 로깅
            Map<String, Object> clickData = new HashMap<>();
            clickData.put("adId", adId);
            clickData.put("position", position);
            clickData.put("userId", userId);
            clickData.put("clickSuccess", true);

            AdLogger.logAdClick(log, request, userId != null ? userId.toString() : null, adId.toString(), clickData);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "클릭 처리 완료");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("광고 클릭 처리 중 오류 - userId: {}, adId: {}", userId, adId, e);

            // 에러 클릭 로깅
            Map<String, Object> errorClickData = new HashMap<>();
            errorClickData.put("adId", adId);
            errorClickData.put("position", position);
            errorClickData.put("userId", userId);
            errorClickData.put("clickSuccess", false);
            errorClickData.put("errorType", e.getClass().getSimpleName());
            errorClickData.put("errorMessage", e.getMessage());

            AdLogger.logAdClick(log, request, userId != null ? userId.toString() : null, adId.toString(), errorClickData);

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
