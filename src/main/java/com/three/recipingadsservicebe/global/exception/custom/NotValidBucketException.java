package com.three.recipingadsservicebe.global.exception.custom;

public class NotValidBucketException extends RuntimeException {
    public NotValidBucketException(String message) {
        super(message);
    }
}
