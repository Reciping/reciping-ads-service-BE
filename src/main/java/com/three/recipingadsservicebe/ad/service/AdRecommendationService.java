package com.three.recipingadsservicebe.ad.service;
import com.three.recipingadsservicebe.abtest.enums.AbTestScenario;
import com.three.recipingadsservicebe.abtest.service.AbTestService;
import com.three.recipingadsservicebe.ad.entity.Ad;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.ad.enums.AdStatus;
import com.three.recipingadsservicebe.ad.repository.AdRepository;
import com.three.recipingadsservicebe.targeting.dto.UserProfileDto;
import com.three.recipingadsservicebe.targeting.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdRecommendationService {
    private final AdRepository adRepository;
    private final UserProfileService userProfileService;
    private final AbTestService abTestService;

    /**
     * 🎯 핵심 메서드: 안정성이 강화된 사용자 맞춤 광고 추천
     */
    @Transactional
    public Map<String, List<Ad>> recommendAdsForUser(Long userId) {
        log.info("광고 추천 시작 - userId: {}", userId);

        Map<String, List<Ad>> result = new HashMap<>();

        try {
            // 1. 사용자 프로필 조회 (캐싱 + Fallback 적용)
            UserProfileDto userProfile = userProfileService.getUserProfile(userId);

            // 2. A/B 테스트 그룹 결정
            AbTestScenario scenario = null;
            if (userId != null) {
                scenario = abTestService.assignUserToGroup(userId);
            }

            // 3. 각 포지션별 광고 추천 (안전한 처리)
            for (AdPosition position : AdPosition.values()) {
                try {
                    List<Ad> recommendedAds = recommendAdsForPosition(userProfile, scenario, position);

                    // 4. 노출 이벤트 기록 (에러가 발생해도 광고 서빙은 계속)
                    recordImpressionsSafely(userId, scenario, recommendedAds, position);

                    result.put(position.name(), recommendedAds);

                } catch (Exception e) {
                    log.error("포지션별 광고 추천 실패 - userId: {}, position: {}", userId, position, e);
                    // 실패해도 빈 리스트로 계속 진행
                    result.put(position.name(), List.of());
                }
            }

            int totalAds = result.values().stream().mapToInt(List::size).sum();
            log.info("광고 추천 완료 - userId: {}, 총 광고 수: {}", userId, totalAds);

            return result;

        } catch (Exception e) {
            log.error("전체 광고 추천 프로세스 실패 - userId: {}", userId, e);

            // 최후 Fallback: 모든 포지션에 빈 리스트
            Map<String, List<Ad>> fallbackResult = new HashMap<>();
            for (AdPosition position : AdPosition.values()) {
                fallbackResult.put(position.name(), List.of());
            }
            return fallbackResult;
        }
    }

    /**
     * 🔧 안전한 노출 이벤트 기록 (광고 서빙에 영향 없도록)
     */
    private void recordImpressionsSafely(Long userId, AbTestScenario scenario,
                                         List<Ad> ads, AdPosition position) {
        if (userId == null || scenario == null || ads.isEmpty()) {
            return;
        }

        try {
            for (Ad ad : ads) {
                abTestService.recordImpression(userId, scenario.getScenarioCode(),
                        scenario.getGroup(), ad.getId(), position);
                ad.increaseImpression();
            }
        } catch (Exception e) {
            log.warn("노출 이벤트 기록 실패 (광고 서빙은 계속) - userId: {}, position: {}",
                    userId, position, e);
            // 에러가 발생해도 광고 서빙은 계속 진행
        }
    }

    /**
     * 포지션별 광고 추천 로직
     */
    private List<Ad> recommendAdsForPosition(UserProfileDto userProfile,
                                             AbTestScenario scenario, AdPosition position) {

        // 비로그인 사용자 또는 Control 그룹: 랜덤 서빙
        if (userProfile == null || scenario == null || scenario.getGroup().name().equals("CONTROL")) {
            return selectRandomAds(position);
        }

        // Treatment 그룹: 행동태그 기반 타겟팅
        return selectTargetedAds(userProfile, scenario, position);
    }

    /**
     * 행동태그 기반 타겟팅 (Treatment 그룹)
     */
    private List<Ad> selectTargetedAds(UserProfileDto userProfile,
                                       AbTestScenario scenario, AdPosition position) {

        log.debug("행동태그 기반 타겟팅 시작 - userId: {}, position: {}",
                userProfile.getUserId(), position);

        // 1순위: 완전 매치 광고
        List<Ad> perfectMatch = adRepository.findByBehaviorTargeting(
                position,
                scenario.getScenarioCode(),
                userProfile.getDemographicSegment(),
                userProfile.getEngagementLevel(),
                userProfile.getCookingStylePreference()
        );

        if (!perfectMatch.isEmpty()) {
            List<Ad> selected = selectBestAds(perfectMatch, 3);
            log.debug("완전 매치 광고 선택 - userId: {}, 광고 수: {}",
                    userProfile.getUserId(), selected.size());
            return selected;
        }

        // 2순위: 부분 매치 광고
        List<Ad> partialMatch = adRepository.findByPartialBehaviorTargeting(
                position,
                scenario.getScenarioCode(),
                userProfile.getDemographicSegment(),
                userProfile.getEngagementLevel(),
                userProfile.getCookingStylePreference()
        );

        if (!partialMatch.isEmpty()) {
            List<Ad> selected = selectBestAds(partialMatch, 3);
            log.debug("부분 매치 광고 선택 - userId: {}, 광고 수: {}",
                    userProfile.getUserId(), selected.size());
            return selected;
        }

        // 3순위: Fallback - 랜덤 광고
        log.warn("타겟팅 매치 실패, Fallback 수행 - userId: {}", userProfile.getUserId());
        return selectRandomAds(position);
    }

    /**
     * 랜덤 광고 서빙 (Control 그룹 또는 비로그인)
     */
    private List<Ad> selectRandomAds(AdPosition position) {
        log.debug("랜덤 광고 선택 - position: {}", position);

        // Control 그룹용 광고 조회
        List<Ad> randomAds = adRepository.findByScenarioCodeAndPosition("CONTROL", position);

        if (randomAds.isEmpty()) {
            // Control 광고가 없으면 모든 활성 광고에서 선택
            randomAds = adRepository.findAll().stream()
                    .filter(ad -> ad.getPreferredPosition() == position)
                    .filter(this::isAdEligible)
                    .collect(Collectors.toList());
        }

        return selectBestAds(randomAds, 3);
    }

    /**
     * 최적 광고 선택 (점수 기반)
     */
    private List<Ad> selectBestAds(List<Ad> candidates, int limit) {
        return candidates.stream()
                .filter(this::isAdEligible)
                .sorted((a1, a2) -> {
                    // 1순위: 광고 점수
                    int scoreCompare = Float.compare(
                            a2.getScore() != null ? a2.getScore() : 0f,
                            a1.getScore() != null ? a1.getScore() : 0f
                    );
                    if (scoreCompare != 0) return scoreCompare;

                    // 2순위: CTR
                    int ctrCompare = Float.compare(a2.calculateCTR(), a1.calculateCTR());
                    if (ctrCompare != 0) return ctrCompare;

                    // 3순위: 최신순
                    return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 광고 송출 가능 여부 검증
     */
    private boolean isAdEligible(Ad ad) {
        // 상태 확인
        if (ad.getStatus() != AdStatus.ACTIVE) return false;

        // 예산 확인
        if (ad.getBudget() != null && ad.getSpentAmount() != null) {
            if (ad.getSpentAmount() >= ad.getBudget()) return false;
        }

        // 기간 확인
        LocalDateTime now = LocalDateTime.now();
        if (ad.getStartAt() != null && now.isBefore(ad.getStartAt())) return false;
        if (ad.getEndAt() != null && now.isAfter(ad.getEndAt())) return false;

        return true;
    }

    /**
     * 클릭 이벤트 처리
     */
    @Transactional
    public void handleAdClick(Long userId, Long adId, AdPosition position) {
        log.info("광고 클릭 처리 - userId: {}, adId: {}", userId, adId);

        // 광고 클릭 수 증가
        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("광고를 찾을 수 없습니다: " + adId));
        ad.increaseClick();

        // A/B 테스트 이벤트 기록
        if (userId != null) {
            AbTestScenario scenario = abTestService.assignUserToGroup(userId);
            abTestService.recordClick(userId, scenario.getScenarioCode(),
                    scenario.getGroup(), adId, position);
        }
    }
}
