package com.example.boardpjt.filter;

import com.example.boardpjt.model.entity.RefreshToken;
import com.example.boardpjt.model.repository.RefreshTokenRepository;
import com.example.boardpjt.util.CookieUtil;
import com.example.boardpjt.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
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
 * Refresh Token을 활용한 자동 토큰 갱신 필터
 * Access Token 만료 시 Refresh Token을 검증하여 새로운 Access Token을 자동으로 발급
 * 사용자가 의식하지 못하는 투명한 토큰 갱신으로 끊김 없는 사용자 경험 제공
 *
 * 주요 기능:
 * - Access Token 만료 감지
 * - Refresh Token 유효성 검증 (DB와 비교)
 * - 새로운 Access Token 자동 발급
 * - SecurityContext에 인증 정보 설정
 *
 * 보안 특징:
 * - 서버 사이드 Refresh Token 상태 관리
 * - 토큰 불일치 시 갱신 거부
 * - 만료/무효 토큰에 대한 안전한 처리
 */
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입용)
public class RefreshJwtFilter extends OncePerRequestFilter {

    // JWT 토큰 생성, 검증, 파싱을 담당하는 유틸리티 클래스
    private final JwtUtil jwtUtil;

    // 사용자 정보를 데이터베이스에서 로드하는 서비스
    private final UserDetailsService userDetailsService;

    // Refresh Token을 데이터베이스에서 관리하기 위한 Repository
    // 서버에서 Refresh Token의 유효성을 추적하고 관리
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * HTTP 요청마다 실행되는 필터 메인 로직
     * Access Token의 상태를 확인하고, 만료된 경우 Refresh Token으로 자동 갱신
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

        // === 필터 실행 시작 로그 ===
        System.out.println("[Refresh Jwt Filter] - 토큰 갱신 필터 시작");

        // === 1단계: Access Token 존재 여부 확인 ===
        // CookieUtil을 사용하여 요청에서 Access Token 추출
        String accessToken = CookieUtil.findCookie(request, "access_token");

        if (accessToken == null) {
            // Access Token이 없는 경우:
            // - 비로그인 사용자의 공개 페이지 접근
            // - 이미 완전히 로그아웃된 상태
            // - 쿠키가 삭제되거나 손상된 경우
            System.out.println("[Refresh Filter] Access Token 없음 - 갱신 불필요");
            filterChain.doFilter(request, response);
            return; // 토큰 갱신 로직 건너뛰고 다음 필터로 진행
        }

        // === 2단계: Access Token 유효성 검증 ===
        try {
            // Access Token의 유효성을 검증
            // 주의: 여기서는 DB 조회 없이 토큰 자체의 유효성만 검사
            // jwtUtil.getClaims(accessToken); // 대안: 클레임 추출로 검증
            jwtUtil.validateToken(accessToken);

            // 토큰이 유효한 경우: 갱신 불필요, 다음 필터(JwtFilter)에서 정상 처리됨
            System.out.println("[Refresh Filter] Access Token 유효 - 갱신 불필요");

        } catch (ExpiredJwtException ex) {
            // === Access Token 만료 시 자동 갱신 처리 ===
            System.out.println("[Refresh Filter] Access Token 만료 감지 - 자동 갱신 시작");

            // Refresh Token을 사용하여 새로운 Access Token 발급
            handleRefreshToken(request, response);

            // 갱신 처리 완료 후 다음 필터로 진행
            // 성공 시: 새로운 Access Token으로 JwtFilter에서 정상 처리
            // 실패 시: JwtFilter에서 인증 실패 처리

        } catch (Exception e) {
            // === 기타 토큰 오류 처리 ===
            // ExpiredJwtException 외의 다른 JWT 관련 예외들:
            // - MalformedJwtException: 잘못된 토큰 형식
            // - SignatureException: 서명 검증 실패
            // - 기타 보안 관련 예외들
            System.out.println("[Refresh Filter] Access Token 오류 (만료 외): " + e.getClass().getSimpleName());

            // 토큰 갱신 없이 다음 필터로 진행
            // JwtFilter에서 동일한 예외가 발생하여 인증 실패 처리됨
            filterChain.doFilter(request, response);
            return;
        }

        // === 3단계: 다음 필터로 요청 전달 ===
        // 모든 경우에 대해 필터 체인 계속 진행
        // - 토큰 유효: JwtFilter에서 정상 인증 처리
        // - 토큰 갱신 완료: 새 토큰으로 JwtFilter에서 인증 처리
        // - 갱신 실패: JwtFilter에서 인증 실패 처리
        filterChain.doFilter(request, response);
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token을 발급하는 메서드
     * 서버에 저장된 Refresh Token과 클라이언트의 토큰을 비교하여 안전성 보장
     *
     * @param request HTTP 요청 객체 (Refresh Token 쿠키 추출용)
     * @param response HTTP 응답 객체 (새 Access Token 쿠키 설정용)
     */
    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println("[Refresh Filter] Refresh Token 갱신 처리 시작");

            // === 1단계: Refresh Token 추출 ===
            String refreshToken = CookieUtil.findCookie(request, "refresh_token");

            if (refreshToken == null) {
                // Refresh Token이 없는 경우:
                // - Access Token만 만료되고 Refresh Token은 삭제된 상태
                // - 부분적인 로그아웃 상태 (비정상적 상황)
                // - 쿠키 조작 또는 손상
                System.out.println("[Refresh Filter] Refresh Token 없음 - 갱신 불가");
                return; // 갱신 중단, 사용자는 재로그인 필요
            }

            // === 2단계: Refresh Token에서 사용자 정보 추출 ===
            String username = jwtUtil.getUsername(refreshToken);
            System.out.println("[Refresh Filter] Refresh Token에서 추출된 사용자: " + username);

            // === 3단계: 서버 저장 Refresh Token과 비교 검증 ===
            // 데이터베이스에서 해당 사용자의 Refresh Token 조회
            RefreshToken storedToken = refreshTokenRepository.findById(username)
                    .orElseThrow(() -> new RuntimeException("서버에 저장된 Refresh Token 없음"));

            // 클라이언트의 Refresh Token과 서버 저장 토큰 비교
            if (!refreshToken.equals(storedToken.getToken())) {
                // 토큰 불일치 시나리오:
                // - 다른 기기에서 로그인하여 기존 토큰 무효화됨
                // - 토큰 탈취 시도 (보안 위험)
                // - 토큰 조작 시도
                throw new RuntimeException("Refresh Token 불일치 - 보안 위험 감지");
            }

            System.out.println("[Refresh Filter] Refresh Token 검증 완료");

            // === 4단계: 새로운 Access Token 발급 ===
            // Refresh Token에서 사용자 권한 정보 추출
            String role = jwtUtil.getRole(refreshToken);

            // 새로운 Access Token 생성 (1시간 유효)
            String newAccessToken = jwtUtil.generateToken(username, role, false);

            // 새 Access Token을 HTTP 쿠키로 설정
            CookieUtil.createCookie(response, "access_token", newAccessToken, 60 * 60); // 1시간

            System.out.println("[Refresh Filter] 새 Access Token 발급 완료");

            // === 5단계: SecurityContext에 인증 정보 설정 ===
            // 갱신된 토큰으로 즉시 인증 상태 설정하여 현재 요청 처리 가능
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,                    // 인증된 사용자 정보
                    null,                          // 자격증명 (JWT 방식에서는 불필요)
                    userDetails.getAuthorities()   // 사용자 권한 목록
            );

            // SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("[Refresh Filter] 인증 정보 설정 완료 - 토큰 갱신 성공");

            // === 추가 보안 고려사항 ===
            // 1. Refresh Token 회전 (Rotation) 정책 적용 고려:
            //    새 Refresh Token도 함께 발급하여 보안 강화
            // 2. 갱신 이력 로깅: 감사 추적을 위한 갱신 기록
            // 3. 비정상적 갱신 패턴 감지: 짧은 시간 내 반복 갱신 모니터링

        } catch (Exception e) {
            // === Refresh Token 갱신 실패 처리 ===
            System.err.println("[Refresh Filter] 토큰 갱신 실패: " + e.getMessage());

            // === 가능한 실패 시나리오들 ===
            // 1. ExpiredJwtException: Refresh Token도 만료됨
            // 2. 토큰 불일치: 보안 위험 또는 다중 기기 로그인
            // 3. 사용자 없음: 계정 삭제 또는 비활성화
            // 4. 네트워크/DB 오류: 일시적 시스템 장애

            // === 실패 시 처리 방침 ===
            // 1. SecurityContext 정리하지 않음 (이미 비어있음)
            // 2. 쿠키 삭제하지 않음 (사용자 의도와 다를 수 있음)
            // 3. 다음 필터에서 인증 실패로 처리됨
            // 4. 사용자에게는 일반적인 로그인 요구 메시지 표시

            // 보안상 구체적인 실패 사유는 로그에만 기록
            // 클라이언트에는 일반적인 인증 실패로 처리됨
        }
    }
}

// === RefreshJwtFilter 동작 원리 ===

/**
 * 토큰 갱신 플로우:
 *
 * 1. 정상 상황 (Access Token 유효):
 *    요청 → RefreshFilter(통과) → JwtFilter(인증) → 컨트롤러
 *
 * 2. 토큰 만료 상황:
 *    요청 → RefreshFilter(만료 감지) → Refresh Token 검증 →
 *    새 Access Token 발급 → JwtFilter(새 토큰 인증) → 컨트롤러
 *
 * 3. 갱신 실패 상황:
 *    요청 → RefreshFilter(갱신 실패) → JwtFilter(인증 실패) →
 *    SecurityConfig(로그인 페이지 리다이렉트)
 */

// === 보안 고려사항 ===

/**
 * 1. 이중 검증 시스템:
 * - 클라이언트 Refresh Token vs 서버 저장 토큰 비교
 * - 토큰 탈취나 조작 시도 감지 가능
 * - 다중 기기 로그인 시 기존 토큰 무효화 처리
 *
 * 2. 서버 사이드 상태 관리:
 * - Refresh Token을 DB에 저장하여 중앙 집중 관리
 * - 필요시 특정 사용자의 모든 토큰 강제 무효화 가능
 * - 보안 사고 발생 시 빠른 대응 가능
 *
 * 3. 예외 정보 보안:
 * - 구체적인 실패 사유는 서버 로그에만 기록
 * - 클라이언트에는 일반적인 인증 실패만 전달
 * - 공격자에게 유용한 정보 노출 방지
 */

// === 성능 최적화 ===

/**
 * 1. 선택적 처리:
 * - Access Token이 유효한 경우 갱신 로직 건너뛰기
 * - 불필요한 DB 조회 최소화
 * - 대부분의 요청에서 빠른 통과
 *
 * 2. 효율적인 토큰 검증:
 * - validateToken()으로 빠른 유효성 검사
 * - 만료 시에만 복잡한 갱신 로직 수행
 * - DB 조회는 Refresh Token 검증 시에만
 *
 * 3. 메모리 관리:
 * - 토큰 갱신 시에만 UserDetails 로드
 * - 불필요한 객체 생성 최소화
 * - GC 압박 감소
 */

// === 사용자 경험 ===

/**
 * 1. 투명한 갱신:
 * - 사용자가 토큰 만료를 의식하지 못함
 * - 연속적인 서비스 이용 가능
 * - 갑작스러운 로그인 요구 최소화
 *
 * 2. 장기간 로그인 유지:
 * - Refresh Token 7일로 장기 세션 지원
 * - 자주 사용하는 서비스에서 편의성 제공
 * - 재로그인 빈도 감소
 *
 * 3. 안전한 다중 기기 지원:
 * - 새 기기 로그인 시 기존 토큰 무효화
 * - 각 기기별 독립적인 세션 관리
 * - 보안과 편의성의 균형
 */

// === 추가 개선사항 ===

/**
 * 1. Refresh Token 회전 (Rotation):
 * - 갱신 시 새로운 Refresh Token도 함께 발급
 * - 기존 Refresh Token 무효화로 보안 강화
 * - 토큰 탈취 위험 최소화
 *
 * 2. 갱신 이력 추적:
 * - 토큰 갱신 시간, IP 주소 등 기록
 * - 비정상적인 갱신 패턴 감지
 * - 보안 감사 및 사용자 활동 분석
 *
 * 3. 적응적 만료 시간:
 * - 사용자 활동 패턴에 따른 동적 만료 시간 조정
 * - 활발한 사용자는 더 긴 유효기간
 * - 비활성 사용자는 더 짧은 유효기간
 */