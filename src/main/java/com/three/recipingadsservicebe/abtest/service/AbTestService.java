package com.three.recipingadsservicebe.abtest.service;
import com.three.recipingadsservicebe.abtest.entity.AbTestEvent;
import com.three.recipingadsservicebe.abtest.enums.AbTestScenario;
import com.three.recipingadsservicebe.abtest.repository.AbTestEventRepository;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbTestService {
    private final AbTestEventRepository abTestEventRepository;

    /**
     * 사용자 A/B 테스트 그룹 결정
     */
    public AbTestScenario assignUserToGroup(Long userId) {
        AbTestScenario scenario = AbTestScenario.assignForUser(userId);
        log.debug("A/B 테스트 그룹 할당 - userId: {}, scenario: {}", userId, scenario.getScenarioCode());
        return scenario;
    }

    /**
     * 광고 노출 이벤트 기록
     */
    @Transactional
    public void recordImpression(Long userId, String scenarioCode, AbTestGroup group,
                                 Long adId, AdPosition position) {
        try {
            AbTestEvent impression = AbTestEvent.createImpression(
                    userId, scenarioCode, group, adId, position.name());

            abTestEventRepository.save(impression);

            log.debug("노출 이벤트 기록 완료 - userId: {}, adId: {}, group: {}",
                    userId, adId, group);
        } catch (Exception e) {
            log.error("노출 이벤트 기록 실패 - userId: {}, adId: {}", userId, adId, e);
        }
    }

    /**
     * 광고 클릭 이벤트 기록
     */
    @Transactional
    public void recordClick(Long userId, String scenarioCode, AbTestGroup group,
                            Long adId, AdPosition position) {
        try {
            AbTestEvent click = AbTestEvent.createClick(
                    userId, scenarioCode, group, adId, position.name());

            abTestEventRepository.save(click);

            log.info("클릭 이벤트 기록 완료 - userId: {}, adId: {}, group: {}",
                    userId, adId, group);
        } catch (Exception e) {
            log.error("클릭 이벤트 기록 실패 - userId: {}, adId: {}", userId, adId, e);
        }
    }
}
