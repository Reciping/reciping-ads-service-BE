package com.three.recipingadsservicebe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.openfeign.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:testdb"
        }
)
@ActiveProfiles("test")
class RecipingAdsServiceBeApplicationTests {

    @Test
    void contextLoads() {
        // Spring Context가 정상적으로 로드되는지 확인
    }
}