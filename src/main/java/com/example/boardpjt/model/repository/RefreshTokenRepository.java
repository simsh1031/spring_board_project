package com.example.boardpjt.model.repository;

import com.example.boardpjt.model.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

/**
 * RefreshToken 엔티티에 대한 Redis 기반 데이터 접근 계층(Repository) 인터페이스
 * Spring Data Redis를 사용하여 Refresh Token 관련 데이터베이스 작업을 처리
 * CrudRepository를 상속받아 기본적인 CRUD 메서드들을 자동으로 제공받음
 *
 * JpaRepository 대신 CrudRepository 사용 이유:
 * - Redis는 관계형 데이터베이스가 아니므로 JPA 기능이 불필요
 * - 기본적인 CRUD 작업만 필요하므로 CrudRepository가 적합
 * - 더 가벼운 인터페이스로 성능상 이점
 *
 * Redis 기반 Repository의 특징:
 * - TTL(Time To Live) 자동 관리 지원
 * - 메모리 기반 빠른 조회 성능
 * - 키-값 구조의 단순한 데이터 모델
 * - 분산 환경에서의 세션 공유 가능
 */
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    // CrudRepository<엔티티 클래스, Primary Key 타입>
    // - RefreshToken: 관리할 엔티티 클래스
    // - String: Primary Key(username 필드)의 데이터 타입

    // === CrudRepository에서 자동으로 제공되는 기본 메서드들 ===

    /**
     * Refresh Token 저장 또는 업데이트
     * Redis에 "refreshToken:{username}" 키로 토큰 정보 저장
     *
     * 동작 방식:
     * - 새로운 username인 경우: 새 토큰 생성
     * - 기존 username인 경우: 기존 토큰 덮어쓰기 (로그인 시)
     * - TTL은 @RedisHash 설정에 따라 자동 적용
     *
     * @param entity 저장할 RefreshToken 엔티티
     * @return 저장된 RefreshToken 엔티티
     */
    // <S extends RefreshToken> S save(S entity);

    /**
     * 사용자명으로 Refresh Token 조회
     * Redis에서 "refreshToken:{username}" 키로 토큰 검색
     *
     * 사용 시나리오:
     * - Access Token 갱신 시 서버 저장 토큰과 클라이언트 토큰 비교
     * - 로그아웃 시 기존 토큰 존재 여부 확인
     *
     * @param username 조회할 사용자명 (Primary Key)
     * @return Optional<RefreshToken> - 토큰이 존재하지 않을 수 있음
     */
    // Optional<RefreshToken> findById(String username);

    /**
     * Refresh Token 존재 여부 확인
     * 특정 사용자의 활성 토큰이 있는지 빠르게 확인
     *
     * @param username 확인할 사용자명
     * @return boolean - 토큰 존재 시 true, 없으면 false
     */
    // boolean existsById(String username);

    /**
     * 사용자명으로 Refresh Token 삭제
     * 로그아웃 시 서버에서 토큰 무효화할 때 사용
     *
     * 보안 중요성:
     * - 클라이언트 쿠키 삭제와 함께 서버 토큰도 제거
     * - 토큰 탈취 시에도 서버 검증에서 차단됨
     *
     * @param username 삭제할 사용자명
     */
    // void deleteById(String username);

    /**
     * 모든 Refresh Token 조회
     * 관리자 기능이나 통계 목적으로 사용
     *
     * 주의사항:
     * - 대용량 데이터의 경우 성능에 영향
     * - 일반적으로 실제 서비스에서는 사용하지 않음
     *
     * @return Iterable<RefreshToken> - 모든 토큰 목록
     */
    // Iterable<RefreshToken> findAll();

    /**
     * 전체 Refresh Token 수 조회
     * 활성 사용자 수 통계나 모니터링 목적
     *
     * @return long - 현재 활성 토큰 수
     */
    // long count();

    /**
     * 모든 Refresh Token 삭제
     * 시스템 초기화나 전체 로그아웃 기능
     *
     * 위험성:
     * - 모든 사용자가 강제 로그아웃됨
     * - 운영 환경에서는 신중하게 사용
     */
    // void deleteAll();

    // === Redis 기반 Repository 특별 고려사항 ===

    /**
     * TTL(Time To Live) 자동 관리:
     * - @RedisHash(timeToLive = 604800)에 의해 7일 후 자동 삭제
     * - 별도의 만료 토큰 정리 작업 불필요
     * - Redis의 EXPIRE 명령어 자동 적용
     */

    /**
     * 원자적 연산 보장:
     * - Redis의 단일 스레드 특성으로 동시성 문제 해결
     * - 토큰 저장/삭제가 원자적으로 수행됨
     * - 경쟁 상태(Race Condition) 방지
     */

    /**
     * 메모리 기반 성능:
     * - 디스크 I/O 없이 메모리에서 직접 처리
     * - 마이크로초 단위의 빠른 응답시간
     * - 대용량 동시 접속에도 안정적인 성능
     */

    // === 확장 가능한 커스텀 메서드들 (구현 예정) ===

    /**
     * 특정 시간 이후 생성된 토큰들 조회 (구현 시 고려사항)
     * Redis에서는 기본적으로 이런 쿼리가 어려우므로 추가 필드 필요
     */
    // List<RefreshToken> findByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 다중 기기 지원을 위한 사용자별 모든 토큰 삭제
     * 사용자명 패턴으로 여러 토큰 삭제 (username:deviceId 구조 시)
     */
    // void deleteAllByUsernameStartingWith(String usernamePrefix);

    /**
     * 배치 작업을 위한 만료 임박 토큰 조회
     * Redis TTL 명령어를 활용한 커스텀 구현 필요
     */
    // List<RefreshToken> findTokensExpiringWithin(Duration duration);

    // === 실제 사용 패턴 예시 ===

    /**
     * 로그인 시 토큰 저장:
     *
     * RefreshToken refreshToken = new RefreshToken(username, jwtToken);
     * refreshTokenRepository.save(refreshToken);
     *
     * 특징:
     * - 기존 토큰이 있으면 자동으로 덮어쓰기
     * - TTL이 자동으로 리셋되어 7일 연장됨
     */

    /**
     * 토큰 갱신 시 검증:
     *
     * Optional<RefreshToken> stored = refreshTokenRepository.findById(username);
     * if (stored.isPresent() && clientToken.equals(stored.get().getToken())) {
     *     // 토큰 일치 - 새 Access Token 발급
     * } else {
     *     // 토큰 불일치 또는 없음 - 재로그인 필요
     * }
     */

    /**
     * 로그아웃 시 토큰 무효화:
     *
     * refreshTokenRepository.deleteById(username);
     *
     * 중요성:
     * - 클라이언트 쿠키 삭제만으로는 불충분
     * - 서버에서도 토큰을 제거해야 완전한 로그아웃
     */

    // === 모니터링 및 관리 ===

    /**
     * 활성 사용자 수 확인:
     * long activeUsers = refreshTokenRepository.count();
     *
     * 토큰 존재 여부 확인:
     * boolean hasActiveSession = refreshTokenRepository.existsById(username);
     *
     * 전체 세션 종료 (긴급 상황 시):
     * refreshTokenRepository.deleteAll(); // 주의: 모든 사용자 로그아웃
     */

    // === Redis 연결 및 설정 고려사항 ===

    /**
     * Redis 연결 실패 시 처리:
     * - 토큰 저장/조회 실패 시 fallback 전략 필요
     * - 일시적 장애 시 사용자 경험 저하 최소화
     * - 헬스 체크 및 모니터링 시스템 구축
     *
     * 성능 최적화:
     * - Redis 연결 풀 적절히 설정
     * - 파이프라이닝 활용으로 네트워크 지연 최소화
     * - 적절한 직렬화 방식 선택 (JSON, Binary 등)
     *
     * 보안 강화:
     * - Redis AUTH 설정
     * - 네트워크 구간 암호화 (TLS)
     * - 접근 가능한 IP 제한
     */
}