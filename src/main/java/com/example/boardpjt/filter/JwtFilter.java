package com.example.boardpjt.filter;

import com.example.boardpjt.util.CookieUtil;
import com.example.boardpjt.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Access Token 기반 인증을 처리하는 Spring Security 필터 (개선된 버전)
 * HTTP 요청마다 한 번씩 실행되어 쿠키에서 Access Token을 추출하고 유효성을 검증
 * CookieUtil을 활용하여 코드 가독성과 유지보수성을 향상시킨 버전
 * RefreshJwtFilter와 함께 동작하는 이중 토큰 시스템의 일부
 */
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입용)
public class JwtFilter extends OncePerRequestFilter {
    // 참고: SecurityConfig에서 이 필터를 생성하고 필터 체인에 추가함
    // 필터 순서: RefreshJwtFilter → JwtFilter → 기타 필터들

    // JWT 토큰 생성, 검증, 파싱을 담당하는 유틸리티 클래스
    private final JwtUtil jwtUtil;

    // 사용자 세부 정보를 로드하는 서비스 (DB에서 사용자 정보 조회)
    private final UserDetailsService userDetailsService;
    // 주의: 이 클래스는 Spring Bean이 아니므로 SecurityConfig에서 수동으로 의존성 주입

    /**
     * HTTP 요청마다 실행되는 필터 메인 로직 (CookieUtil 활용 버전)
     * 쿠키에서 Access Token을 추출하고, 유효한 경우 Spring Security Context에 인증 정보 설정
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param filterChain 다음 필터로 요청을 전달하기 위한 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException 입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // === 디버깅 로그 ===
        // 필터 실행 확인용 로그 (운영환경에서는 제거 권장)
        System.out.println("[Jwt Filter] - Access Token 검증 시작");

        // === 1단계: 쿠키에서 Access Token 추출 (개선된 방식) ===
        // CookieUtil.findCookie(): 쿠키 검색 로직을 유틸리티로 분리
        // 장점: 코드 재사용성, 가독성 향상, 일관된 쿠키 처리
        String token = CookieUtil.findCookie(request, "access_token");

        // === 기존 방식 (주석 처리됨) ===
        // 직접적인 쿠키 순회 방식에서 유틸리티 사용으로 개선
        /*
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (c.getName().equals("access_token")) {
                    token = c.getValue();
                    break;
                }
            }
        }
        */

        // 디버깅용 토큰 값 출력 (운영환경에서는 보안상 제거 필요)
        System.out.println("Access Token = " + token);

        // === 2단계: 토큰 부재 시 처리 ===
        if (token == null) {
            // === Access Token이 없는 경우의 시나리오들 ===
            // 1. 비로그인 사용자의 공개 페이지 접근
            // 2. 로그인했지만 Access Token이 만료되어 쿠키에서 삭제된 경우
            // 3. 쿠키가 손상되거나 조작된 경우
            // 4. 다른 도메인에서의 요청 (CORS 이슈)

            System.out.println("[JWT Filter] Access Token 없음 - 인증 없이 진행");

            // 인증 없이 다음 필터로 요청 전달
            // SecurityConfig의 설정에 따라 접근 제한이 적용됨:
            // - 공개 경로("/", "/auth/**"): 접근 허용
            // - 보호된 경로("/my-page", "/posts/**"): 401 Unauthorized 또는 로그인 페이지 리다이렉트
            filterChain.doFilter(request, response);
            return; // 메서드 종료 - 이후 코드 실행 안 함
        }

        // === 추가 토큰 검증 옵션 (주석 처리됨) ===
        // 토큰 존재 여부와 유효성을 동시에 검사하는 방식도 가능
        // if (token == null || !jwtUtil.validateToken(token)) {
        //     filterChain.doFilter(request, response);
        //     return;
        // }
        // 하지만 현재는 토큰 파싱 과정에서 유효성 검증을 함께 수행

        // === 3단계: JWT 토큰 검증 및 인증 정보 설정 ===
        try {
            System.out.println("[JWT Filter] Access Token 검증 및 인증 정보 설정 시작");

            // === 토큰에서 사용자명 추출 ===
            // jwtUtil.getUsername(): 토큰 파싱 + 유효성 검증을 동시 수행
            // 이 과정에서 다음 검증들이 이루어짐:
            // - 토큰 형식 검증 (Header.Payload.Signature)
            // - 서명 검증 (SecretKey로 무결성 확인)
            // - 만료시간 검증 (exp 클레임 확인)
            // - 발급자, 대상자 등 기타 클레임 검증
            String username = jwtUtil.getUsername(token);

            System.out.println("[JWT Filter] 토큰에서 추출된 사용자명: " + username);

            // === 사용자 세부 정보 로드 ===
            // CustomUserDetailsService를 통해 데이터베이스에서 사용자 정보 조회
            // 이 과정에서 사용자 존재 여부, 계정 상태, 권한 정보 등을 확인
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            System.out.println("[JWT Filter] 사용자 정보 로드 완료: " + userDetails.getUsername());
            System.out.println("[JWT Filter] 사용자 권한: " + userDetails.getAuthorities());

            // === Spring Security 인증 객체 생성 ===
            // UsernamePasswordAuthenticationToken을 사용하여 인증된 상태의 Authentication 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,                    // Principal: 인증된 사용자의 상세 정보
                    null,                          // Credentials: JWT 방식에서는 비밀번호 불필요
                    userDetails.getAuthorities()   // Authorities: 사용자의 역할/권한 목록 (ROLE_USER, ROLE_ADMIN 등)
            );

            // === Spring Security Context에 인증 정보 저장 ===
            // SecurityContextHolder는 Thread-Local 방식으로 현재 스레드의 보안 컨텍스트 관리
            // 이후 모든 Spring Security 컴포넌트에서 현재 사용자 정보에 접근 가능:
            // - @PreAuthorize, @Secured 등 어노테이션에서 권한 검사
            // - 컨트롤러의 Authentication 파라미터로 주입
            // - SecurityContextHolder.getContext().getAuthentication()으로 직접 조회
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("[JWT Filter] 인증 정보 설정 완료 - SecurityContext에 저장됨");

        } catch (Exception e) {
            // === 토큰 검증 실패 처리 ===
            // 다양한 예외 상황들을 포괄적으로 처리

            System.err.println("[JWT Filter] 토큰 검증 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());

            // === 가능한 예외 시나리오들 ===
            // 1. ExpiredJwtException: Access Token 만료
            //    → RefreshJwtFilter에서 토큰 갱신 처리 예정
            // 2. MalformedJwtException: 잘못된 토큰 형식
            //    → 토큰 조작 시도 또는 클라이언트 오류
            // 3. SignatureException: 서명 검증 실패
            //    → 토큰 위변조 시도
            // 4. UsernameNotFoundException: 사용자 없음
            //    → 토큰은 유효하지만 해당 사용자가 삭제된 경우
            // 5. 기타 보안 관련 예외들

            // === 예외 처리 정책 ===
            // 모든 예외에 대해 인증 실패로 처리하되, 요청 흐름은 계속 진행
            // SecurityConfig의 설정에 따라 접근 제한이 적용됨
            // 예외 세부 사항은 로그로만 기록 (보안상 클라이언트에 노출 안 함)

            // SecurityContext를 명시적으로 정리 (선택사항)
            // SecurityContextHolder.clearContext();
        }

        // === 4단계: 다음 필터로 요청 전달 ===
        // 인증 성공/실패와 관계없이 필터 체인의 다음 단계로 진행
        // Spring Security의 다른 필터들이 추가 처리 수행:
        // - AuthorizationFilter: URL별 접근 권한 검사
        // - ExceptionTranslationFilter: 인증/인가 예외 처리
        // - FilterSecurityInterceptor: 최종 보안 검사

        System.out.println("[JWT Filter] 필터 처리 완료 - 다음 필터로 요청 전달");
        filterChain.doFilter(request, response);
    }
}

// === 주요 개선사항 분석 ===

/**
 * 1. CookieUtil 도입:
 * - 기존: 직접적인 쿠키 순회 및 검색 로직
 * - 개선: CookieUtil.findCookie()로 코드 간소화
 * - 장점: 재사용성, 가독성, 일관성 향상
 *
 * 2. 코드 구조 개선:
 * - 주석 처리된 기존 코드로 변화 과정 확인 가능
 * - 더 명확한 로직 흐름과 단계별 처리
 * - 디버깅 로그 강화로 문제 추적 용이
 *
 * 3. 예외 처리 강화:
 * - 더 상세한 예외 로깅
 * - 예외 타입별 시나리오 문서화
 * - 보안을 고려한 예외 정보 처리
 */

// === RefreshJwtFilter와의 협력 관계 ===

/**
 * 필터 체인에서의 역할 분담:
 *
 * 1. RefreshJwtFilter (우선 실행):
 * - /auth/refresh 경로에서 Refresh Token 처리
 * - 만료된 Access Token 자동 갱신
 * - 새로운 Access Token 쿠키 설정
 *
 * 2. JwtFilter (이후 실행):
 * - 일반 요청에서 Access Token 검증
 * - 유효한 토큰으로 인증 정보 설정
 * - 인증 상태를 SecurityContext에 저장
 *
 * 협력 시나리오:
 * 1. Access Token 만료 → JwtFilter 실패 → RefreshJwtFilter 개입 → 토큰 갱신
 * 2. 새로운 Access Token 발급 → 다음 요청에서 JwtFilter 성공
 */

// === 보안 고려사항 ===

/**
 * 1. 토큰 검증 강화:
 * - 서명 무결성 검사로 위변조 방지
 * - 만료시간 검증으로 오래된 토큰 차단
 * - 사용자 존재 여부 확인으로 삭제된 계정 접근 방지
 *
 * 2. 예외 정보 보안:
 * - 구체적인 실패 사유를 클라이언트에 노출하지 않음
 * - 서버 로그에만 상세 정보 기록
 * - 공격자에게 유용한 정보 차단
 *
 * 3. 컨텍스트 관리:
 * - Thread-Local SecurityContext로 요청별 격리
 * - 요청 완료 후 자동 정리 (Spring Security가 처리)
 * - 메모리 누수 방지
 */

// === 성능 최적화 ===

/**
 * 1. 토큰 검증 효율성:
 * - stateless 검증으로 서버 부하 최소화
 * - 데이터베이스 조회는 사용자 정보 로드 시에만
 * - JWT 자체 검증은 메모리 내에서 빠르게 처리
 *
 * 2. 필터 체인 최적화:
 * - 토큰 없는 경우 빠른 종료로 불필요한 처리 방지
 * - 예외 발생 시에도 요청 흐름 유지
 * - 다른 필터들과의 효율적인 협력
 *
 * 3. 메모리 관리:
 * - UserDetails 객체 재사용
 * - 불필요한 객체 생성 최소화
 * - GC 압박 감소
 */

// === 모니터링 및 디버깅 ===

/**
 * 1. 로그 레벨별 정보:
 * - DEBUG: 토큰 파싱 과정 상세 정보
 * - INFO: 인증 성공/실패 요약
 * - WARN: 의심스러운 토큰 시도
 * - ERROR: 시스템 오류 및 보안 이벤트
 *
 * 2. 성능 모니터링:
 * - 필터 처리 시간 측정
 * - 토큰 검증 실패율 추적
 * - UserDetailsService 응답 시간 모니터링
 *
 * 3. 보안 감사:
 * - 비정상적인 토큰 패턴 감지
 * - 반복적인 실패 시도 추적
 * - 의심스러운 사용자 행동 기록
 */