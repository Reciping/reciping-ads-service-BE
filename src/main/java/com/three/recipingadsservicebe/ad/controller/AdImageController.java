package com.three.recipingadsservicebe.ad.controller;

import com.three.recipingadsservicebe.ad.dto.AdImageUploadResponseDto;
import com.three.recipingadsservicebe.ad.service.AdImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ads/images")
public class AdImageController {

    private final AdImageUploadService imageUploadService;

    @PostMapping
    public ResponseEntity<AdImageUploadResponseDto> uploadAdImage(@RequestPart("image") MultipartFile image) throws IOException {
        return ResponseEntity.ok(imageUploadService.upload(image));
    }
}
