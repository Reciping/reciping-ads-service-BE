package com.three.recipingadsservicebe.global.security;

import com.three.recipingadsservicebe.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    /**
     * 🔧 개발/테스트 환경: 대부분 허용
     */
    @Bean
    @Profile({"local", "dev", "test"})
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 모든 요청 허용 (개발 편의성)
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * 🔧 운영 환경: 실제 보안 적용
     */
    @Bean
    @Profile({"prod", "staging"})
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(jwtUtil);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger (운영에서는 제한)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").hasRole("ADMIN")

                        // 공개 API들
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/images").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ads/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ads/serve").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/*/click").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/log/**").permitAll()

                        // ADMIN 전용 CRUD
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/ads/**").hasRole("ADMIN")

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
