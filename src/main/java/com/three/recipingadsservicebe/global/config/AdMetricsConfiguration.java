package com.three.recipingadsservicebe.global.config;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AdMetricsConfiguration {

    /**
     * 애플리케이션별 메트릭 태그 설정
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "service", "ads-service",
                "phase", "phase1",
                "version", getClass().getPackage().getImplementationVersion() != null
                        ? getClass().getPackage().getImplementationVersion()
                        : "dev"
        );
    }

    /**
     * ✅ 중요: Counter Bean 대신 MeterRegistry만 주입받기
     * 태그가 있는 메트릭은 동적으로 생성해야 함
     */
    @Bean
    public Timer adServeTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ads_serve_duration_seconds")
                .description("Time taken to serve ads to users")
                .register(meterRegistry);
    }

    /**
     * 활성 광고 수 게이지
     */
    @Bean
    public AtomicInteger activeAdsGauge(MeterRegistry meterRegistry) {
        AtomicInteger activeAdsCount = new AtomicInteger(0);

        Gauge.builder("ads_active_count", activeAdsCount, AtomicInteger::get)
                .description("Number of currently active ads")
                .register(meterRegistry);


        return activeAdsCount;
    }
}