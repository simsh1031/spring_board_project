package com.example.boardpjt.config;

import com.example.boardpjt.filter.JwtFilter;
import com.example.boardpjt.filter.RefreshJwtFilter;
import com.example.boardpjt.model.repository.RefreshTokenRepository;
import com.example.boardpjt.service.CustomUserDetailsService;
import com.example.boardpjt.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스 (Refresh Token 지원 버전)
 * JWT 기반 인증 시스템을 구성하며, Access Token과 Refresh Token을 모두 지원
 * 세션을 사용하지 않는 Stateless 방식으로 동작하면서도 토큰 갱신 기능 제공
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 보안 필터 체인 설정 (Refresh Token 지원)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // === 기본 보안 설정 비활성화 ===
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // === URL별 접근 권한 설정 ===
        http.authorizeHttpRequests(auth -> auth
                        // 홈페이지("/")와 인증 관련 경로("/auth/**")는 모든 사용자 접근 허용
                        .requestMatchers("/", "/auth/**").permitAll()

                        // ★ API 경로 - 인증된 사용자만 접근 가능
                        .requestMatchers("/api/**").authenticated()

                        // "/my-page" 경로는 인증된 사용자만 접근 가능
                        .requestMatchers("/my-page").authenticated()

                        // "/admin/**" 경로는 ADMIN 역할을 가진 사용자만 접근 가능
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // === 예외 처리 설정 (★ API와 일반 페이지 분리) ===
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            // ★ API 요청인 경우 JSON 응답 반환
                            if (req.getRequestURI().startsWith("/api/")) {
                                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                res.setCharacterEncoding("UTF-8");
                                res.getWriter().write("{\"error\": \"인증이 필요합니다\", \"message\": \"로그인 후 이용해주세요\"}");
                            } else {
                                // 일반 페이지 요청인 경우 로그인 페이지로 리다이렉트
                                res.sendRedirect("/auth/login");
                            }
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            // ★ API 요청인 경우 JSON 응답 반환
                            if (req.getRequestURI().startsWith("/api/")) {
                                res.setStatus(HttpStatus.FORBIDDEN.value());
                                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                res.setCharacterEncoding("UTF-8");
                                res.getWriter().write("{\"error\": \"접근 권한이 없습니다\"}");
                            } else {
                                res.sendRedirect("/auth/login");
                            }
                        })
                );

        // === JWT 필터 체인 구성 ===
        http
                .addFilterBefore(new JwtFilter(jwtUtil, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RefreshJwtFilter(jwtUtil, userDetailsService, refreshTokenRepository),
                        JwtFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 인코더 빈 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 인증 매니저 빈 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}