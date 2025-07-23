package com.three.recipingadsservicebe.targeting.service;
import com.three.recipingadsservicebe.feign.UserFeignClient;
import com.three.recipingadsservicebe.targeting.dto.UserInfoDto;
import com.three.recipingadsservicebe.targeting.dto.UserProfileDto;
import com.three.recipingadsservicebe.targeting.enums.CookingStylePreference;
import com.three.recipingadsservicebe.targeting.enums.DemographicSegment;
import com.three.recipingadsservicebe.targeting.enums.EngagementLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserFeignClient userFeignClient;

    /**
     * 🎯 최적화된 사용자 프로필 조회 (캐싱 + Fallback)
     */
    @Cacheable(value = "userProfiles", key = "#userId", unless = "#result == null")
    public UserProfileDto getUserProfile(Long userId) {
        if (userId == null) {
            return createGuestProfile();
        }

        try {
            log.debug("사용자 프로필 조회 시작 - userId: {}", userId);

            // 🔧 Feign Client로 User 서비스 호출
            UserInfoDto userInfo = userFeignClient.getUserInfo(userId);

            if (userInfo == null) {
                log.warn("사용자 정보 조회 실패 - userId: {}", userId);
                return createDefaultProfile(userId);
            }

            // 🔧 효율적인 변환 로직
            UserProfileDto profile = convertToUserProfile(userInfo);

            log.debug("사용자 프로필 조회 완료 - userId: {}, segment: {}, cooking: {}",
                    userId, profile.getDemographicSegment(), profile.getCookingStylePreference());

            return profile;

        } catch (Exception e) {
            log.error("사용자 프로필 조회 중 오류 - userId: {}", userId, e);
            return createDefaultProfile(userId);
        }
    }

    /**
     * 🔧 효율적인 UserInfoDto → UserProfileDto 변환
     */
    private UserProfileDto convertToUserProfile(UserInfoDto userInfo) {
        // Case 1: User 도메인에서 이미 행동태그를 계산해서 제공하는 경우
        if (userInfo.getDemographicSegment() != null &&
                userInfo.getEngagementLevel() != null &&
                userInfo.getCookingStylePreference() != null) {

            log.debug("User 도메인에서 행동태그 제공 - userId: {}", userInfo.getUserId());

            return UserProfileDto.builder()
                    .userId(userInfo.getUserId())
                    .demographicSegment(userInfo.getDemographicSegment())
                    .engagementLevel(userInfo.getEngagementLevel())
                    .cookingStylePreference(userInfo.getCookingStylePreference())
                    .segmentCalculatedAt(userInfo.getSegmentCalculatedAt())
                    .behaviorCalculatedAt(userInfo.getBehaviorCalculatedAt())
                    .build();
        }

        // Case 2: User 도메인에서 기본 정보만 제공하는 경우 (현재 상황)
        else {
            log.debug("기본 정보로부터 행동태그 추론 - userId: {}", userInfo.getUserId());

            return UserProfileDto.builder()
                    .userId(userInfo.getUserId())
                    .demographicSegment(calculateDemographicSegment(userInfo.getSex(), userInfo.getAge()))
                    .engagementLevel(estimateEngagementLevel(userInfo.getUserId()))
                    .cookingStylePreference(estimateCookingStyle(userInfo.getUserId()))
                    .build();
        }
    }

    /**
     * 🔧 인구통계학적 세그먼트 계산 (기존 정보 활용)
     */
    private DemographicSegment calculateDemographicSegment(UserInfoDto.SexType sex, UserInfoDto.AgeType age) {
        if (sex == null || age == null) {
            return DemographicSegment.UNKNOWN;
        }

        switch (sex) {
            case FEMALE:
                switch (age) {
                    case TWENTIES: return DemographicSegment.FEMALE_TWENTIES;
                    case THIRTIES: return DemographicSegment.FEMALE_THIRTIES;
                    case FORTIES: return DemographicSegment.FEMALE_FORTIES;
                    case FIFTIES:
                    case SIXTIES_PLUS: return DemographicSegment.FEMALE_FIFTIES_PLUS;
                }
                break;
            case MALE:
                switch (age) {
                    case TWENTIES: return DemographicSegment.MALE_TWENTIES;
                    case THIRTIES: return DemographicSegment.MALE_THIRTIES;
                    case FORTIES: return DemographicSegment.MALE_FORTIES;
                    case FIFTIES:
                    case SIXTIES_PLUS: return DemographicSegment.MALE_FIFTIES_PLUS;
                }
                break;
        }

        return DemographicSegment.UNKNOWN;
    }

    /**
     * 🔧 참여도 레벨 추정 (임시 로직 - 추후 실제 데이터로 대체)
     */
    private EngagementLevel estimateEngagementLevel(Long userId) {
        // 임시: 사용자 ID 기반 분산 (실제로는 로그인 횟수, 활동 패턴 등으로 계산)
        int group = (int) (userId % 4);

        switch (group) {
            case 0: return EngagementLevel.HIGH_ACTIVE;
            case 1: return EngagementLevel.REGULAR_USER;
            case 2: return EngagementLevel.CASUAL_USER;
            default: return EngagementLevel.DORMANT_USER;
        }
    }

    /**
     * 🔧 요리 스타일 추정 (임시 로직 - 추후 실제 행동 데이터로 대체)
     */
    private CookingStylePreference estimateCookingStyle(Long userId) {
        // 임시: 사용자 ID 기반 분산 (실제로는 레시피 조회 패턴, 검색 키워드 등으로 계산)
        int styleGroup = (int) (userId % 5);

        switch (styleGroup) {
            case 0: return CookingStylePreference.HEALTH_CONSCIOUS;
            case 1: return CookingStylePreference.CONVENIENCE_SEEKER;
            case 2: return CookingStylePreference.GOURMET_EXPLORER;
            case 3: return CookingStylePreference.FAMILY_ORIENTED;
            default: return CookingStylePreference.DIVERSE_EXPLORER;
        }
    }

    /**
     * 비로그인 사용자 프로필 생성
     */
    private UserProfileDto createGuestProfile() {
        return UserProfileDto.builder()
                .userId(null)
                .demographicSegment(DemographicSegment.UNKNOWN)
                .engagementLevel(EngagementLevel.CASUAL_USER)
                .cookingStylePreference(CookingStylePreference.DIVERSE_EXPLORER)
                .build();
    }

    /**
     * 기본 프로필 생성 (조회 실패 시 Fallback)
     */
    private UserProfileDto createDefaultProfile(Long userId) {
        log.info("기본 프로필 생성 - userId: {}", userId);

        return UserProfileDto.builder()
                .userId(userId)
                .demographicSegment(DemographicSegment.UNKNOWN)
                .engagementLevel(EngagementLevel.CASUAL_USER)
                .cookingStylePreference(CookingStylePreference.DIVERSE_EXPLORER)
                .build();
    }
}
