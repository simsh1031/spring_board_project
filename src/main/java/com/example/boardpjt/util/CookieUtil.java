package com.example.boardpjt.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * HTTP 쿠키 조작을 위한 유틸리티 클래스
 * JWT 토큰 저장, 조회, 삭제 등의 쿠키 관련 작업을 담당
 *
 * 이 클래스는 JWT 기반 인증 시스템에서 토큰을 안전하게 관리하기 위해 설계됨
 * 모든 메서드는 static으로 구현되어 인스턴스 생성 없이 사용 가능
 *
 * 주요 기능:
 * - 보안 쿠키 생성 (HttpOnly, Path 설정)
 * - 쿠키 값 조회 (키 기반 검색)
 * - 쿠키 삭제 (만료 시간 0으로 설정)
 *
 * 보안 특징:
 * - HttpOnly 플래그로 XSS 공격 방지
 * - Path 설정으로 쿠키 스코프 제한
 * - 일관된 보안 정책 적용
 */
public class CookieUtil {

    /**
     * 보안 쿠키를 생성하고 HTTP 응답에 추가하는 메서드
     * JWT Access Token, Refresh Token 등을 안전하게 클라이언트에 전송할 때 사용
     *
     * 보안 설정:
     * - HttpOnly: JavaScript에서 접근 불가능하도록 설정 (XSS 방지)
     * - Path: 쿠키가 전송될 경로를 전체 도메인(/)으로 설정
     * - MaxAge: 쿠키 유효기간을 초 단위로 설정
     *
     * 사용 예시:
     * - Access Token: CookieUtil.createCookie(response, "access_token", token, 3600);
     * - Refresh Token: CookieUtil.createCookie(response, "refresh_token", token, 604800);
     *
     * @param response HTTP 응답 객체 (쿠키 설정 대상)
     * @param key 쿠키의 이름 (예: "access_token", "refresh_token")
     * @param value 쿠키에 저장할 값 (JWT 토큰 문자열)
     * @param maxAge 쿠키 유효기간 (초 단위, 0이면 즉시 삭제)
     */
    public static void createCookie(HttpServletResponse response, String key, String value, int maxAge) {

        // === ResponseCookie 빌더 패턴으로 보안 쿠키 생성 ===
        // Spring Framework의 ResponseCookie 사용으로 더 안전하고 명확한 쿠키 설정
        ResponseCookie cookie = ResponseCookie.from(key, value)

                // === 보안 설정 ===
                .httpOnly(true) // XSS(Cross-Site Scripting) 공격 방지
                // JavaScript의 document.cookie로 접근 불가능
                // 클라이언트 사이드 스크립트에서 토큰 탈취 방지

                .path("/") // 쿠키가 유효한 경로 설정
                // "/"로 설정하면 전체 도메인에서 쿠키 전송
                // 애플리케이션의 모든 경로에서 토큰 사용 가능

                .maxAge(maxAge) // 쿠키 유효기간 설정 (초 단위)
                // Access Token: 3600 (1시간)
                // Refresh Token: 604800 (7일)
                // 0: 즉시 삭제 (쿠키 삭제 시 사용)

                // === 추가 보안 옵션 (필요시 활성화) ===
                // .secure(true)        // HTTPS에서만 전송 (운영환경 권장)
                // .sameSite("Strict")  // CSRF 공격 방지 (동일 사이트에서만 전송)
                // .domain(".example.com") // 쿠키가 유효한 도메인 설정

                .build();

        // === HTTP 응답 헤더에 Set-Cookie 추가 ===
        // "Set-Cookie" 헤더를 통해 클라이언트(브라우저)에 쿠키 저장 지시
        // 브라우저는 이 헤더를 받아 쿠키를 저장하고, 이후 요청 시 자동으로 전송
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // === 생성되는 Set-Cookie 헤더 예시 ===
        // Set-Cookie: access_token=eyJ0eXAiOiJKV1Q...; Path=/; Max-Age=3600; HttpOnly
    }

    /**
     * HTTP 요청에서 특정 이름의 쿠키 값을 조회하는 메서드
     * JWT 필터에서 Access Token, Refresh Token을 추출할 때 사용
     *
     * 동작 과정:
     * 1. 요청에 포함된 모든 쿠키 배열 조회
     * 2. 쿠키 배열을 순회하며 원하는 이름의 쿠키 검색
     * 3. 일치하는 쿠키 발견 시 값 반환, 없으면 null 반환
     *
     * 널 안전성:
     * - 쿠키가 없는 요청에 대한 안전한 처리
     * - Optional 패턴 대신 null 반환으로 간단한 처리
     *
     * @param request HTTP 요청 객체 (쿠키 정보 포함)
     * @param key 찾고자 하는 쿠키의 이름
     * @return String - 쿠키 값 (없으면 null)
     */
    public static String findCookie(HttpServletRequest request, String key) {

        // === 쿠키 존재 여부 확인 ===
        // request.getCookies()는 쿠키가 없으면 null을 반환할 수 있음
        // NullPointerException 방지를 위한 null 체크 필수
        if (request.getCookies() != null) {

            // === 모든 쿠키 순회 검색 ===
            // Cookie 배열을 순회하면서 원하는 이름의 쿠키 검색
            for (Cookie c : request.getCookies()) {

                // === 쿠키 이름 비교 ===
                // String.equals() 사용으로 정확한 이름 매칭
                // 대소문자 구분하여 정확히 일치하는 쿠키만 반환
                if (c.getName().equals(key)) {
                    return c.getValue(); // 일치하는 쿠키의 값 반환
                }

                // === 개선 고려사항 ===
                // 1. 대소문자 무시 검색: c.getName().equalsIgnoreCase(key)
                // 2. 정규표현식 패턴 매칭: Pattern.matches(pattern, c.getName())
                // 3. 여러 값 반환: List<String> 타입으로 변경
            }
        }

        // === 쿠키를 찾지 못한 경우 ===
        // null 반환으로 "쿠키 없음" 상태를 명시적으로 표현
        // 호출하는 쪽에서 null 체크를 통해 적절한 처리 수행
        return null;

        // === 사용 패턴 예시 ===
        // String token = CookieUtil.findCookie(request, "access_token");
        // if (token == null) {
        //     // 토큰 없음 - 인증되지 않은 요청
        // } else {
        //     // 토큰 있음 - 토큰 검증 진행
        // }
    }

    /**
     * 쿠키를 삭제하는 메서드
     * 로그아웃 시 클라이언트의 JWT 토큰을 제거할 때 사용
     *
     * 쿠키 삭제 원리:
     * - HTTP에는 직접적인 쿠키 삭제 방법이 없음
     * - 대신 동일한 이름으로 빈 값과 만료 시간 0인 쿠키를 전송
     * - 브라우저가 만료된 쿠키를 자동으로 삭제
     *
     * 주의사항:
     * - 삭제할 쿠키와 동일한 path, domain 설정 필요
     * - 다른 설정 시 쿠키가 삭제되지 않을 수 있음
     *
     * @param response HTTP 응답 객체 (쿠키 삭제 명령 전송용)
     * @param key 삭제할 쿠키의 이름
     */
    public static void deleteCookie(HttpServletResponse response, String key) {

        // === 만료 쿠키 생성 ===
        // 동일한 이름으로 빈 값("")과 maxAge(0)인 쿠키 생성
        // 브라우저는 maxAge가 0인 쿠키를 즉시 삭제함
        ResponseCookie cookie = ResponseCookie.from(key, "") // 빈 값으로 설정

                // === 기존 쿠키와 동일한 설정 유지 ===
                // 쿠키 삭제가 제대로 작동하려면 생성 시와 동일한 설정 필요
                .httpOnly(true) // 생성 시와 동일한 HttpOnly 설정
                .path("/")      // 생성 시와 동일한 Path 설정

                .maxAge(0)      // 만료 시간 0으로 즉시 삭제 지시
                // 음수 값(-1)은 세션 쿠키를 의미하므로 0 사용

                .build();

        // === 쿠키 삭제 명령 전송 ===
        // Set-Cookie 헤더를 통해 브라우저에 쿠키 삭제 명령 전송
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // === 생성되는 Set-Cookie 헤더 예시 ===
        // Set-Cookie: access_token=; Path=/; Max-Age=0; HttpOnly

        // === 삭제 확인 ===
        // 브라우저는 이 헤더를 받은 즉시 해당 쿠키를 삭제
        // 다음 요청부터는 해당 쿠키가 전송되지 않음
    }

    // === 추가 유틸리티 메서드 (구현 고려사항) ===

    /**
     * 모든 쿠키를 삭제하는 메서드 (구현 예정)
     * 전체 로그아웃 기능이나 계정 보안 강화 시 사용
     */
    /*
    public static void deleteAllCookies(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                deleteCookie(response, cookie.getName());
            }
        }
    }
    */

    /**
     * 쿠키 존재 여부 확인 메서드 (구현 예정)
     * 불필요한 값 추출 없이 존재 여부만 확인
     */
    /*
    public static boolean hasCookie(HttpServletRequest request, String key) {
        return findCookie(request, key) != null;
    }
    */

    /**
     * 보안 강화 쿠키 생성 메서드 (구현 예정)
     * HTTPS 환경에서 Secure, SameSite 속성 추가
     */
    /*
    public static void createSecureCookie(HttpServletResponse response, String key, String value, int maxAge, boolean isProduction) {
        ResponseCookie.Builder builder = ResponseCookie.from(key, value)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge);

        if (isProduction) {
            builder.secure(true)           // HTTPS에서만 전송
                   .sameSite("Strict");    // CSRF 공격 방지
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
    */
}

// === CookieUtil 설계 원칙 및 고려사항 ===

/**
 * 1. 정적 유틸리티 클래스:
 * - 모든 메서드가 static으로 인스턴스 생성 불필요
 * - 상태를 갖지 않는 순수 함수로 구성
 * - 어디서든 쉽게 사용 가능한 헬퍼 클래스
 *
 * 2. 보안 우선 설계:
 * - 기본적으로 HttpOnly 플래그 적용
 * - XSS 공격 방지를 위한 JavaScript 접근 차단
 * - 일관된 보안 정책 적용
 *
 * 3. 널 안전성:
 * - 쿠키가 없는 요청에 대한 안전한 처리
 * - NullPointerException 방지
 * - 명시적인 null 반환으로 상태 표현
 *
 * 4. 사용 편의성:
 * - 간단한 메서드 시그니처
 * - 직관적인 메서드명
 * - 최소한의 매개변수로 사용 편의성 향상
 */

// === JWT 토큰 쿠키 관리 패턴 ===

/**
 * 로그인 시 토큰 저장:
 * CookieUtil.createCookie(response, "access_token", accessToken, 3600);
 * CookieUtil.createCookie(response, "refresh_token", refreshToken, 604800);
 *
 * 필터에서 토큰 조회:
 * String accessToken = CookieUtil.findCookie(request, "access_token");
 * String refreshToken = CookieUtil.findCookie(request, "refresh_token");
 *
 * 로그아웃 시 토큰 삭제:
 * CookieUtil.deleteCookie(response, "access_token");
 * CookieUtil.deleteCookie(response, "refresh_token");
 */

// === 보안 강화 방안 ===

/**
 * 1. HTTPS 환경에서의 추가 보안:
 * - Secure 플래그: HTTPS에서만 쿠키 전송
 * - SameSite 속성: CSRF 공격 방지
 *
 * 2. 쿠키 암호화:
 * - 민감한 정보는 추가 암호화 고려
 * - JWT 자체가 서명되어 있어 기본 보안 제공
 *
 * 3. 도메인 제한:
 * - 특정 도메인에서만 사용할 쿠키의 경우 domain 설정
 * - 서브도메인 간 쿠키 공유 제어
 *
 * 4. 만료 시간 관리:
 * - Access Token: 짧은 만료 시간 (1시간)
 * - Refresh Token: 적절한 만료 시간 (7일)
 * - 보안과 사용자 경험의 균형 고려
 */