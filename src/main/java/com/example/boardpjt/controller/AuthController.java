package com.example.boardpjt.controller;

import com.example.boardpjt.model.entity.RefreshToken;
import com.example.boardpjt.model.repository.RefreshTokenRepository;
import com.example.boardpjt.service.UserAccountService;
import com.example.boardpjt.util.CookieUtil;
import com.example.boardpjt.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 사용자 인증 관련 컨트롤러 (Refresh Token 지원 버전)
 * 회원가입, 로그인, 로그아웃 기능을 담당하며 HTML 뷰를 반환하는 전통적인 MVC 컨트롤러
 * Access Token과 Refresh Token을 모두 지원하는 완전한 JWT 인증 시스템
 */
@Controller // Spring MVC 컨트롤러로 등록 (view 반환 → template → html 렌더링, thymeleaf 사용)
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입용)
@RequestMapping("/auth") // 모든 요청 경로에 "/auth" 접두사 추가 (/auth/**)
public class AuthController {

    // 사용자 계정 관련 비즈니스 로직을 처리하는 서비스
    private final UserAccountService userAccountService;

    // Spring Security의 인증 매니저 (사용자 로그인 인증 처리)
    private final AuthenticationManager authenticationManager;

    // JWT 토큰 생성, 검증, 파싱을 담당하는 유틸리티 클래스
    private final JwtUtil jwtUtil;

    // Refresh Token을 데이터베이스에서 관리하기 위한 Repository
    // 서버에서 Refresh Token의 유효성을 추적하고 관리
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 회원가입 페이지를 보여주는 메서드
     * GET 요청으로 회원가입 폼을 사용자에게 제공
     *
     * @return String - 렌더링할 템플릿 파일명 (templates/register.html)
     */
    @GetMapping("/register") // GET /auth/register 요청 처리
    public String registerForm() {
        // Thymeleaf 템플릿 엔진이 templates/register.html 파일을 찾아서 렌더링
        return "register";
    }

    /**
     * 회원가입 처리 메서드
     * POST 요청으로 전송된 사용자 정보를 받아서 회원가입을 처리
     *
     * @param username 사용자가 입력한 사용자명
     * @param password 사용자가 입력한 비밀번호
     * @param redirectAttributes 리다이렉트 시 데이터 전달을 위한 객체
     * @return String - 리다이렉트할 경로
     */
    @PostMapping("/register") // POST /auth/register 요청 처리
    public String register(@RequestParam String username, // HTML 폼의 username 파라미터
                           @RequestParam String password, // HTML 폼의 password 파라미터
                           RedirectAttributes redirectAttributes) { // 리다이렉트 시 속성 전달

        // TODO: @Valid 어노테이션으로 유효성 검증 추가 가능 (Bean Validation)
        try {
            // UserAccountService를 통해 회원가입 처리 (사용자 저장, 비밀번호 암호화 등)
            userAccountService.register(username, password);

            // 회원가입 성공 시 홈페이지로 리다이렉트
            return "redirect:/";

        } catch (IllegalArgumentException e) {
            // 회원가입 실패 (예: 중복 사용자명) 시 예외 처리

            // addAttribute vs addFlashAttribute 차이점:
            // addAttribute: URL 파라미터로 전달 (?error=message)
            // addFlashAttribute: 세션에 임시 저장 후 한 번만 사용되고 자동 제거
            redirectAttributes.addFlashAttribute("error", e.getMessage());

            // 에러 메시지와 함께 회원가입 페이지로 다시 리다이렉트
            return "redirect:/auth/register";
        }
    }

    /**
     * 로그인 페이지를 보여주는 메서드
     * GET 요청으로 로그인 폼을 사용자에게 제공
     *
     * @return String - 렌더링할 템플릿 파일명 (templates/login.html)
     */
    @GetMapping("/login") // GET /auth/login 요청 처리
    public String loginForm() {
        // Thymeleaf 템플릿 엔진이 templates/login.html 파일을 찾아서 렌더링
        return "login";
    }

    /**
     * 로그인 처리 메서드 (Refresh Token 지원)
     * POST 요청으로 전송된 로그인 정보를 받아서 인증을 처리하고
     * Access Token과 Refresh Token을 모두 발급
     *
     * @param username 사용자가 입력한 사용자명
     * @param password 사용자가 입력한 비밀번호
     * @param response HTTP 응답 객체 (쿠키 설정을 위해 사용)
     * @param redirectAttributes 리다이렉트 시 데이터 전달을 위한 객체
     * @return String - 리다이렉트할 경로
     */
    @PostMapping("/login") // POST /auth/login 요청 처리
    public String login(@RequestParam String username, // HTML 폼의 username 파라미터
                        @RequestParam String password, // HTML 폼의 password 파라미터
                        HttpServletResponse response, // HTTP 응답 객체 (쿠키 설정용)
                        RedirectAttributes redirectAttributes) { // 리다이렉트 시 속성 전달
        try {
            // === 1단계: 사용자 인증 ===
            // Spring Security의 AuthenticationManager를 통해 사용자 인증 시도
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            username, password // 사용자명과 비밀번호로 인증 토큰 생성
                    ));
            // 인증 성공 시 Authentication 객체에 사용자 정보와 권한이 포함됨

            // === 2단계: Access Token 생성 및 쿠키 설정 ===
            // 인증된 사용자 정보를 바탕으로 JWT Access Token 생성
            String accessToken = jwtUtil.generateToken(
                    username, // 토큰 subject (사용자명)
                    authentication.getAuthorities().toString(), // 사용자 권한 정보
                    false // Access Token 타입 (Refresh Token이 아님)
            );

            // CookieUtil을 사용하여 Access Token을 HTTP 쿠키로 설정
            // 매개변수: (응답 객체, 쿠키명, 토큰값, 유효시간(초))
            CookieUtil.createCookie(response, "access_token", accessToken, 60 * 60); // 1시간

            // === 3단계: Refresh Token 생성, 저장 및 쿠키 설정 ===
            // Refresh Token은 Access Token보다 훨씬 긴 유효기간을 가짐
            String refreshToken = jwtUtil.generateToken(
                    username,
                    authentication.getAuthorities().toString(),
                    true // Refresh Token 타입
            );

            // Refresh Token을 데이터베이스에 저장
            // 서버에서 Refresh Token의 유효성을 추적하고 관리할 수 있음
            // 로그아웃 시나 보안 이슈 발생 시 강제 무효화 가능
            refreshTokenRepository.save(new RefreshToken(username, refreshToken));

            // Refresh Token을 HTTP 쿠키로 설정 (7일 유효)
            CookieUtil.createCookie(response, "refresh_token", refreshToken, 60 * 60 * 24 * 7); // 7일

            // === 4단계: 로그인 성공 후 리다이렉트 ===
            // 인증 완료 후 마이페이지로 이동
            return "redirect:/my-page";

        } catch (Exception e) {
            // === 로그인 실패 처리 ===
            // 인증 실패 시 (잘못된 사용자명/비밀번호, 계정 비활성화 등)
            redirectAttributes.addFlashAttribute("error", "로그인 실패");

            // 에러 메시지와 함께 로그인 페이지로 다시 리다이렉트
            return "redirect:/auth/login";
        }
    }

    /**
     * 로그아웃 처리 메서드
     * 클라이언트의 토큰 쿠키를 삭제하고 서버의 Refresh Token도 무효화
     * 완전한 로그아웃을 위해 클라이언트와 서버 양쪽에서 토큰 정리
     *
     * @param response HTTP 응답 객체 (쿠키 삭제용)
     * @param authentication 현재 인증된 사용자 정보 (Refresh Token 삭제용)
     * @return String - 로그아웃 후 리다이렉트할 경로
     */
    @PostMapping("/logout") // POST /auth/logout 요청 처리
    public String logout(HttpServletResponse response, Authentication authentication) {

        // === 1단계: 클라이언트 쿠키 삭제 ===
        // 브라우저에 저장된 Access Token과 Refresh Token 쿠키를 모두 삭제
        // 이렇게 하면 클라이언트에서는 더 이상 토큰을 사용할 수 없음
        CookieUtil.deleteCookie(response, "access_token");
        CookieUtil.deleteCookie(response, "refresh_token");

        // === 2단계: 서버 Refresh Token 삭제 ===
        // 데이터베이스에 저장된 Refresh Token을 삭제하여 완전히 무효화
        // 이는 보안상 매우 중요한 단계:
        // - 쿠키가 탈취되더라도 서버에서 토큰이 무효화되어 있어 사용 불가
        // - 다른 기기에서 동일한 Refresh Token 사용 방지
        refreshTokenRepository.deleteById(authentication.getName());

        // === 3단계: 로그아웃 후 리다이렉트 ===
        // 정책에 따라 다른 경로로 리다이렉트 가능:

        return "redirect:/"; // 홈페이지로 이동 (모든 사용자 접근 가능)
        // 대안: return "redirect:/auth/login"; // 로그인 페이지로 이동 (다른 계정 로그인 유도)

        // === 로그아웃 정책 고려사항 ===
        // 1. 홈페이지 리다이렉트: 사용자가 로그아웃 후에도 서비스를 계속 둘러볼 수 있음
        // 2. 로그인 페이지 리다이렉트: 즉시 다른 계정으로 로그인하도록 유도
        // 3. 로그아웃 확인 페이지: "로그아웃되었습니다" 메시지와 함께 선택지 제공
    }

    // === 추가 구현 고려사항 ===

    /**
     * 토큰 갱신 엔드포인트 (구현 예정)
     * Refresh Token을 사용하여 새로운 Access Token 발급
     * RefreshJwtFilter에서 처리되지만, 명시적인 엔드포인트도 고려 가능
     */
    /*
    @PostMapping("/refresh")
    @ResponseBody
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Refresh Token 검증 및 새로운 Access Token 발급
        // RefreshJwtFilter와 중복되므로 필요시에만 구현
    }
    */

    /**
     * 모든 기기에서 로그아웃 (구현 예정)
     * 해당 사용자의 모든 Refresh Token을 무효화
     */
    /*
    @PostMapping("/logout-all")
    public String logoutFromAllDevices(Authentication authentication, HttpServletResponse response) {
        // 현재 사용자의 모든 Refresh Token 삭제
        refreshTokenRepository.deleteAllByUsername(authentication.getName());

        // 현재 기기의 쿠키도 삭제
        CookieUtil.deleteCookie(response, "access_token");
        CookieUtil.deleteCookie(response, "refresh_token");

        return "redirect:/?message=all-devices-logout";
    }
    */
}

// === 주요 업데이트 사항 분석 ===

/**
 * 1. Refresh Token 완전 지원:
 * - 로그인 시 Access Token + Refresh Token 동시 발급
 * - Refresh Token은 데이터베이스에 저장하여 서버에서 상태 관리
 * - 쿠키 유효기간: Access Token(1시간) vs Refresh Token(7일)
 *
 * 2. CookieUtil 도입:
 * - 쿠키 생성/삭제 로직을 유틸리티 클래스로 분리
 * - 코드 재사용성과 유지보수성 향상
 * - 일관된 쿠키 설정 정책 적용
 *
 * 3. 완전한 로그아웃 구현:
 * - 클라이언트 쿠키 삭제 + 서버 Refresh Token 무효화
 * - 보안성 크게 향상 (토큰 탈취 시에도 안전)
 * - 다중 기기 로그인 시나리오 고려
 *
 * 4. 확장 가능한 구조:
 * - 모든 기기 로그아웃 기능 추가 가능
 * - 토큰 갱신 엔드포인트 추가 가능
 * - 세션 관리 및 감사 로그 기능 확장 가능
 */

// === 보안 개선사항 ===

/**
 * 1. 이중 토큰 보안:
 * - Access Token: 짧은 수명으로 노출 위험 최소화
 * - Refresh Token: 서버 검증으로 탈취 시에도 무효화 가능
 *
 * 2. 서버 사이드 토큰 관리:
 * - Refresh Token을 DB에 저장하여 중앙 집중 관리
 * - 필요시 강제 무효화 가능 (계정 보안 이슈 발생 시)
 *
 * 3. 완전한 로그아웃:
 * - 클라이언트와 서버 양쪽에서 토큰 정리
 * - 로그아웃 후 토큰 재사용 완전 차단
 *
 * 4. 쿠키 보안:
 * - HttpOnly 플래그로 XSS 공격 방지
 * - Secure 플래그로 HTTPS 전용 전송 (운영환경)
 * - SameSite 속성으로 CSRF 공격 방지
 */

// === 사용자 경험 개선 ===

/**
 * 1. 자동 토큰 갱신:
 * - RefreshJwtFilter가 만료된 Access Token을 자동 갱신
 * - 사용자가 의식하지 못하는 원활한 인증 유지
 *
 * 2. 긴 로그인 유지:
 * - Refresh Token 7일로 장기간 로그인 상태 유지
 * - 자주 사용하는 서비스에서 재로그인 번거로움 최소화
 *
 * 3. 안전한 로그아웃:
 * - 완전한 토큰 무효화로 보안 우려 해소
 * - 공용 PC 등에서 안심하고 사용 가능
 */

// === 성능 고려사항 ===

/**
 * 1. 토큰 검증 최적화:
 * - Access Token은 stateless 검증 (빠름)
 * - Refresh Token만 DB 조회 (필요시에만)
 *
 * 2. 데이터베이스 부하 관리:
 * - Refresh Token 테이블 인덱스 최적화
 * - 만료된 토큰 정기 정리 배치 작업
 *
 * 3. 쿠키 크기 최적화:
 * - JWT 페이로드 최소화
 * - 필수 정보만 토큰에 포함
 */