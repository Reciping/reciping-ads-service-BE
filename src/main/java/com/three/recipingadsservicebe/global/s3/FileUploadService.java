package com.three.recipingadsservicebe.global.s3;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


public interface FileUploadService {
    String upload(MultipartFile auctionImage, String keyName) throws IOException;

    void delete(String keyName);

    String getPresignedURL(String keyName);
}
