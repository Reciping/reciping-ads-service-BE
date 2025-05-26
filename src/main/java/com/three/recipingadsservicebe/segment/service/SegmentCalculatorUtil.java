package com.three.recipingadsservicebe.segment.service;

import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import com.three.recipingadsservicebe.segment.enums.AgeType;
import com.three.recipingadsservicebe.segment.enums.InterestKeywordType;
import com.three.recipingadsservicebe.segment.enums.SegmentType;
import com.three.recipingadsservicebe.segment.enums.SexType;
import org.springframework.stereotype.Component;

@Component
public class SegmentCalculatorUtil {

    public SegmentType calculate(UserInfoDto user) {
        SexType sex = user.getSex();
        AgeType age = user.getAge();
        InterestKeywordType keyword = user.getInterestKeyword();

        // SEG_002: 다이어트 관심 여성 (20-40대)
        if (sex == SexType.FEMALE &&
                (age == AgeType.TWENTIES || age == AgeType.THIRTIES || age == AgeType.FORTIES) &&
                (keyword == InterestKeywordType.DIET || keyword == InterestKeywordType.HEALTHY)) {
            return SegmentType.DIET_FEMALE_ALL;
        }

        // SEG_005: 비건, 환경 중시 MZ 여성
        if (sex == SexType.FEMALE &&
                (age == AgeType.TWENTIES || age == AgeType.THIRTIES) &&
                (keyword == InterestKeywordType.VEGAN || keyword == InterestKeywordType.HEALTHY)) {
            return SegmentType.CONSCIOUS_MZ_FEMALE;
        }

        // SEG_001: 자취 청년
        if ((age == AgeType.TEENS || age == AgeType.TWENTIES) &&
                keyword == InterestKeywordType.SOLO_COOKING) {
            return SegmentType.SOLO_YOUNG;
        }

        // SEG_003: 육아맘 (30~40대, KIDS 관심)
        if (sex == SexType.FEMALE &&
                (age == AgeType.THIRTIES || age == AgeType.FORTIES) &&
                keyword == InterestKeywordType.KIDS) {
            return SegmentType.ACTIVE_MOM;
        }

        // SEG_006: 남성 요리 입문자
        if (sex == SexType.MALE &&
                (keyword == InterestKeywordType.SOLO_COOKING)) {
            return SegmentType.MALE_COOK_STARTER;
        }

        // SEG_004: 고급 요리에 관심 있는 중장년
        if ((age == AgeType.FORTIES || age == AgeType.FIFTIES || age == AgeType.SIXTIES) &&
                keyword == InterestKeywordType.FINE_DINING) {
            return SegmentType.FINE_DINING_MATURE;
        }

        // SEG_007: 10-20대 관심 미설정 or 기타
        if ((age == AgeType.TEENS || age == AgeType.TWENTIES)) {
            return SegmentType.DIGITAL_NATIVE_YOUNG;
        }

        // Fallback: SEG_008
        return SegmentType.PRACTICAL_ADULT;
    }
}
