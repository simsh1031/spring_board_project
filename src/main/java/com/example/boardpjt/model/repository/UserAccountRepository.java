package com.example.boardpjt.model.repository;

import com.example.boardpjt.model.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UserAccount 엔티티에 대한 데이터 접근 계층(Repository)
 * Spring Data JPA를 사용하여 사용자 계정 관련 데이터베이스 CRUD 작업을 처리
 * JpaRepository를 상속받아 기본적인 데이터베이스 조작 메서드들을 자동으로 제공받음
 *
 * 핵심 기능:
 * - 사용자 인증 및 권한 관리
 * - 사용자 검색 및 조회 기능
 * - 관리자 기능을 위한 사용자 목록 관리
 * - 통계 및 모니터링 지원
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    // JpaRepository<엔티티 클래스, Primary Key 타입>
    // - UserAccount: 관리할 엔티티 클래스
    // - Long: Primary Key(id 필드)의 데이터 타입

    // === JpaRepository에서 자동으로 제공되는 기본 메서드들 ===
    // save(UserAccount entity) - 엔티티 저장/수정
    // findById(Long id) - ID로 엔티티 조회
    // findAll() - 모든 엔티티 조회 (관리자 기능용)
    // deleteById(Long id) - ID로 엔티티 삭제 (회원 탈퇴용)
    // delete(UserAccount entity) - 엔티티 삭제
    // count() - 전체 사용자 수 조회 (통계용)
    // existsById(Long id) - ID 존재 여부 확인
    // 등등...

    /**
     * 사용자명으로 사용자 계정을 조회하는 메서드
     * JWT 인증 시스템에서 가장 자주 사용되는 핵심 메서드
     * Spring Data JPA의 Query Method 기능을 활용한 자동 쿼리 생성
     *
     * 사용 시나리오:
     * - 로그인 시 사용자 존재 여부 확인
     * - JWT 토큰에서 추출한 사용자명으로 사용자 정보 로드
     * - 회원가입 시 중복 사용자명 검사
     * - CustomUserDetailsService에서 인증 정보 로드
     *
     * @param username 조회할 사용자명 (unique 제약조건 적용)
     * @return Optional<UserAccount> - 조회된 사용자 계정 (존재하지 않을 수 있음)
     */
    Optional<UserAccount> findByUsername(String username);

    // === Spring Data JPA Query Method 작동 원리 ===
    // 메서드명 패턴: find + By + 엔티티필드명
    // - "findBy": 조회 작업임을 나타냄
    // - "Username": UserAccount 엔티티의 username 필드를 의미
    //
    // 자동 생성되는 SQL 쿼리:
    // SELECT * FROM user_account WHERE username = ?

    // === Optional 사용 이유 ===
    // - 사용자명이 존재하지 않을 가능성이 있음
    // - NullPointerException 방지
    // - 명시적으로 "값이 없을 수 있음"을 표현
    // - Optional.isPresent(), Optional.orElse() 등으로 안전한 처리 가능

    // === 실제 서비스에서 필요한 추가 Query Method들 ===

    /**
     * 사용자명 존재 여부 확인
     * 회원가입 시 중복 체크에 사용하며, existsBy 메서드는 count 쿼리를 사용하여 성능상 유리
     *
     * @param username 확인할 사용자명
     * @return boolean - 존재하면 true, 없으면 false
     */
    // boolean existsByUsername(String username);

    /**
     * 역할별 사용자 조회
     * 관리자 기능에서 역할별 사용자 목록을 조회할 때 사용
     *
     * @param role 조회할 역할 (ROLE_USER, ROLE_ADMIN 등)
     * @return List<UserAccount> - 해당 역할을 가진 사용자 목록
     */
    // List<UserAccount> findByRole(String role);

    /**
     * 역할별 사용자 수 조회
     * 통계 및 대시보드에서 사용자 분포 현황 파악
     *
     * @param role 조회할 역할
     * @return long - 해당 역할을 가진 사용자 수
     */
    // long countByRole(String role);

    /**
     * 사용자명으로 부분 검색
     * 관리자 페이지에서 사용자 검색 기능
     *
     * @param keyword 검색 키워드
     * @return List<UserAccount> - 사용자명에 키워드가 포함된 사용자 목록
     */
    // List<UserAccount> findByUsernameContainingIgnoreCase(String keyword);

    /**
     * 특정 날짜 이후 가입한 사용자 조회
     * 신규 가입자 분석 및 마케팅 데이터 활용
     *
     * @param date 기준 날짜
     * @return List<UserAccount> - 해당 날짜 이후 가입한 사용자 목록
     */
    // List<UserAccount> findByCreatedAtAfter(LocalDateTime date);

    /**
     * 최근 가입한 사용자 N명 조회
     * 관리자 대시보드의 최신 가입자 현황
     *
     * @return List<UserAccount> - 최근 가입한 10명의 사용자
     */
    // List<UserAccount> findTop10ByOrderByCreatedAtDesc();

    // === @Query 어노테이션을 사용한 커스텀 쿼리 예시 ===

    /**
     * 사용자별 게시물 수와 함께 조회
     * 관리자 기능에서 활동도가 높은 사용자 파악
     *
     * @return List<Object[]> - [UserAccount, postCount] 형태의 결과
     */
    // @Query("SELECT u, COUNT(p) FROM UserAccount u LEFT JOIN Post p ON u = p.author GROUP BY u ORDER BY COUNT(p) DESC")
    // List<Object[]> findUsersWithPostCount();

    /**
     * 특정 기간 동안 활동한 사용자 조회
     * 활성 사용자 분석 및 이벤트 대상자 선정
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return List<UserAccount> - 해당 기간 동안 활동한 사용자 목록
     */
    // @Query("SELECT DISTINCT u FROM UserAccount u JOIN Post p ON u = p.author WHERE p.createdAt BETWEEN :startDate AND :endDate")
    // List<UserAccount> findActiveUsersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 사용자명과 역할로 복합 검색
     * 관리자 페이지의 고급 검색 기능
     *
     * @param username 사용자명 (선택적)
     * @param role 역할 (선택적)
     * @return Optional<UserAccount> - 조건에 맞는 사용자
     */
    // Optional<UserAccount> findByUsernameAndRole(String username, String role);

    // === Query Method 명명 규칙 및 연산자 ===

    /**
     * findBy: 조회 (SELECT)
     * - findByUsername: username = ?
     * - findByUsernameAndRole: username = ? AND role = ?
     * - findByUsernameOrRole: username = ? OR role = ?
     *
     * 문자열 검색:
     * - findByUsernameContaining: username LIKE '%?%'
     * - findByUsernameStartingWith: username LIKE '?%'
     * - findByUsernameEndingWith: username LIKE '%?'
     * - findByUsernameIgnoreCase: UPPER(username) = UPPER(?)
     *
     * 비교 연산:
     * - findByIdGreaterThan: id > ?
     * - findByIdLessThan: id < ?
     * - findByIdBetween: id BETWEEN ? AND ?
     * - findByCreatedAtAfter: created_at > ?
     * - findByCreatedAtBefore: created_at < ?
     *
     * NULL 체크:
     * - findByEmailIsNull: email IS NULL
     * - findByEmailIsNotNull: email IS NOT NULL
     *
     * 정렬:
     * - findByRoleOrderByUsernameAsc: ORDER BY username ASC
     * - findByRoleOrderByCreatedAtDesc: ORDER BY created_at DESC
     *
     * 개수 제한:
     * - findTop10ByRole: LIMIT 10
     * - findFirst5ByRole: LIMIT 5
     *
     * 존재 여부:
     * - existsByUsername: SELECT COUNT(*) > 0
     *
     * 개수 조회:
     * - countByRole: SELECT COUNT(*)
     *
     * 삭제:
     * - deleteByUsername: DELETE FROM user_account WHERE username = ?
     */

    // === 성능 최적화 고려사항 ===

    /**
     * 1. 인덱스 활용:
     * - username 컬럼: UNIQUE 인덱스 (자동 생성)
     * - role 컬럼: 일반 인덱스 (역할별 조회 시)
     * - created_at 컬럼: 날짜 기반 조회 시
     *
     * 2. 페이징 처리:
     * - 대량 사용자 데이터에 대한 페이징 적용
     * - Pageable 파라미터 활용
     *
     * 예시:
     * Page<UserAccount> findByRole(String role, Pageable pageable);
     *
     * 3. Projection 활용:
     * - 필요한 컬럼만 조회하여 성능 향상
     *
     * interface UserSummary {
     *     Long getId();
     *     String getUsername();
     *     String getRole();
     * }
     *
     * List<UserSummary> findProjectedByRole(String role);
     */

    // === 보안 고려사항 ===

    /**
     * 1. 민감 정보 처리:
     * - 비밀번호는 절대 평문으로 조회하지 않음
     * - 필요한 경우 DTO나 Projection 활용
     *
     * 2. 권한 기반 접근:
     * - 일반 사용자는 자신의 정보만 조회
     * - 관리자만 전체 사용자 목록 접근
     *
     * 3. SQL Injection 방지:
     * - JPA Query Method는 자동으로 파라미터 바인딩 처리
     * - @Query 사용 시에도 :parameter 형태로 안전하게 처리
     */

    // === AdminController에서 사용할 메서드들 ===

    /**
     * 관리자 기능을 위한 추가 메서드들
     * AdminController에서 회원 관리 기능 구현 시 필요
     */

    /**
     * 모든 사용자 조회 (관리자 전용)
     * AdminController.adminPage()에서 사용
     *
     * @return List<UserAccount> - 모든 사용자 목록
     */
    // List<UserAccount> findAllByOrderByCreatedAtDesc();

    /**
     * 사용자 ID로 조회 (관리자 기능)
     * 사용자 상세 정보 조회 및 수정 시 사용
     *
     * @param id 사용자 ID
     * @return Optional<UserAccount> - 해당 ID의 사용자
     */
    // 기본 findById(Long id) 사용

    /**
     * 사용자 역할 변경을 위한 조회
     * 역할 변경 기능 구현 시 사용
     *
     * @param id 사용자 ID
     * @return Optional<UserAccount> - 역할 변경 대상 사용자
     */
    // 기본 findById(Long id) 사용 후 setRole() 호출
}