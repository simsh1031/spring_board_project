package com.example.boardpjt.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT(JSON Web Token) 관련 유틸리티 클래스
 * 토큰 생성, 검증, 파싱 기능을 제공하며 Access Token과 Refresh Token을 모두 지원
 * 설정 파일(application.yml)에서 비밀키와 만료 시간을 주입받아 사용
 */
@Component // Spring 컨테이너가 관리하는 빈으로 등록
public class JwtUtil {

    // JWT 서명에 사용할 비밀키 (HMAC-SHA 알고리즘 사용)
    private final SecretKey secretKey;

    // Access Token 만료 시간 (밀리초 단위)
    private final Long accessExpiry;

    // Refresh Token 만료 시간 (밀리초 단위)
    private final Long refreshExpiry;

    /**
     * JwtUtil 생성자
     * application.yml 설정값을 주입받아 JWT 관련 설정을 초기화
     *
     * @param secret JWT 서명용 비밀키 문자열 (application.yml의 jwt.secret 값)
     * @param accessExpiry Access Token 만료 시간 (application.yml의 jwt.expiry.access 값)
     * @param refreshExpiry Refresh Token 만료 시간 (application.yml의 jwt.expiry.refresh 값)
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,           // 예: "mySecretKey123456789012345678901234567890"
            @Value("${jwt.expiry.access}") Long accessExpiry, // 예: 3600000 (1시간)
            @Value("${jwt.expiry.refresh}") Long refreshExpiry) { // 예: 604800000 (7일)

        // === 비밀키 생성 ===
        // 문자열 비밀키를 HMAC-SHA 알고리즘용 SecretKey 객체로 변환
        // UTF-8 인코딩을 사용하여 바이트 배열로 변환 후 SecretKey 생성
        // 주의: 비밀키는 최소 256비트(32바이트) 이상이어야 함
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;

        // === 설정값 확인용 로그 (운영환경에서는 보안상 제거 권장) ===
        System.out.println("JWT 비밀키 설정 완료: " + secret);
        System.out.println("Access Token 만료시간: " + accessExpiry + "ms (" + (accessExpiry/1000/60) + "분)");
        System.out.println("Refresh Token 만료시간: " + refreshExpiry + "ms (" + (refreshExpiry/1000/60/60/24) + "일)");
    }

    /**
     * JWT 토큰을 생성하는 메서드
     * 사용자 정보와 권한을 포함한 Access Token 또는 Refresh Token 생성
     *
     * @param username 토큰에 포함할 사용자명 (JWT의 subject 클레임)
     * @param role 사용자 권한 정보 (커스텀 클레임으로 추가)
     * @param isRefresh true면 Refresh Token, false면 Access Token 생성
     * @return String 생성된 JWT 토큰 문자열
     */
    public String generateToken(String username, String role, boolean isRefresh) {
        return Jwts.builder()
                // === JWT Payload 설정 ===

                // subject 클레임: 토큰의 주체(사용자명) 설정
                // 표준 클레임으로 일반적으로 사용자 식별자를 저장
                .subject(username)

                // 커스텀 클레임: 사용자 권한 정보 추가
                // "role" 키로 사용자의 권한 정보 저장 (예: "ROLE_USER", "ROLE_ADMIN")
                .claim("role", role)

                // === JWT 시간 설정 ===

                // iat (issued at): 토큰 발급 시간 설정
                .issuedAt(new Date())

                // exp (expiration): 토큰 만료 시간 설정
                // 현재 시간 + (Refresh Token이면 refreshExpiry, Access Token이면 accessExpiry)
                // System.currentTimeMillis(): 현재 시간을 밀리초로 반환
                .expiration(new Date(System.currentTimeMillis() + (isRefresh ? refreshExpiry : accessExpiry)))

                // === JWT 서명 ===

                // 비밀키로 토큰에 서명하여 무결성 보장
                // HMAC-SHA 알고리즘 사용으로 토큰 위변조 방지
                .signWith(secretKey)

                // JWT 문자열로 직렬화하여 반환
                // 형식: "header.payload.signature"
                .compact();
    }

    /**
     * JWT 토큰에서 클레임(페이로드 데이터)을 추출하는 메서드
     * 토큰 검증과 파싱을 동시에 수행
     *
     * @param token 파싱할 JWT 토큰 문자열
     * @return Claims 토큰에 포함된 모든 클레임 정보
     * @throws 토큰이 유효하지 않을 경우 다양한 JWT 예외 발생
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                // 서명 검증용 비밀키 설정
                .verifyWith(secretKey)

                // JWT 파서 빌드
                .build()

                // 토큰 파싱 및 서명 검증
                // 이 과정에서 토큰 형식, 서명, 만료시간 등이 모두 검증됨
                .parseSignedClaims(token)

                // 페이로드(클레임) 데이터 반환
                .getPayload();
    }

    /**
     * JWT 토큰에서 사용자명(subject)을 추출하는 메서드
     *
     * @param token JWT 토큰 문자열
     * @return String 토큰에 포함된 사용자명
     */
    public String getUsername(String token) {
        // getClaims()를 통해 토큰 검증 후 subject 클레임 반환
        return getClaims(token).getSubject();
    }

    /**
     * JWT 토큰에서 사용자 권한(role)을 추출하는 메서드
     *
     * @param token JWT 토큰 문자열
     * @return String 토큰에 포함된 권한 정보
     */
    public String getRole(String token) {
        // getClaims()를 통해 토큰 검증 후 "role" 커스텀 클레임을 String 타입으로 반환
        return getClaims(token).get("role", String.class);
    }

    /**
     * JWT 토큰의 유효성을 검증하는 메서드
     * 토큰 형식, 서명, 만료시간 등을 종합적으로 검증
     *
     * @param token 검증할 JWT 토큰 문자열
     * @return boolean 토큰이 유효하면 true, 유효하지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            // getClaims() 메서드를 호출하여 토큰 파싱 시도
            // 파싱 성공 = 토큰이 유효함 (형식, 서명, 만료시간 모두 정상)
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw e;
            // validate를 쓰고 싶으면 시간만료에 대해서는 다시금 상위처리를 하게...
        } catch (Exception e) {
            // === 가능한 예외 종류들 ===
            // 1. MalformedJwtException: 잘못된 JWT 형식
            // 2. ExpiredJwtException: 토큰 만료
            // 3. UnsupportedJwtException: 지원하지 않는 JWT
            // 4. IllegalArgumentException: 잘못된 인수
            // 5. SignatureException: 서명 검증 실패
            // 6. 기타 보안 관련 예외들

            // 모든 예외를 false로 처리 (토큰 무효)
            // 운영환경에서는 로깅 추가 고려: log.warn("JWT validation failed", e);
            return false;
        }
    }
}