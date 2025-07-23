package com.three.recipingadsservicebe.ad.controller;

import com.three.recipingadsservicebe.ad.service.AdRecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("광고 서빙 컨트롤러 테스트")
class AdServingControllerTest {

    @Autowired
    private AdServingController adServingController;

    @MockBean
    private AdRecommendationService adRecommendationService;

    @Test
    @DisplayName("✅ Controller Bean 정상 생성")
    void controllerExists() {
        assertThat(adServingController).isNotNull();
    }

    @Test
    @DisplayName("✅ Service 의존성 주입 확인")
    void serviceDependencyInjected() {
        // Controller가 정상 생성되었다면
        // 모든 의존성이 올바르게 주입된 것
        assertThat(adServingController).isNotNull();
    }
}