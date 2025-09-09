package com.example.boardpjt.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * Refresh Token 정보를 Redis에 저장하기 위한 엔티티 클래스
 * JWT 기반 인증 시스템에서 Access Token 갱신을 위한 Refresh Token을 관리
 *
 * 설계 특징:
 * - JPA 대신 Spring Data Redis 사용으로 성능 최적화
 * - TTL(Time To Live) 설정으로 자동 만료 처리
 * - 사용자명을 Key로 사용하여 빠른 조회 지원
 * - 서버 사이드 토큰 상태 관리로 보안 강화
 *
 * Redis 활용 이유:
 * - 메모리 기반 빠른 조회 성능
 * - TTL 기능으로 자동 만료 처리
 * - 세션 데이터 관리에 적합한 특성
 * - 스케일 아웃 환경에서 공유 저장소 역할
 */
@Getter // Lombok: 모든 필드에 대한 getter 메서드 자동 생성
@NoArgsConstructor // Lombok: 매개변수 없는 기본 생성자 자동 생성 (Redis 직렬화/역직렬화용)
@AllArgsConstructor // Lombok: 모든 필드를 매개변수로 받는 생성자 자동 생성
// 참고: Record 클래스로도 대체 가능하지만, Redis 직렬화 호환성을 위해 일반 클래스 사용

@RedisHash(value = "refreshToken",        // Redis에서 사용할 키 접두사
        timeToLive = 60 * 60 * 24 * 7) // TTL: 7일 (604800초)
// RedisHash 설정 상세:
// - value: Redis 키 패턴 "refreshToken:{username}" 형태로 저장
// - timeToLive: 자동 만료 시간 (초 단위)
// - 7일 = 60초 * 60분 * 24시간 * 7일 = 604,800초
//
// TTL과 JWT 만료시간 동기화 고려사항:
// - JWT Refresh Token 만료시간과 Redis TTL이 일치하는 것이 이상적
// - 완전 일치가 아니어도 기능상 문제없음 (JWT 자체 만료 검증이 우선)
// - Redis TTL이 약간 길어도 JWT 만료 검증으로 보안 보장
public class RefreshToken {

    /**
     * Refresh Token의 식별자 (Redis Key)
     * 사용자명을 Primary Key로 사용하여 사용자별 고유한 토큰 관리
     *
     * Redis 저장 구조:
     * - Key: "refreshToken:username" (예: "refreshToken:user123")
     * - Value: RefreshToken 객체 직렬화된 데이터
     *
     * 장점:
     * - 사용자명으로 빠른 토큰 조회 가능
     * - 한 사용자당 하나의 활성 Refresh Token 보장
     * - 새 로그인 시 기존 토큰 자동 덮어쓰기 (보안)
     */
    @Id // Spring Data Redis: Redis Key로 사용할 필드 지정
    private String username;

    /**
     * 실제 Refresh Token 문자열
     * JWT 형태의 토큰 값이 저장됨
     *
     * 토큰 형태 예시:
     * "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIiwicm9sZSI6IlJPTEVfVVNFUiIsImlhdCI6MTYzOTEyMzQ1NiwiZXhwIjoxNjM5NzI4MjU2fQ.signature"
     *
     * 보안 고려사항:
     * - Redis 접근 권한 적절히 설정 필요
     * - 네트워크 구간 암호화 (TLS) 권장
     * - Redis 인증 설정 권장
     */
    private String token;

    // === RefreshToken 엔티티 사용 패턴 ===

    /**
     * 토큰 저장 시나리오:
     * 1. 사용자 로그인 성공
     * 2. Access Token + Refresh Token 생성
     * 3. RefreshToken 객체 생성하여 Redis 저장
     * 4. 클라이언트에 두 토큰 모두 쿠키로 전송
     *
     * 예시 코드:
     * RefreshToken refreshToken = new RefreshToken(username, jwtRefreshToken);
     * refreshTokenRepository.save(refreshToken);
     */

    /**
     * 토큰 검증 시나리오:
     * 1. Access Token 만료 감지
     * 2. 클라이언트의 Refresh Token 추출
     * 3. Redis에서 사용자별 저장된 토큰 조회
     * 4. 클라이언트 토큰과 서버 저장 토큰 비교
     * 5. 일치 시 새 Access Token 발급
     *
     * 예시 코드:
     * Optional<RefreshToken> stored = refreshTokenRepository.findById(username);
     * if (stored.isPresent() && clientToken.equals(stored.get().getToken())) {
     *     // 새 Access Token 발급
     * }
     */

    /**
     * 토큰 무효화 시나리오:
     * 1. 사용자 로그아웃
     * 2. Redis에서 해당 사용자의 Refresh Token 삭제
     * 3. 클라이언트 쿠키에서 토큰 제거
     *
     * 예시 코드:
     * refreshTokenRepository.deleteById(username);
     */
}

// === Redis 설정 및 성능 고려사항 ===

/**
 * Redis 구성 권장사항:
 *
 * 1. Redis 설정 (application.yml):
 * spring:
 *   redis:
 *     host: localhost
 *     port: 6379
 *     password: ${REDIS_PASSWORD}
 *     timeout: 2000ms
 *     jedis:
 *       pool:
 *         max-active: 8
 *         max-idle: 8
 *         min-idle: 0
 *
 * 2. Redis 보안 설정:
 * - 인증 활성화: requirepass 설정
 * - 네트워크 보안: bind 설정으로 접근 IP 제한
 * - TLS 암호화: Redis 6.0+ TLS 지원
 * - 방화벽: Redis 포트 접근 제한
 *
 * 3. 성능 최적화:
 * - 연결 풀 설정으로 연결 재사용
 * - 적절한 timeout 설정
 * - 메모리 정책 설정 (maxmemory-policy)
 */

// === TTL 관리 전략 ===

/**
 * TTL(Time To Live) 관리 방식:
 *
 * 1. 고정 TTL (현재 구현):
 * - 모든 토큰이 동일한 7일 TTL
 * - 간단한 구현 및 관리
 * - 예측 가능한 토큰 생명주기
 *
 * 2. 동적 TTL (향후 고려):
 * - 사용자 활동에 따른 TTL 조정
 * - 활발한 사용자는 더 긴 TTL
 * - 보안 등급에 따른 차등 TTL
 *
 * 3. 토큰 갱신 시 TTL 연장:
 * - Access Token 갱신 시 Refresh Token TTL도 연장
 * - 지속적인 서비스 이용 시 재로그인 방지
 * - 비활성 사용자는 자동 로그아웃
 */

// === 보안 고려사항 ===

/**
 * 1. 토큰 회전 (Token Rotation):
 * - 갱신 시 새로운 Refresh Token도 함께 발급
 * - 기존 Refresh Token 무효화
 * - 토큰 탈취 위험 최소화
 *
 * 구현 예시:
 * public void rotateRefreshToken(String username) {
 *     // 기존 토큰 삭제
 *     refreshTokenRepository.deleteById(username);
 *
 *     // 새 토큰 생성 및 저장
 *     String newToken = jwtUtil.generateToken(username, role, true);
 *     refreshTokenRepository.save(new RefreshToken(username, newToken));
 * }
 *
 * 2. 다중 기기 지원:
 * - 사용자별 여러 Refresh Token 허용
 * - 기기별 식별자 추가 (username + deviceId)
 * - 기기별 독립적인 세션 관리
 *
 * 3. 비정상 패턴 감지:
 * - 짧은 시간 내 반복적인 토큰 갱신
 * - 여러 IP에서 동시 토큰 사용
 * - 의심스러운 활동 모니터링
 */

// === 모니터링 및 관리 ===

/**
 * 1. Redis 모니터링:
 * - 메모리 사용량 추적
 * - 연결 수 모니터링
 * - TTL 만료 패턴 분석
 *
 * 2. 토큰 통계:
 * - 활성 토큰 수
 * - 토큰 갱신 빈도
 * - 평균 토큰 생명주기
 *
 * 3. 정리 작업:
 * - 만료된 토큰 자동 정리 (TTL로 자동 처리)
 * - 고아 토큰 정리 (사용자 삭제 시)
 * - 정기적인 Redis 최적화
 */

// === 대안 구현 방식 ===

/**
 * 1. Record 클래스 사용 (Java 14+):
 * @RedisHash(value = "refreshToken", timeToLive = 604800)
 * public record RefreshToken(
 *     @Id String username,
 *     String token
 * ) {}
 *
 * 장점: 불변성, 간결한 코드
 * 고려사항: Redis 직렬화 호환성 확인 필요
 *
 * 2. 복합 키 사용 (다중 기기 지원):
 * @RedisHash(value = "refreshToken", timeToLive = 604800)
 * public class RefreshToken {
 *     @Id
 *     private String id; // username:deviceId 형태
 *     private String username;
 *     private String deviceId;
 *     private String token;
 * }
 *
 * 3. 추가 메타데이터 포함:
 * @RedisHash(value = "refreshToken", timeToLive = 604800)
 * public class RefreshToken {
 *     @Id private String username;
 *     private String token;
 *     private String ipAddress;        // 발급 IP
 *     private String userAgent;        // 클라이언트 정보
 *     private LocalDateTime issuedAt;  // 발급 시간
 *     private LocalDateTime lastUsed;  // 마지막 사용 시간
 * }
 */