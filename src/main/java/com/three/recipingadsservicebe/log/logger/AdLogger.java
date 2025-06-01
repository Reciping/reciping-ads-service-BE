package com.three.recipingadsservicebe.log.logger;

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


@Slf4j(topic = "AdLogger")
public class AdLogger {
    /**
     * 광고 도메인 전용 로깅 메서드
     * 팀 표준 형식을 따르되, 광고 특화 정보는 payload에 JSON 형태로 저장
     */
    public static void track(
            Logger logger,
            LogType logType,
            String path,
            String method,
            String userId,
            String transactionId,
            String targetId,      // adId를 targetId로 매핑
            String payload,       // 광고 특화 정보 (JSON)
            HttpServletRequest request
    ) {
        LogActorType actorType = resolveActorRole();
        String traceId = MDC.get("traceId"); // traceId 추가

        logger.info(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                traceId != null ? traceId : "-",
                logType.name(),
                actorType.name(),
                LocalDateTime.now(),
                path,
                method,
                userId != null ? userId : "-",
                transactionId != null ? transactionId : "-",
                targetId != null ? targetId : "-",
                payload != null ? payload : "-",
                IpUtil.getClientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        ));
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
}
