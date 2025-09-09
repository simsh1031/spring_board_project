package com.example.boardpjt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정 클래스
 * Spring Data JPA의 Auditing 기능을 활성화하여 엔티티의 생성일시, 수정일시 등을 자동으로 관리
 *
 * JPA Auditing이란?
 * - 엔티티의 생성 시간, 수정 시간, 생성자, 수정자 등을 자동으로 추적하고 기록하는 기능
 * - @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy 어노테이션 지원
 * - 별도의 코드 없이 Spring Data JPA가 자동으로 처리
 */
@Configuration // Spring의 설정 클래스임을 나타냄 - Spring 컨테이너가 이 클래스를 Bean으로 등록하고 설정 정보로 활용
@EnableJpaAuditing // JPA Auditing 기능 활성화 - 이 어노테이션이 없으면 Auditing 관련 어노테이션들이 동작하지 않음
public class JpaConfig {

    // === JPA Auditing 기능 설명 ===

    // 이 클래스는 현재 비어있지만, @EnableJpaAuditing 어노테이션만으로도 충분한 기능을 제공함
    // Spring Data JPA가 자동으로 Auditing 기능을 활성화하고 관련 리스너들을 등록함

    // === Auditing 관련 어노테이션들 ===

    // 1. @CreatedDate: 엔티티 생성 시간 자동 설정
    //    - INSERT 시 현재 시간이 자동으로 설정됨
    //    - LocalDateTime, Date, Long(timestamp) 등 시간 타입에 사용 가능
    //
    // 2. @LastModifiedDate: 엔티티 수정 시간 자동 설정
    //    - INSERT/UPDATE 시 현재 시간이 자동으로 설정됨
    //    - 생성 시에도 설정되고, 수정할 때마다 갱신됨
    //
    // 3. @CreatedBy: 엔티티 생성자 자동 설정
    //    - 현재 인증된 사용자의 정보가 자동으로 설정됨
    //    - AuditorAware 인터페이스 구현체가 필요함
    //
    // 4. @LastModifiedBy: 엔티티 수정자 자동 설정
    //    - 현재 인증된 사용자의 정보가 자동으로 설정됨
    //    - AuditorAware 인터페이스 구현체가 필요함

    // === 사용 예시 ===

    // UserAccount 엔티티에 Auditing 필드 추가 예시:
    /*
    @Entity
    @EntityListeners(AuditingEntityListener.class) // Auditing 리스너 등록 (필수)
    public class UserAccount {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true, length = 50)
        private String username;

        @Column(nullable = false)
        private String password;

        @Column(nullable = false, length = 20)
        private String role;

        // === Auditing 필드들 ===

        @CreatedDate // 생성 시간 자동 설정
        @Column(name = "created_at", nullable = false, updatable = false)
        // updatable = false: UPDATE 시 이 컬럼은 변경되지 않음
        private LocalDateTime createdAt;

        @LastModifiedDate // 수정 시간 자동 설정
        @Column(name = "updated_at", nullable = false)
        private LocalDateTime updatedAt;

        @CreatedBy // 생성자 자동 설정 (옵션, AuditorAware 구현 필요)
        @Column(name = "created_by", length = 50, updatable = false)
        private String createdBy;

        @LastModifiedBy // 수정자 자동 설정 (옵션, AuditorAware 구현 필요)
        @Column(name = "updated_by", length = 50)
        private String updatedBy;
    }
    */

    // === Auditing 동작 시점 ===

    // 1. @PrePersist: 엔티티가 처음 저장되기 전
    //    - @CreatedDate, @CreatedBy 설정
    //    - @LastModifiedDate, @LastModifiedBy도 함께 설정됨
    //
    // 2. @PreUpdate: 엔티티가 수정되기 전
    //    - @LastModifiedDate, @LastModifiedBy만 갱신
    //    - @CreatedDate, @CreatedBy는 변경되지 않음

    // === 장점 ===

    // 1. 자동화: 개발자가 수동으로 시간을 설정할 필요 없음
    // 2. 일관성: 모든 엔티티에 동일한 방식으로 시간 정보 관리
    // 3. 실수 방지: 시간 설정을 깜빡하는 실수 방지
    // 4. 추적성: 데이터 변경 이력 추적 가능
    // 5. 감사(Audit): 규정 준수나 보안 감사에 활용

    // === 추가 설정 옵션 ===

    // 시간대 설정이 필요한 경우:
    /*
    @EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now(ZoneId.of("Asia/Seoul")));
    }
    */

    // 특정 조건에서만 Auditing 비활성화:
    /*
    @EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware",
                       setDates = true,    // 날짜 자동 설정 여부
                       modifyOnCreate = true) // 생성 시에도 수정 날짜 설정 여부
    */
}

// === 실제 사용 시나리오 ===

// 1. 사용자 계정 관리:
//    - 언제 가입했는지 (createdAt)
//    - 마지막 정보 수정은 언제인지 (updatedAt)
//
// 2. 게시글 관리:
//    - 언제 작성되었는지 (createdAt)
//    - 마지막 수정은 언제인지 (updatedAt)
//    - 누가 작성했는지 (createdBy)
//    - 누가 마지막으로 수정했는지 (updatedBy)
//
// 3. 감사 로그:
//    - 데이터 변경 이력 추적
//    - 규정 준수 요구사항 충족
//    - 보안 사고 발생 시 추적