package com.three.recipingadsservicebe.segment.service;

import com.three.recipingadsservicebe.abtest.entity.AbTestScenario;
import com.three.recipingadsservicebe.abtest.repository.AbTestScenarioRepository;
import com.three.recipingadsservicebe.ad.enums.AbTestGroup;
import com.three.recipingadsservicebe.ad.enums.AdPosition;
import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.AbTestScenarioType;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ABTestManager {

    private final SegmentCalculatorUtil segmentCalculator;

    public AbTestScenarioType assignScenario(UserInfoDto userInfo, AdPosition position) {
        if (userInfo == null) {
            log.debug("사용자 정보 없음 - 기본 시나리오 사용");
            return AbTestScenarioType.getDefaultScenario();
        }

        try {
            SegmentType segment = segmentCalculator.calculate(userInfo);

            // 활성화된 세그먼트만 처리
            if (!segment.isActive()) {
                log.debug("비활성 세그먼트 [{}] - 기본 시나리오로 fallback", segment.name());
                return AbTestScenarioType.getDefaultScenario();
            }

            AbTestGroup group = calculateGroup(userInfo.getUserId(), segment.name());

            log.debug("A/B 테스트 할당: 사용자={}, 세그먼트={}, 그룹={}, 위치={}",
                    userInfo.getUserId(), segment.name(), group.name(), position.name());

            return AbTestScenarioType.findBySegmentAndGroup(segment, group)
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("활성 A/B 시나리오 없음: segment=%s, group=%s", segment, group)));

        } catch (Exception e) {
            log.warn("A/B 테스트 시나리오 할당 실패 - 기본 시나리오 사용: {}", e.getMessage());
            return AbTestScenarioType.getDefaultScenario();
        }
    }

    private AbTestGroup calculateGroup(Long userId, String segmentName) {
        int hash = Math.abs(Objects.hash(userId, segmentName));
        int mod = hash % 2;

        AbTestGroup result = (mod == 0) ? AbTestGroup.A : AbTestGroup.B;
        log.trace("A/B 그룹 계산: userId={}, segment={}, hash={}, group={}",
                userId, segmentName, hash, result);

        return result;
    }
}