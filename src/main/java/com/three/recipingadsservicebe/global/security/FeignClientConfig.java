package com.three.recipingadsservicebe.global.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignClientConfig implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            String token = userDetails.getToken(); // ⭐️ JwtAuthorizationFilter에서 토큰을 넣어줘야 함
            if (token != null) {
                template.header("Authorization", "Bearer " + token);
            }
        }
    }
}
