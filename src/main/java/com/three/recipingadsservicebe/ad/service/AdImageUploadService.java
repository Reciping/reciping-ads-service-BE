package com.three.recipingadsservicebe.ad.service;

import com.three.recipingadsservicebe.ad.dto.AdImageUploadResponseDto;
import com.three.recipingadsservicebe.global.s3.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdImageUploadService {
    private final FileUploadService fileUploadService;

    public AdImageUploadResponseDto upload(MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("이미지가 필요합니다.");
        }

        String key = generateKey(image.getOriginalFilename());
        String url = fileUploadService.upload(image, key);

        return new AdImageUploadResponseDto(
                image.getOriginalFilename(),
                key,
                url
        );
    }

    private String generateKey(String filename) {
        return UUID.randomUUID() + "-" + filename.replaceAll("\\s", "");
    }
}
