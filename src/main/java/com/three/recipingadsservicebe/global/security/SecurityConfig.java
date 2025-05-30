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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(jwtUtil);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()

                        // ✅ 이미지 업로드 비회원 임시 허용
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/images").permitAll()

                        // ✅ 사용자 광고 조회 API는 인증 없이 허용
                        .requestMatchers(HttpMethod.GET, "/api/v1/ads/public/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/ads/test/**").permitAll()

                        // ✅ 광고 노출/클릭 로그 API → 비회원 허용
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/log/**").permitAll()

                        // ✅ 광고 CRUD는 ADMIN만 허용
                        .requestMatchers(HttpMethod.POST, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ads/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/ads/**").hasRole("ADMIN")

                        // ✅ 그 외 요청은 인증만 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ CORS 설정: 명시적 Origin 허용
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
//        config.setAllowCredentials(true);
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//        config.addExposedHeader("Authorization");
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }

}
