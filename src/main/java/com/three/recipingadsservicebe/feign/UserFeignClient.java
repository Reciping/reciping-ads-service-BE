package com.three.recipingadsservicebe.feign;

import com.three.recipingadsservicebe.segment.dto.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${user-service.url}"
)
public interface UserFeignClient {

    @GetMapping("/api/v1/internal/users/{userId}/info")
    UserInfoDto getUserInfo(@PathVariable("userId") Long userId);
}
