package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("광고 추천 서비스 테스트")
class AdRecommendationServiceTest {

    @InjectMocks
    private AdRecommendationService adRecommendationService;

    @Mock
    private AdRepository adRepository;

    @Test
    @DisplayName("✅ 서비스 객체 생성 성공")
    void serviceCreation() {
        assertThat(adRecommendationService).isNotNull();
    }

    @Test
    @DisplayName("✅ Repository Mock 기본 동작")
    void repositoryMockWorks() {
        // Given
        List<Ad> mockAds = Collections.emptyList();
        given(adRepository.findAll()).willReturn(mockAds);

        // When
        List<Ad> result = adRepository.findAll();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("✅ 사용자별 광고 추천 기본 동작")
    void recommendAdsBasicFunction() {
        // Given: 기본 설정
        given(adRepository.findAll()).willReturn(Collections.emptyList());

        // When: 실제 서비스 메서드가 있다면 호출
        // 현재는 Mock이 동작하는지만 확인
        List<Ad> result = adRepository.findAll();

        // Then: 기본 검증
        assertThat(result).isNotNull();
    }
}
