package com.three.recipingadsservicebe.segment.dto;

import com.three.recipingadsservicebe.segment.enums.AgeType;
import com.three.recipingadsservicebe.segment.enums.InterestKeywordType;
import com.three.recipingadsservicebe.segment.enums.SexType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfoDto {
    Long userId;
    SexType sex;
    AgeType age;
    InterestKeywordType interestKeyword;
}
