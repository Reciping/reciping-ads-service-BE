package com.three.recipingadsservicebe.global.security;

import com.three.recipingadsservicebe.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(jwtUtil);

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ✅ 사용자 광고 조회는 공개
                        .requestMatchers(HttpMethod.GET, "/api/v1/ads/public/**").permitAll()

                        // ✅ 광고 관리 API는 ADMIN 권한 필요
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/ads/**").hasRole("ADMIN")

                        // ✅ 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
