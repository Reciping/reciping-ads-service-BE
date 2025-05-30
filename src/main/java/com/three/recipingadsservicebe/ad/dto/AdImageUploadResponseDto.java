package com.three.recipingadsservicebe.ad.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdImageUploadResponseDto {
    private String objectName;  // 원본 파일명
    private String keyName;     // S3에 저장된 key (UUID 포함)
    private String filePath;    // S3 URL
}
