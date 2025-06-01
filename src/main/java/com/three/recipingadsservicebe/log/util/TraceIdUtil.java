package com.three.recipingadsservicebe.log.util;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TraceIdUtil {

    private static final String TRACE_HEADER = "X-TRACE-ID";

    // MSA 서비스 진입점에서 호출하는 로직
    public String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_HEADER);
        return (traceId != null && !traceId.isEmpty()) ? traceId : generateNewTraceId(); // 게이트웨이 서버에서 안 줬으면 generateNewTraceId() 호출
    }

    // gateway에서 X-TRACE-ID가 없을 때만 호출
    private String generateNewTraceId() {
        return UUID.randomUUID().toString();
    }
}
