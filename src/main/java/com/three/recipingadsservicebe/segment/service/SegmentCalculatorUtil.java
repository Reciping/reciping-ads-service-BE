package com.three.recipingadsservicebe.segment.service;

import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.AgeType;
import com.three.recipingadsservicebe.segment.enums.InterestKeywordType;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import com.three.recipingadsservicebe.segment.enums.SexType;
import org.springframework.stereotype.Component;

@Component
public class SegmentCalculatorUtil {

    private static final boolean PHASE_2_ENABLED = false;

    public SegmentType calculate(UserInfoDto user) {
        if (user == null) {
            return SegmentType.GENERAL_ALL; // 범용 세그먼트 반환
        }

        SexType sex = user.getSex();
        AgeType age = user.getAge();
        InterestKeywordType keyword = user.getInterestKeyword();

        // Phase 1: 핵심 3개 세그먼트
        SegmentType phase1Result = calculatePhase1Segments(sex, age, keyword);
        if (phase1Result != null) {
            return phase1Result;
        }

        // Phase 2: 확장 세그먼트 (조건부)
        if (PHASE_2_ENABLED) {
            SegmentType phase2Result = calculatePhase2Segments(sex, age, keyword);
            if (phase2Result != null) {
                return phase2Result;
            }
        }

        // ✅ 범용 세그먼트로 Fallback
        return SegmentType.GENERAL_ALL;
    }

    private SegmentType calculatePhase1Segments(SexType sex, AgeType age, InterestKeywordType keyword) {
        // 기존 로직 동일
        // 다이어트 관심 여성
        if (sex == SexType.FEMALE &&
                (age == AgeType.TWENTIES || age == AgeType.THIRTIES || age == AgeType.FORTIES) &&
                (keyword == InterestKeywordType.DIET || keyword == InterestKeywordType.HEALTHY)) {
            return SegmentType.DIET_FEMALE_ALL;
        }

        // 활동적인 엄마
        if (sex == SexType.FEMALE &&
                (age == AgeType.THIRTIES || age == AgeType.FORTIES) &&
                keyword == InterestKeywordType.KIDS) {
            return SegmentType.ACTIVE_MOM;
        }

        // 남성 요리 입문자
        if (sex == SexType.MALE &&
                keyword == InterestKeywordType.SOLO_COOKING) {
            return SegmentType.MALE_COOK_STARTER;
        }

        return null;
    }

    private SegmentType calculatePhase2Segments(SexType sex, AgeType age, InterestKeywordType keyword) {
        // Phase 2 로직 (현재는 비활성)
        return null;
    }
}