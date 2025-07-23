package com.three.recipingadsservicebe.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트 환경 전용 Configuration
 * 외부 의존성을 Mock으로 대체하여 테스트 안정성 확보
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * AWS S3 관련 Bean들을 Mock으로 대체
     * 실제 AWS 연결 없이 테스트 가능
     */
    @Bean
    @Primary
    public String testAwsConfig() {
        return "test-aws-config";
    }

    /**
     * Feign Client 관련 Bean들을 Mock으로 대체
     * 외부 API 호출 없이 테스트 가능
     */
    @Bean
    @Primary
    public String testFeignConfig() {
        return "test-feign-config";
    }
}
