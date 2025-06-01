package com.three.recipingadsservicebe.log.util;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TraceIdFilter implements Filter {
    private final TraceIdUtil traceIdUtil;

    private static final String TRACE_KEY = "traceId"; // MDC (로그 프레임워크 내부 컨텍스트)에 저장할 key 이름 - logback-spring.xml에서 %X{traceId}로 사용

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String traceId = traceIdUtil.resolveTraceId(httpRequest);

        try {
            MDC.put(TRACE_KEY, traceId);  // 요청 스레드에 traceId 주입
            chain.doFilter(request, response); // 다음 필터 or 컨트롤러 실행
        } finally {
            MDC.remove(TRACE_KEY); // 요청 끝나면 정리 (메모리 누수 방지)
        }
    }
}
