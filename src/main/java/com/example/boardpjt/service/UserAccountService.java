package com.example.boardpjt.service;

import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 계정 관련 비즈니스 로직을 처리하는 서비스 클래스 (관리자 기능 확장 버전)
 * 회원가입, 사용자 정보 관리, 관리자 전용 기능 등의 핵심 기능을 담당
 * 데이터베이스 트랜잭션과 비밀번호 암호화를 포함한 안전한 사용자 관리 시스템
 *
 * 주요 기능:
 * - 사용자 회원가입 처리
 * - 관리자용 전체 사용자 조회
 * - 관리자용 사용자 강제 탈퇴 처리
 * - 비밀번호 암호화 및 보안 관리
 */
@Service // Spring의 서비스 빈으로 등록 (비즈니스 로직을 담당하는 @Component의 특화 버전)
@RequiredArgsConstructor // final로 선언된 필드들에 대한 생성자 자동 생성 (의존성 주입용)
// 생성자 주입 방식: 필드 주입(@Autowired)보다 권장되는 방식
// - 불변성 보장 (final 키워드 사용 가능)
// - 순환 참조 방지
// - 테스트 용이성 향상
public class UserAccountService {

    // 사용자 계정 데이터를 데이터베이스에서 조회/저장하기 위한 Repository
    private final UserAccountRepository userAccountRepository;

    // 비밀번호 암호화를 위한 Spring Security의 PasswordEncoder
    // SecurityConfig에서 BCrypt 기반의 DelegatingPasswordEncoder로 설정됨
    private final PasswordEncoder passwordEncoder;

    /**
     * 새로운 사용자를 등록하는 메서드
     * 사용자명 중복 검사, 비밀번호 암호화, 기본 권한 설정을 포함한 완전한 회원가입 처리
     *
     * @param username 등록할 사용자명 (중복 불가)
     * @param password 사용자가 입력한 평문 비밀번호 (암호화되어 저장됨)
     * @return UserAccount 생성된 사용자 계정 엔티티 (id 포함)
     * @throws IllegalArgumentException 이미 존재하는 사용자명일 경우 발생
     */
    @Transactional // 데이터베이스 트랜잭션 관리 - 메서드 전체가 하나의 트랜잭션으로 처리됨
    // @Transactional의 역할:
    // - 메서드 실행 중 예외 발생 시 모든 변경사항 롤백
    // - 메서드 정상 완료 시 모든 변경사항 커밋
    // - 데이터 일관성 보장
    // - 기본 전파 수준: REQUIRED (기존 트랜잭션이 있으면 참여, 없으면 새로 생성)
    public UserAccount register(String username, String password) {

        // === 1단계: 사용자명 중복 검사 ===
        // findByUsername()은 Optional<UserAccount>를 반환
        // isPresent(): Optional에 값이 있는지 확인 (즉, 해당 사용자명이 이미 존재하는지 검사)
        if (userAccountRepository.findByUsername(username).isPresent()) {
            // 중복된 사용자명이 존재할 경우 예외 발생
            // 이 예외는 Controller에서 catch되어 사용자에게 에러 메시지로 표시됨
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username);
        }

        // === 2단계: 새로운 UserAccount 엔티티 생성 및 설정 ===
        UserAccount userAccount = new UserAccount();

        // 사용자명 설정 (입력받은 값 그대로 저장)
        userAccount.setUsername(username);

        // === 비밀번호 암호화 및 설정 ===
        // passwordEncoder.encode(): 평문 비밀번호를 암호화
        // - BCrypt 알고리즘 사용 (기본 설정)
        // - Salt 자동 생성으로 같은 비밀번호라도 다른 해시값 생성
        // - 단방향 암호화로 복호화 불가능 (보안성 확보)
        // 예: "password123" → "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKWKQp1bR..."
        userAccount.setPassword(passwordEncoder.encode(password));

        // === 기본 권한 설정 ===
        // 새로 가입하는 모든 사용자에게 "ROLE_USER" 권한 부여
        // Spring Security에서 권한은 "ROLE_" 접두사를 사용하는 것이 관례
        // - ROLE_USER: 일반 사용자 권한
        // - ROLE_ADMIN: 관리자 권한 (별도 로직으로 부여)
        userAccount.setRole("ROLE_USER");

        // === 3단계: 데이터베이스에 저장 ===
        // save() 메서드는 JPA의 persist 작동 방식:
        // - id가 null인 경우: INSERT 쿼리 실행 (새로운 엔티티 생성)
        // - id가 존재하는 경우: UPDATE 쿼리 실행 (기존 엔티티 수정)
        // 저장 후 자동 생성된 id가 포함된 UserAccount 객체 반환
        return userAccountRepository.save(userAccount);

        // === 트랜잭션 커밋 ===
        // 메서드가 정상적으로 종료되면 @Transactional에 의해 자동 커밋
        // 예외 발생 시 자동 롤백되어 데이터 일관성 보장
    }

    /**
     * 전체 사용자 목록을 조회하는 메서드 (관리자 전용 기능)
     * AdminController에서 관리자 페이지의 사용자 목록 표시를 위해 사용
     *
     * 보안 고려사항:
     * - 이 메서드는 관리자 권한(ROLE_ADMIN)을 가진 사용자만 호출해야 함
     * - SecurityConfig에서 /admin/** 경로를 hasRole("ADMIN")으로 보호
     * - 컨트롤러 레벨에서 권한 검증이 이미 완료된 상태에서 호출됨
     *
     * 성능 고려사항:
     * - 대량의 사용자 데이터가 있는 경우 페이징 처리 고려 필요
     * - 민감한 정보(비밀번호)는 이미 암호화되어 있어 안전
     * - 필요시 DTO 변환으로 필요한 정보만 전달 고려
     *
     * @return List<UserAccount> - 모든 사용자 계정 목록 (생성일 기준 정렬 권장)
     */
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션 설정
    // readOnly = true의 장점:
    // - 성능 최적화: 하이버네이트가 변경 감지(dirty checking)를 수행하지 않음
    // - 메모리 절약: 스냅샷을 생성하지 않아 메모리 사용량 감소
    // - 데이터베이스 최적화: 읽기 전용으로 처리되어 DB 레벨에서 최적화 가능
    // - 안전성: 실수로 데이터를 변경하는 것을 방지
    public List<UserAccount> findAllUsers() {
        // === 전체 사용자 조회 ===
        // findAll(): JpaRepository에서 제공하는 기본 메서드
        // SELECT * FROM user_account 쿼리 실행
        return userAccountRepository.findAll();

        // === 개선 고려사항 ===
        // 1. 정렬 추가:
        // return userAccountRepository.findAllByOrderByCreatedAtDesc();
        //
        // 2. 페이징 처리:
        // public Page<UserAccount> findAllUsers(Pageable pageable) {
        //     return userAccountRepository.findAll(pageable);
        // }
        //
        // 3. DTO 변환:
        // return userAccountRepository.findAll().stream()
        //     .map(user -> new UserAccountDTO(user.getId(), user.getUsername(), user.getRole()))
        //     .collect(Collectors.toList());
    }

    /**
     * 사용자를 강제 탈퇴(삭제)시키는 메서드 (관리자 전용 기능)
     * AdminController에서 관리자가 문제 사용자를 시스템에서 제거할 때 사용
     *
     * 중요한 보안 및 비즈니스 로직:
     * - 관리자만 실행 가능한 매우 민감한 작업
     * - 되돌릴 수 없는 작업이므로 신중한 사용 필요
     * - 관련 데이터 정리도 함께 고려해야 함
     *
     * 삭제 시 처리해야 할 연관 데이터:
     * 1. 해당 사용자가 작성한 게시물 (Post 엔티티)
     * 2. 해당 사용자의 Refresh Token (RefreshToken 엔티티)
     * 3. 향후 댓글, 좋아요 등 추가 기능 시 관련 데이터
     *
     * @param id 삭제할 사용자의 ID (Primary Key)
     * @throws EntityNotFoundException 존재하지 않는 사용자 ID인 경우 (JPA에서 자동 발생)
     */
    @Transactional // 쓰기 트랜잭션으로 데이터 일관성 보장
    // 삭제 작업은 여러 테이블에 영향을 줄 수 있으므로 트랜잭션 필수
    public void deleteUser(Long id) {
        // === 사용자 삭제 실행 ===
        // deleteById(): JpaRepository에서 제공하는 기본 메서드
        // 1. 먼저 해당 ID의 엔티티 존재 여부 확인 (SELECT 쿼리)
        // 2. 존재하면 DELETE 쿼리 실행
        // 3. 존재하지 않으면 EmptyResultDataAccessException 발생
        userAccountRepository.deleteById(id);

        // === 추가 정리 작업 고려사항 ===
        // 현재는 단순 삭제만 수행하지만, 실제 운영에서는 다음과 같은 추가 작업 필요:

        // 1. 연관 데이터 정리 (Cascade 설정이 없는 경우):
        // postRepository.deleteByAuthorId(id);
        // refreshTokenRepository.deleteById(username);

        // 2. 소프트 삭제 방식 고려:
        // user.setDeleted(true);
        // user.setDeletedAt(LocalDateTime.now());
        // userAccountRepository.save(user);

        // 3. 삭제 로그 기록 (감사 추적):
        // auditLogService.logUserDeletion(id, getCurrentAdminUsername());

        // 4. 캐시 무효화 (캐싱 사용 시):
        // cacheManager.evict("users", id);

        // === 트랜잭션 커밋 ===
        // 메서드가 정상 완료되면 모든 변경사항이 데이터베이스에 커밋됨
        // 예외 발생 시 모든 변경사항이 롤백되어 데이터 일관성 보장
    }

    public UserAccount findByUsername(String name) {
        return userAccountRepository.findByUsername(name)
                .orElseThrow();
    }

    // === 추가 구현 고려사항 ===

    /**
     * 사용자 ID로 단일 사용자 조회 (관리자 기능)
     * 사용자 상세 정보 조회나 수정 전 조회에 사용
     */
    /*
    @Transactional(readOnly = true)
    public UserAccount findUserById(Long id) {
        return userAccountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + id));
    }
    */

    /**
     * 사용자 역할 변경 메서드 (관리자 기능)
     * 일반 사용자를 관리자로 승격하거나 권한을 변경할 때 사용
     */
    /*
    @Transactional
    public void changeUserRole(Long id, String newRole) {
        UserAccount user = findUserById(id);

        // 역할 유효성 검증
        if (!newRole.equals("ROLE_USER") && !newRole.equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("유효하지 않은 역할입니다: " + newRole);
        }

        user.setRole(newRole);
        userAccountRepository.save(user);

        // 역할 변경 로그 기록
        // auditLogService.logRoleChange(id, user.getRole(), newRole);
    }
    */

    /**
     * 사용자 계정 활성화/비활성화 토글 (관리자 기능)
     * 문제 사용자를 일시적으로 정지시킬 때 사용
     */
    /*
    @Transactional
    public boolean toggleUserStatus(Long id) {
        UserAccount user = findUserById(id);
        boolean newStatus = !user.isEnabled();
        user.setEnabled(newStatus);
        userAccountRepository.save(user);
        return newStatus;
    }
    */

    /**
     * 비밀번호 정책 검증 메서드
     * 회원가입이나 비밀번호 변경 시 보안 정책 적용
     */
    /*
    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("비밀번호는 대문자를 포함해야 합니다");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("비밀번호는 소문자를 포함해야 합니다");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("비밀번호는 숫자를 포함해야 합니다");
        }
    }
    */

    /**
     * 사용자 통계 정보 조회 (관리자 대시보드용)
     * 전체 사용자 수, 역할별 분포, 최근 가입자 수 등
     */
    /*
    @Transactional(readOnly = true)
    public UserStatisticsDTO getUserStatistics() {
        long totalUsers = userAccountRepository.count();
        long adminCount = userAccountRepository.countByRole("ROLE_ADMIN");
        long userCount = userAccountRepository.countByRole("ROLE_USER");

        return new UserStatisticsDTO(totalUsers, adminCount, userCount);
    }
    */
}

// === 서비스 클래스 설계 원칙 ===

/**
 * 1. 단일 책임 원칙 (SRP):
 * - UserAccountService는 사용자 계정 관련 비즈니스 로직만 담당
 * - 인증/인가는 Spring Security가 처리
 * - 데이터 접근은 Repository가 담당
 *
 * 2. 트랜잭션 관리:
 * - 읽기 전용 작업: @Transactional(readOnly = true)
 * - 쓰기 작업: @Transactional
 * - 긴 작업이나 외부 API 호출 시 트랜잭션 분리 고려
 *
 * 3. 예외 처리:
 * - 비즈니스 로직 위반 시 명확한 예외 메시지
 * - Controller에서 예외를 catch하여 사용자 친화적 메시지 제공
 * - 예상 가능한 예외와 예상 불가능한 예외 구분 처리
 *
 * 4. 보안 고려사항:
 * - 민감한 정보(비밀번호) 암호화 처리
 * - 관리자 기능은 컨트롤러 레벨에서 권한 검증
 * - SQL Injection 방지 (JPA 사용으로 기본 보장)
 *
 * 5. 성능 최적화:
 * - 대량 데이터 처리 시 페이징 적용
 * - 읽기 전용 트랜잭션으로 성능 향상
 * - 필요시 캐싱 전략 적용
 */