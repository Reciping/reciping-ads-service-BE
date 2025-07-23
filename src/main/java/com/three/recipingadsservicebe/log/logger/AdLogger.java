package com.three.recipingadsservicebe.log.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.three.recipingadsservicebe.log.dto.LogActorType;
import com.three.recipingadsservicebe.log.dto.LogType;
import com.three.recipingadsservicebe.log.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
public class AdLogger {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 🔧 개선된 광고 도메인 전용 로깅 메서드 (JSON 콘솔 출력)
     */
    public static void track(
            Logger logger,
            LogType logType,
            String path,
            String method,
            String userId,
            String transactionId,
            String targetId,
            String payload,
            HttpServletRequest request
    ) {
        try {
            // 1. TraceId 확인 및 처리
            String traceId = MDC.get("traceId");
            if (traceId == null || traceId.isEmpty()) {
                log.warn("TraceId missing from gateway - request path: {}", path);
                traceId = "MISSING_TRACE_ID";
            }

            LogActorType actorType = resolveActorRole();

            // 2. 구조화된 로그 데이터 생성
            Map<String, Object> logData = createStructuredLogData(
                    traceId, logType, actorType, path, method,
                    userId, transactionId, targetId, payload, request
            );

            // 3. MDC 컨텍스트 설정 (JSON 로깅용)
            setMDCContext(logType, actorType, userId, targetId, logData);

            // 4. 메시지 구성
            String message = buildLogMessage(logType, logData, userId, path, method, traceId);

            // 5. 구조화된 JSON 로깅
            logger.info(message, convertMapToStructuredArgs(logData));

        } catch (Exception e) {
            logger.warn("Structured logging failed for {}: {}, using fallback",
                    logType, e.getMessage());
            logSimpleFormat(logger, logType, path, method, userId, targetId, payload, request);
        } finally {
            cleanupMDC();
        }
    }

    /**
     * 🔧 간편한 광고 서빙 로깅 메서드
     */
    public static void logAdServing(Logger logger, HttpServletRequest request,
                                    String userId, Map<String, Object> servingData) {
        try {
            String payload = objectMapper.writeValueAsString(servingData);
            track(logger, LogType.AD_SERVE, request.getRequestURI(), request.getMethod(),
                    userId, null, null, payload, request);
        } catch (Exception e) {
            logger.warn("Ad serving logging failed: {}", e.getMessage());
        }
    }

    /**
     * 🔧 간편한 광고 클릭 로깅 메서드
     */
    public static void logAdClick(Logger logger, HttpServletRequest request,
                                  String userId, String adId, Map<String, Object> clickData) {
        try {
            String payload = objectMapper.writeValueAsString(clickData);
            track(logger, LogType.AD_CLICK, request.getRequestURI(), request.getMethod(),
                    userId, null, adId, payload, request);
        } catch (Exception e) {
            logger.warn("Ad click logging failed: {}", e.getMessage());
        }
    }

    /**
     * 🔧 간편한 광고 관리 로깅 메서드
     */
    public static void logAdManagement(Logger logger, LogType logType, HttpServletRequest request,
                                       String userId, String adId, Map<String, Object> managementData) {
        try {
            String payload = objectMapper.writeValueAsString(managementData);
            track(logger, logType, request.getRequestURI(), request.getMethod(),
                    userId, null, adId, payload, request);
        } catch (Exception e) {
            logger.warn("Ad management logging failed: {}", e.getMessage());
        }
    }

    /**
     * 로그 메시지 구성
     */
    private static String buildLogMessage(LogType logType, Map<String, Object> logData,
                                          String userId, String path, String method, String traceId) {
        StringBuilder message = new StringBuilder();
        message.append("[").append(logType.name()).append("] ");

        // A/B 테스트 정보 포함
        if (logData.containsKey("abGroup")) {
            message.append("[").append(logData.get("abGroup")).append("] ");
        }

        // 폴백 정보 포함
        if (logData.containsKey("fallbackLevel") &&
                !logData.get("fallbackLevel").toString().equals("0")) {
            message.append("[FALLBACK:L").append(logData.get("fallbackLevel")).append("] ");
        }

        message.append(method).append(" ").append(path)
                .append(" by ").append(userId != null ? "user:" + userId : "GUEST")
                .append(" (trace:").append(traceId).append(")");

        // 주요 비즈니스 정보 추가
        if (logData.containsKey("userSegment")) {
            message.append(" [segment:").append(logData.get("userSegment")).append("]");
        }
        if (logData.containsKey("adPosition")) {
            message.append(" [pos:").append(logData.get("adPosition")).append("]");
        }
        if (logData.containsKey("totalAds")) {
            message.append(" [ads:").append(logData.get("totalAds")).append("]");
        }

        return message.toString();
    }

    /**
     * 구조화된 로그 데이터 생성
     */
    private static Map<String, Object> createStructuredLogData(
            String traceId, LogType logType, LogActorType actorType,
            String path, String method, String userId, String transactionId,
            String targetId, String payload, HttpServletRequest request
    ) {
        Map<String, Object> logData = new HashMap<>();

        // 기본 메타데이터
        logData.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        logData.put("path", path);
        logData.put("method", method);
        logData.put("transactionId", transactionId != null ? transactionId : "-");

        // 클라이언트 정보
        logData.put("clientIp", IpUtil.getClientIp(request));
        logData.put("userAgent", request.getHeader("User-Agent"));
        logData.put("referer", request.getHeader("Referer"));

        // 페이로드 파싱 및 비즈니스 메트릭 추출
        if (payload != null && !payload.equals("-")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
                logData.put("payload", payloadMap);

                // 비즈니스 메트릭 추출
                extractBusinessMetrics(logData, payloadMap, logType);

            } catch (Exception e) {
                logData.put("payload", payload);
                logData.put("payloadParseError", e.getMessage());
            }
        } else {
            logData.put("payload", "{}");
        }

        return logData;
    }

    /**
     * 🔧 개선된 비즈니스 메트릭 추출
     */
    private static void extractBusinessMetrics(Map<String, Object> logData,
                                               Map<String, Object> payload, LogType logType) {
        switch (logType) {
            case AD_SERVE -> {
                // 서빙 관련 메트릭
                extractIfPresent(payload, logData, "totalAds");
                extractIfPresent(payload, logData, "userSegment");
                extractIfPresent(payload, logData, "scenario");
                extractIfPresent(payload, logData, "abGroup");
                extractIfPresent(payload, logData, "positions");

                // 서빙 성공률 계산
                if (payload.containsKey("totalAds")) {
                    int totalAds = Integer.parseInt(payload.get("totalAds").toString());
                    logData.put("servingSuccess", totalAds > 0);
                }
            }

            case AD_CLICK -> {
                // 클릭 관련 메트릭
                extractIfPresent(payload, logData, "adId");
                extractIfPresent(payload, logData, "position");
                extractIfPresent(payload, logData, "userSegment");
                extractIfPresent(payload, logData, "abGroup");
                extractIfPresent(payload, logData, "ctr");

                // CTR 분류
                if (payload.containsKey("ctr")) {
                    try {
                        double ctr = Double.parseDouble(payload.get("ctr").toString());
                        logData.put("ctrCategory", categorizeCTR(ctr));
                    } catch (Exception e) {
                        logData.put("ctr", payload.get("ctr"));
                    }
                }
            }

            case AD_IMPRESSION -> {
                // 노출 관련 메트릭
                extractIfPresent(payload, logData, "adId");
                extractIfPresent(payload, logData, "position");
                extractIfPresent(payload, logData, "billingType");
                extractIfPresent(payload, logData, "targetSegment");
                extractIfPresent(payload, logData, "abGroup");
            }

            case AD_CREATE, AD_UPDATE -> {
                // CRUD 관련 메트릭
                extractIfPresent(payload, logData, "advertiserId");
                extractIfPresent(payload, logData, "adType");
                extractIfPresent(payload, logData, "title");
                extractIfPresent(payload, logData, "preferredPosition");

                if (logType == LogType.AD_UPDATE) {
                    // 수정된 필드들 추적
                    extractIfPresent(payload, logData, "newTitle");
                    extractIfPresent(payload, logData, "newAdType");
                    extractIfPresent(payload, logData, "newPosition");
                }
            }

            case AD_DELETE -> {
                extractIfPresent(payload, logData, "deleteType");
            }

            case AD_STATUS_CHANGE -> {
                extractIfPresent(payload, logData, "newStatus");
            }

            case AD_PERFORMANCE -> {
                extractIfPresent(payload, logData, "ctr");
                extractIfPresent(payload, logData, "impressions");
                extractIfPresent(payload, logData, "clicks");

                // 성과 분류
                if (payload.containsKey("ctr")) {
                    try {
                        double ctr = Double.parseDouble(payload.get("ctr").toString());
                        logData.put("performanceLevel", categorizePerformance(ctr));
                    } catch (Exception e) {
                        // 무시
                    }
                }
            }

            case AD_FALLBACK -> {
                extractIfPresent(payload, logData, "fallbackSteps");
                extractIfPresent(payload, logData, "originalScenario");
                extractIfPresent(payload, logData, "finalScenario");
                extractIfPresent(payload, logData, "fallbackResult");

                if (payload.containsKey("fallbackSteps")) {
                    logData.put("fallbackLevel", extractFallbackLevel(payload));
                }
            }
        }
    }

    /**
     * 헬퍼 메서드들
     */
    private static void extractIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private static String categorizeCTR(double ctr) {
        if (ctr >= 0.03) return "HIGH";
        if (ctr >= 0.015) return "MEDIUM";
        if (ctr >= 0.005) return "LOW";
        return "VERY_LOW";
    }

    private static String categorizePerformance(double ctr) {
        if (ctr >= 0.05) return "EXCELLENT";
        if (ctr >= 0.03) return "GOOD";
        if (ctr >= 0.015) return "AVERAGE";
        if (ctr >= 0.005) return "POOR";
        return "VERY_POOR";
    }

    private static int extractFallbackLevel(Map<String, Object> payload) {
        try {
            Object steps = payload.get("fallbackSteps");
            if (steps instanceof java.util.List) {
                return ((java.util.List<?>) steps).size();
            }
            if (steps instanceof String) {
                return steps.toString().split(",").length;
            }
        } catch (Exception e) {
            // 무시
        }
        return 0;
    }

    /**
     * Map을 StructuredArguments 배열로 변환
     */
    private static Object[] convertMapToStructuredArgs(Map<String, Object> logData) {
        return logData.entrySet().stream()
                .filter(entry -> entry.getValue() != null) // null 값 필터링
                .map(entry -> kv(entry.getKey(), entry.getValue()))
                .toArray();
    }

    /**
     * MDC 컨텍스트 설정 (JSON 필드 포함용)
     */
    private static void setMDCContext(LogType logType, LogActorType actorType,
                                      String userId, String targetId, Map<String, Object> logData) {
        MDC.put("logType", logType.name());
        MDC.put("actorType", actorType.name());
        MDC.put("userId", userId != null ? userId : "GUEST");
        MDC.put("targetId", targetId != null ? targetId : "-");

        // 핵심 비즈니스 차원들을 MDC에 설정 (JSON 로깅에서 최상위 필드로 노출)
        if (logData.containsKey("adPosition")) {
            MDC.put("adPosition", logData.get("adPosition").toString());
        }
        if (logData.containsKey("userSegment")) {
            MDC.put("userSegment", logData.get("userSegment").toString());
        }
        if (logData.containsKey("scenario")) {
            MDC.put("scenario", logData.get("scenario").toString());
        }
        if (logData.containsKey("abGroup")) {
            MDC.put("abGroup", logData.get("abGroup").toString());
        }
        if (logData.containsKey("servingSuccess")) {
            MDC.put("servingSuccess", logData.get("servingSuccess").toString());
        }
    }

    /**
     * 간단한 형태의 로그 (fallback용)
     */
    private static void logSimpleFormat(
            Logger logger, LogType logType, String path, String method,
            String userId, String targetId, String payload, HttpServletRequest request
    ) {
        String traceId = MDC.get("traceId");
        LogActorType actorType = resolveActorRole();

        String simpleLog = String.format("[%s] %s %s by %s (trace:%s) - payload:%s",
                logType.name(),
                method,
                path,
                userId != null ? "user:" + userId : "GUEST",
                traceId != null ? traceId : "MISSING",
                payload != null ? payload : "-"
        );

        logger.info(simpleLog);
    }

    /**
     * 사용자 역할 해석
     */
    private static LogActorType resolveActorRole() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getAuthorities() == null) {
                return LogActorType.GUEST;
            }

            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if (role.contains("ADMIN")) return LogActorType.ADMIN;
                if (role.contains("USER")) return LogActorType.USER;
            }

            return LogActorType.GUEST;
        } catch (Exception e) {
            return LogActorType.GUEST;
        }
    }

    /**
     * MDC 정리
     */
    private static void cleanupMDC() {
        MDC.remove("logType");
        MDC.remove("actorType");
        MDC.remove("userId");
        MDC.remove("targetId");
        MDC.remove("adPosition");
        MDC.remove("userSegment");
        MDC.remove("scenario");
        MDC.remove("abGroup");
        MDC.remove("servingSuccess");
        // traceId는 TraceIdFilter에서 관리하므로 제거하지 않음
    }
}