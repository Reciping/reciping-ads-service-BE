package com.three.recipingadsservicebe.ad.controller;

import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.log.dto.AdLogType;
import com.three.recipingadsservicebe.log.logger.AdLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ads/log")
public class AdLogController {

    @PostMapping("/impression")
    public String logImpression(@RequestParam Long adId,
                                @RequestParam(required = false) Long userId,
                                @RequestParam AdPosition position,
                                @RequestParam(required = false) AbTestGroup abGroup,
                                HttpServletRequest request) {
        AdLogger.log(
                AdLogType.IMPRESSION,
                "/api/v1/ads/log/impression",
                "POST",
                userId,
                adId,
                position,
                abGroup,  // null 허용
                request,
                null
        );
        return "노출 로그 기록됨";
    }

    @PostMapping("/click")
    public String logClick(@RequestParam Long adId,
                           @RequestParam(required = false) Long userId,
                           @RequestParam AdPosition position,
                           @RequestParam(required = false) AbTestGroup abGroup,
                           HttpServletRequest request) {
        AdLogger.log(
                AdLogType.CLICK,
                "/api/v1/ads/log/click",
                "POST",
                userId,
                adId,
                position,
                abGroup,  // null 허용
                request,
                null
        );
        return "클릭 로그 기록됨";
    }
}
