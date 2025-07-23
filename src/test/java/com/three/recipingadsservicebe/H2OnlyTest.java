package com.three.recipingadsservicebe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 🎯 H2 데이터베이스만 테스트 (모든 외부 의존성 제외)
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("H2 데이터베이스 전용 테스트")
class H2OnlyTest {

    @Test
    @DisplayName("✅ H2 데이터베이스만 로딩")
    void h2DatabaseOnly() {
        // @DataJpaTest는 JPA 관련 Bean만 로딩
        // Feign Client, AWS, Security 등 모든 문제 요소 제외
    }
}