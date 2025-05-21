package com.three.recipingadsservicebe.log.logger;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.log.dto.AdLogType;
import com.three.recipingadsservicebe.log.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j(topic = "AdLogger")
public class AdLogger {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static void log(
            AdLogType logType,
            String url,
            String method,
            Long userId,
            Long adId,
            AdPosition position,
            AbTestGroup abGroup,
            HttpServletRequest request,
            String payload
    ) {
        try {
            String ip = IpUtil.getClientIp(request);
            String userAgent = safeTruncate(getOrDefault(request.getHeader("User-Agent")), 300);
            String referer = getOrDefault(request.getHeader("Referer"));
            String timestamp = ZonedDateTime.now().format(ISO_FORMATTER);

            log.info(String.join("\t",
                    logType.name(),
                    timestamp,
                    url,
                    method,
                    userId != null ? userId.toString() : "-",
                    adId != null ? adId.toString() : "-",
                    position != null ? position.name() : "-",
                    abGroup != null ? abGroup.name() : "-",
                    ip,
                    userAgent,
                    referer,
                    payload != null ? payload : "-"
            ));
        } catch (Exception e) {
            log.warn("광고 로그 기록 실패: {}", e.getMessage());
        }
    }


    private static String getOrDefault(String value) {
        return value != null ? value : "-";
    }

    private static String safeTruncate(String input, int maxLength) {
        if (input == null) return "-";
        return input.length() > maxLength ? input.substring(0, maxLength) : input;
    }
}
