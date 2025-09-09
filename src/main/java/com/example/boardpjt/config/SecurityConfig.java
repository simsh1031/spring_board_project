package com.example.boardpjt.config;

import com.example.boardpjt.filter.JwtFilter;
import com.example.boardpjt.filter.RefreshJwtFilter;
import com.example.boardpjt.model.repository.RefreshTokenRepository;
import com.example.boardpjt.service.CustomUserDetailsService;
import com.example.boardpjt.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@Configuration  // Spring의 설정 클래스임을 나타냄
@EnableWebSecurity  // Spring Security 웹 보안 활성화
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성 (Lombok)
public class SecurityConfig {

    // JWT 토큰 관련 유틸리티 클래스 (토큰 생성, 검증, 파싱 등)
    private final JwtUtil jwtUtil;

    // 사용자 정보를 로드하는 커스텀 서비스 (인증 시 사용자 상세정보 제공)
    private final CustomUserDetailsService userDetailsService;

    // Refresh Token을 데이터베이스에서 관리하기 위한 Repository
    // Access Token과 달리 Refresh Token은 서버에서 상태를 관리해야 함
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 보안 필터 체인 설정 (Refresh Token 지원)
     * HTTP 요청에 대한 보안 규칙을 정의하고 JWT 필터들을 순서대로 추가
     *
     * @param http HttpSecurity 객체 - Spring Security의 HTTP 보안 설정을 위한 빌더
     * @return SecurityFilterChain - 구성된 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // === 기본 보안 설정 비활성화 ===
        http.csrf(AbstractHttpConfigurer::disable)  // CSRF 보호 비활성화 (REST API에서는 불필요)
                .formLogin(AbstractHttpConfigurer::disable)  // 기본 폼 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)  // HTTP Basic 인증 비활성화
                // 세션 관리 정책을 STATELESS로 설정 (JWT 사용 시 세션 불필요)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // === URL별 접근 권한 설정 (개선된 버전) ===
        http.authorizeHttpRequests(auth -> auth
                        // 홈페이지("/")와 인증 관련 경로("/auth/**")는 모든 사용자 접근 허용
                        .requestMatchers("/", "/auth/**").permitAll()
                        // auth/** 패턴 사용으로 다음 경로들이 모두 포함됨:
                        // - /auth/register (회원가입)
                        // - /auth/login (로그인)
                        // - /auth/logout (로그아웃)
                        // - /auth/refresh (토큰 갱신) - 새로 추가된 기능

                        // "/my-page" 경로는 인증된 사용자만 접근 가능
                        .requestMatchers("/my-page").authenticated()

                        // === 역할 기반 접근 제어 (RBAC) ===
                        // "/admin/**" 경로는 ADMIN 역할을 가진 사용자만 접근 가능
                        // hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN" 권한을 확인
                        // 주의: UserAccount의 role 필드는 "ROLE_ADMIN" 형태로 저장되어야 함
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 추가 역할 기반 경로 예시:
                        // .requestMatchers("/moderator/**").hasRole("MODERATOR")
                        // .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // === 예외 처리 설정 ===
                .exceptionHandling(e ->
                                // 인증되지 않은 사용자가 보호된 리소스에 접근할 때 로그인 페이지로 리다이렉트
                                e.authenticationEntryPoint((req, res, ex) ->
                                        res.sendRedirect("/auth/login"))

                        // 추가 예외 처리 옵션:
                        // .accessDeniedHandler((req, res, ex) ->
                        //     res.sendRedirect("/access-denied")) // 권한 부족 시 처리
                );

        // === JWT 필터 체인 구성 (중요: 순서가 매우 중요함) ===
        http
                // 2단계: JwtFilter를 UsernamePasswordAuthenticationFilter 앞에 배치
                // - 일반적인 Access Token 검증 담당
                // - 모든 HTTP 요청에 대해 JWT 토큰 유효성 검증
                .addFilterBefore(new JwtFilter(jwtUtil, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                // 1단계: RefreshJwtFilter를 가장 먼저 배치
                // - Refresh Token 갱신 요청을 우선 처리
                // - /auth/refresh 경로에 대한 특별한 처리 담당
                .addFilterBefore(new RefreshJwtFilter(jwtUtil, userDetailsService, refreshTokenRepository),
                        JwtFilter.class);

        // === 필터 체인 실행 순서 ===
        // 1. RefreshJwtFilter: Refresh Token 갱신 처리
        // 2. JwtFilter: Access Token 검증
        // 3. UsernamePasswordAuthenticationFilter: 기본 인증 (사용 안 함)
        // 4. 기타 Spring Security 필터들...

        // 설정이 완료된 SecurityFilterChain 반환
        return http.build();
    }

    /**
     * 비밀번호 인코더 빈 등록
     * Spring Security에서 비밀번호 암호화에 사용
     *
     * @return PasswordEncoder - 위임형 패스워드 인코더 (기본적으로 BCrypt 사용)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // DelegatingPasswordEncoder 생성 - 여러 인코딩 방식을 지원하며 기본으로 BCrypt 사용
        // {bcrypt}, {noop}, {pbkdf2} 등 다양한 인코딩 방식을 자동으로 감지하고 처리
        // 패스워드 마이그레이션이나 다양한 인코딩 방식 지원에 유리
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * 인증 매니저 빈 등록
     * Spring Security에서 인증 처리를 담당하는 핵심 컴포넌트
     *
     * @param configuration AuthenticationConfiguration - Spring Security 인증 설정
     * @return AuthenticationManager - 구성된 인증 매니저
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // AuthenticationConfiguration에서 기본 AuthenticationManager를 가져와서 반환
        // 이는 사용자 인증 (로그인) 처리 시 사용됨
        // RefreshJwtFilter와 AuthController.login()에서 모두 활용
        return configuration.getAuthenticationManager();
    }
}

// === 주요 업데이트 사항 분석 ===

/**
 * 1. Refresh Token 지원 추가
 * - RefreshTokenRepository 의존성 추가
 * - RefreshJwtFilter를 필터 체인에 추가
 * - 토큰 갱신 기능으로 보안성과 사용자 경험 개선
 *
 * 2. 역할 기반 접근 제어 (RBAC) 강화
 * - .hasRole("ADMIN")으로 관리자 전용 경로 보호
 * - 세분화된 권한 관리 체계 구축
 *
 * 3. 필터 순서 최적화
 * - RefreshJwtFilter → JwtFilter 순서로 배치
 * - 토큰 갱신 요청을 우선 처리하는 구조
 *
 * 4. 확장 가능한 구조
 * - 다양한 역할 추가 가능 (MODERATOR, SUPER_ADMIN 등)
 * - 추가 예외 처리 핸들러 설정 가능
 */

// === Refresh Token 워크플로우 ===

/**
 * 일반적인 요청 흐름:
 * 1. 클라이언트가 Access Token으로 API 요청
 * 2. JwtFilter에서 Access Token 검증
 * 3. 유효한 경우: 요청 처리 진행
 * 4. 만료된 경우: 401 Unauthorized 응답
 *
 * 토큰 갱신 요청 흐름:
 * 1. 클라이언트가 /auth/refresh로 Refresh Token 전송
 * 2. RefreshJwtFilter에서 Refresh Token 검증
 * 3. 유효한 경우: 새로운 Access Token 발급
 * 4. 새 토큰을 쿠키에 설정하여 응답
 */

// === 보안 고려사항 ===

/**
 * 1. Refresh Token 보안:
 * - 데이터베이스에 저장하여 서버에서 상태 관리
 * - 토큰 회전(Rotation) 정책 적용 권장
 * - 사용 후 즉시 무효화 처리
 *
 * 2. 역할 기반 보안:
 * - 최소 권한 원칙 적용
 * - 역할별 세분화된 접근 제어
 * - 권한 상승 공격 방지
 *
 * 3. 필터 체인 보안:
 * - 필터 순서가 보안에 미치는 영향 고려
 * - 각 필터의 책임 명확히 분리
 * - 예외 상황에 대한 안전한 처리
 */

// === 성능 최적화 고려사항 ===

/**
 * 1. 토큰 검증 최적화:
 * - Refresh Token 검증은 /auth/refresh 경로에서만 수행
 * - 일반 요청에서는 Access Token만 검증하여 성능 향상
 *
 * 2. 데이터베이스 접근 최소화:
 * - Access Token은 stateless 유지
 * - Refresh Token만 DB 조회 수행
 *
 * 3. 캐싱 전략:
 * - 자주 조회되는 사용자 정보 캐싱
 * - Refresh Token 검증 결과 단기 캐싱
 */