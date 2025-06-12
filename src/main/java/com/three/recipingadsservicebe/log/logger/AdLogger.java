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
     * 광고 도메인 전용 로깅 메서드 - 최종 완성 버전
     *
     * 🔧 최종 개선사항:
     * 1. 중복 필드 완전 제거 (MDC vs StructuredArgs 분리)
     * 2. Phase 1 메트릭 완전 포함
     * 3. 폴백 정보 상세 추적
     * 4. 성능 최적화 유지
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
            // 1. TraceId 확인
            String traceId = MDC.get("traceId");
            if (traceId == null || traceId.isEmpty()) {
                log.warn("TraceId missing from gateway - request path: {}", path);
                traceId = "MISSING_TRACE_ID";
            }

            LogActorType actorType = resolveActorRole();

            // 2. 구조화된 로그 데이터 생성 (중복 제거 적용)
            Map<String, Object> logData = createStructuredLogData(
                    traceId, logType, actorType, path, method,
                    userId, transactionId, targetId, payload, request
            );

            // 3. MDC에 핵심 라벨만 설정 (Loki 필터링용)
            setEnhancedMDCContext(logType, actorType, userId, targetId, logData);

            // 4. 개선된 메시지 포맷
            String detailedMessage = buildDetailedMessage(logType, logData, userId, path, method, traceId);

            // 5. StructuredArguments로 효율적 로깅
            logger.info(detailedMessage, convertMapToStructuredArgs(logData));

        } catch (Exception e) {
            logger.warn("Structured logging failed for {}: {}, fallback to simple format",
                    logType, e.getMessage());
            logSimpleFormat(logger, logType, path, method, userId, targetId, payload, request);
        } finally {
            cleanupMDC();
        }
    }

    /**
     * 🔧 개선: 폴백 정보가 포함된 상세한 로그 메시지 생성
     */
    private static String buildDetailedMessage(LogType logType, Map<String, Object> logData,
                                               String userId, String path, String method, String traceId) {
        StringBuilder message = new StringBuilder();
        message.append("Ad ").append(logType.name().toLowerCase());

        // 폴백 정보 포함
        if (logData.containsKey("originalScenario") && logData.containsKey("scenario")) {
            String original = logData.get("originalScenario").toString();
            String current = logData.get("scenario").toString();
            if (!original.equals(current)) {
                message.append(" [FALLBACK: ").append(original).append("→").append(current).append("]");
            }
        }

        message.append(" tracked - ").append(method).append(" ").append(path)
                .append(" by user ").append(userId != null ? userId : "GUEST")
                .append(" (trace: ").append(traceId).append(")");

        // 세그먼트 정보 추가
        if (logData.containsKey("userSegment")) {
            message.append(" [segment: ").append(logData.get("userSegment")).append("]");
        }

        return message.toString();
    }

    /**
     * Map을 StructuredArguments 배열로 변환 (성능 최적화 유지)
     */
    private static Object[] convertMapToStructuredArgs(Map<String, Object> logData) {
        return logData.entrySet().stream()
                .map(entry -> kv(entry.getKey(), entry.getValue()))
                .toArray();
    }

    /**
     * 🔧 최종 수정: Phase 1 도메인에 특화된 로그 데이터 생성 (중복 필드 제거)
     */
    private static Map<String, Object> createStructuredLogData(
            String traceId, LogType logType, LogActorType actorType,
            String path, String method, String userId, String transactionId,
            String targetId, String payload, HttpServletRequest request
    ) {
        Map<String, Object> logData = new HashMap<>();

        // 기본 메타데이터 (MDC와 중복되지 않는 것만)
        logData.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        // ❌ 제거: traceId, logType, actorType, userId, targetId (MDC에서 처리)

        // 요청 정보
        logData.put("path", path);
        logData.put("method", method);
        logData.put("transactionId", transactionId != null ? transactionId : "-");

        // 클라이언트 정보
        logData.put("clientIp", IpUtil.getClientIp(request));
        logData.put("userAgent", request.getHeader("User-Agent"));
        logData.put("referer", request.getHeader("Referer"));

        // 페이로드 파싱 및 Phase 1 특화 메트릭 추출
        if (payload != null && !payload.equals("-")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
                logData.put("payload", payloadMap);

                // Phase 1 핵심 비즈니스 메트릭 추출 (완전 보존)
                extractPhase1Metrics(logData, payloadMap, logType);

            } catch (Exception e) {
                logData.put("payload", payload);
                logData.put("payloadParseError", e.getMessage());
            }
        } else {
            logData.put("payload", "{}");
        }

        // ❌ 제거: environment (Logback에서 처리)

        return logData;
    }

    /**
     * Phase 1에 최적화된 비즈니스 메트릭 추출 (완전 보존)
     */
    private static void extractPhase1Metrics(Map<String, Object> logData, Map<String, Object> payload, LogType logType) {
        switch (logType) {
            case AD_IMPRESSION -> {
                // 노출 관련 핵심 차원들
                extractCommonAdMetrics(logData, payload);

                // 노출 특화 메트릭
                if (payload.containsKey("billingType")) {
                    logData.put("billingType", payload.get("billingType"));
                }
            }

            case AD_CLICK -> {
                // 클릭 관련 핵심 차원들
                extractCommonAdMetrics(logData, payload);

                // 클릭 특화 메트릭
                if (payload.containsKey("ctr")) {
                    try {
                        double ctr = Double.parseDouble(payload.get("ctr").toString());
                        logData.put("ctr", ctr);
                        logData.put("ctrBucket", categorizeCTR(ctr));
                    } catch (Exception e) {
                        logData.put("ctr", payload.get("ctr"));
                    }
                }
            }

            case AD_SERVE -> {
                // 서빙 관련 메트릭
                if (payload.containsKey("userSegment")) {
                    String segment = payload.get("userSegment").toString();
                    logData.put("userSegment", segment);
                    logData.put("isActiveSegment", isActivePhase1Segment(segment));
                }

                if (payload.containsKey("totalAds")) {
                    logData.put("totalAds", payload.get("totalAds"));
                }
            }

            case AD_FALLBACK -> {
                // 폴백 분석을 위한 상세 메트릭
                if (payload.containsKey("fallbackSteps")) {
                    logData.put("fallbackLevel", extractFallbackLevel(payload));
                    logData.put("fallbackSteps", payload.get("fallbackSteps"));
                }

                if (payload.containsKey("originalScenario")) {
                    logData.put("originalScenario", payload.get("originalScenario"));
                }

                if (payload.containsKey("finalScenario")) {
                    logData.put("finalScenario", payload.get("finalScenario"));
                }

                if (payload.containsKey("result")) {
                    logData.put("fallbackResult", payload.get("result"));
                    logData.put("fallbackSuccess", "SUCCESS".equals(payload.get("result")));
                }
            }

            case AD_CREATE, AD_UPDATE -> {
                // 관리 기능 메트릭
                if (payload.containsKey("advertiserId")) {
                    logData.put("advertiserId", payload.get("advertiserId"));
                }

                if (payload.containsKey("adType")) {
                    logData.put("adType", payload.get("adType"));
                }
            }
        }
    }

    /**
     * 광고 공통 메트릭 추출 (완전 보존)
     */
    private static void extractCommonAdMetrics(Map<String, Object> logData, Map<String, Object> payload) {
        // Phase 1 핵심 차원들
        if (payload.containsKey("position")) {
            String position = payload.get("position").toString();
            logData.put("adPosition", position);
            logData.put("isActivePosition", isActivePhase1Position(position));
        }

        if (payload.containsKey("scenario")) {
            String scenario = payload.get("scenario").toString();
            logData.put("scenario", scenario);
            logData.put("isActiveScenario", isActivePhase1Scenario(scenario));
        }

        if (payload.containsKey("targetSegment")) {
            String segment = payload.get("targetSegment").toString();
            logData.put("targetSegment", segment);
            logData.put("isActiveSegment", isActivePhase1Segment(segment));
        }

        if (payload.containsKey("abGroup")) {
            logData.put("abGroup", payload.get("abGroup"));
        }

        if (payload.containsKey("advertiserId")) {
            logData.put("advertiserId", payload.get("advertiserId"));
        }
    }

    /**
     * 🔧 개선: 향상된 MDC 컨텍스트 설정 (Loki 라벨링용만)
     */
    private static void setEnhancedMDCContext(LogType logType, LogActorType actorType,
                                              String userId, String targetId, Map<String, Object> logData) {
        MDC.put("logType", logType.name());
        MDC.put("actorType", actorType.name());
        MDC.put("userId", userId != null ? userId : "GUEST");
        MDC.put("targetId", targetId != null ? targetId : "-");

        // Phase 1 핵심 차원들을 MDC에도 설정 (Loki 쿼리 최적화)
        if (logData.containsKey("adPosition")) {
            MDC.put("adPosition", logData.get("adPosition").toString());
        }
        if (logData.containsKey("userSegment")) {
            MDC.put("userSegment", logData.get("userSegment").toString());
        }
        if (logData.containsKey("scenario")) {
            MDC.put("scenario", logData.get("scenario").toString());
        }

        // 폴백 정보도 MDC에 포함 (알림용)
        if (logData.containsKey("fallbackSuccess")) {
            MDC.put("fallbackSuccess", logData.get("fallbackSuccess").toString());
        }
    }

    /**
     * Phase 1 활성 검증 헬퍼 메서드들 (완전 보존)
     */
    private static boolean isActivePhase1Position(String position) {
        return "MAIN_TOP".equals(position) || "MAIN_MIDDLE".equals(position);
    }

    private static boolean isActivePhase1Segment(String segment) {
        return segment != null && (
                segment.contains("DIET_FEMALE_ALL") ||
                        segment.contains("MALE_COOK_STARTER") ||
                        segment.contains("ACTIVE_MOM") ||
                        segment.contains("GENERAL_ALL")
        );
    }

    private static boolean isActivePhase1Scenario(String scenario) {
        return scenario != null && (
                scenario.startsWith("SC_DIET_") ||
                        scenario.startsWith("SC_COOK_") ||
                        scenario.startsWith("SC_MOM_") ||
                        scenario.equals("SC_DEFAULT_GENERAL")
        );
    }

    private static String categorizeCTR(double ctr) {
        if (ctr >= 0.03) return "HIGH";
        if (ctr >= 0.015) return "MEDIUM";
        return "LOW";
    }

    private static int extractFallbackLevel(Map<String, Object> payload) {
        try {
            Object steps = payload.get("fallbackSteps");
            if (steps instanceof java.util.List) {
                return ((java.util.List<?>) steps).size();
            }
        } catch (Exception e) {
            // 무시
        }
        return 0;
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

        String simpleLog = String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                traceId != null ? traceId : "MISSING_TRACE_ID",
                logType.name(),
                actorType.name(),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                path,
                method,
                userId != null ? userId : "GUEST",
                targetId != null ? targetId : "-",
                payload != null ? payload : "-",
                IpUtil.getClientIp(request),
                request.getHeader("User-Agent")
        );

        logger.info(simpleLog);
    }

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

    private static void cleanupMDC() {
        MDC.remove("logType");
        MDC.remove("actorType");
        MDC.remove("userId");
        MDC.remove("targetId");
        MDC.remove("adPosition");
        MDC.remove("userSegment");
        MDC.remove("scenario");
        MDC.remove("fallbackSuccess");
        // traceId는 TraceIdFilter에서 관리
    }
}