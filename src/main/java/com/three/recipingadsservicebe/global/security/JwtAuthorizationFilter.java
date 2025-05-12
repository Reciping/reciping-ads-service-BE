package com.three.recipingadsservicebe.global.security;

import com.three.recipingadsservicebe.global.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = jwtUtil.getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                Claims claims = jwtUtil.getUserInfoFromToken(token);

                String email = claims.getSubject();
                String role = claims.get("role", String.class);

                if (email != null && role != null) {

                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    Collections.singleton(new SimpleGrantedAuthority(authority))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (Exception e) {
            log.warn("JWT 인증 실패: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
