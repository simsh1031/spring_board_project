package com.example.boardpjt.model.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티의 공통 감사(Audit) 필드를 제공하는 추상 기본 클래스
 *
 * 이 클래스는 데이터베이스의 모든 테이블에 공통으로 필요한 생성일시와 수정일시 필드를 정의하며,
 * Spring Data JPA의 Auditing 기능을 활용하여 자동으로 시간 정보를 관리합니다.
 *
 * 상속받는 엔티티들:
 * - UserAccount: 사용자 계정 정보
 * - Post: 게시물 정보
 * - Comment: 댓글 정보 (향후 추가 시)
 * - 기타 모든 도메인 엔티티
 *
 * 핵심 기능:
 * - 엔티티 생성 시 자동으로 createdAt 필드 설정
 * - 엔티티 수정 시 자동으로 updatedAt 필드 갱신
 * - 코드 중복 제거 및 일관된 감사 필드 제공
 */
@Getter // Lombok: createdAt, updatedAt 필드에 대한 getter 메서드 자동 생성
@MappedSuperclass // JPA: 이 클래스가 엔티티의 상위 클래스임을 표시, 별도 테이블 생성 안 함
@EntityListeners(AuditingEntityListener.class) // Spring Data JPA Auditing 리스너 등록 (필수)
public abstract class BaseEntity {

    /**
     * 엔티티 생성일시
     *
     * 동작 방식:
     * - 엔티티가 처음 데이터베이스에 저장될 때 (INSERT) 자동으로 현재 시간 설정
     * - 이후 수정 시에는 변경되지 않음 (불변 값)
     * - JpaConfig의 @EnableJpaAuditing과 연동하여 동작
     *
     * 데이터베이스 컬럼:
     * - 컬럼명: created_at (스네이크 케이스로 자동 변환)
     * - 타입: DATETIME 또는 TIMESTAMP (DB에 따라)
     * - NULL 허용: false (자동으로 값이 설정되므로)
     *
     * 활용 예시:
     * - 회원가입 시점 추적
     * - 게시물 작성 시점 기록
     * - 데이터 생성 이력 관리
     */
    @CreatedDate // Spring Data JPA: 엔티티 생성 시 자동으로 현재 시간 설정
    private LocalDateTime createdAt;

    /**
     * 엔티티 최종 수정일시
     *
     * 동작 방식:
     * - 엔티티가 처음 저장될 때 (INSERT) 생성일시와 동일한 값으로 설정
     * - 엔티티가 수정될 때마다 (UPDATE) 현재 시간으로 자동 갱신
     * - 실제 필드 값이 변경된 경우에만 갱신됨
     *
     * 데이터베이스 컬럼:
     * - 컬럼명: updated_at (스네이크 케이스로 자동 변환)
     * - 타입: DATETIME 또는 TIMESTAMP (DB에 따라)
     * - NULL 허용: false (자동으로 값이 설정되므로)
     *
     * 활용 예시:
     * - 사용자 정보 변경 시점 추적
     * - 게시물 수정 시점 기록
     * - 데이터 무결성 검증 및 동시성 제어
     */
    @LastModifiedDate // Spring Data JPA: 엔티티 생성/수정 시 자동으로 현재 시간 설정
    private LocalDateTime updatedAt;

    // === BaseEntity 설계 원칙 ===

    /**
     * 1. 추상 클래스 사용 이유:
     * - BaseEntity 자체로는 인스턴스 생성이 불필요함
     * - 모든 하위 엔티티에서 공통 기능을 강제로 상속받도록 보장
     * - 추후 공통 메서드 추가 시 모든 엔티티에 일괄 적용 가능
     *
     * 2. @MappedSuperclass 사용:
     * - BaseEntity에 대한 별도 테이블이 생성되지 않음
     * - 하위 엔티티 테이블에 createdAt, updatedAt 컬럼이 포함됨
     * - 상속받는 엔티티별로 독립적인 테이블 구조 유지
     *
     * 3. Setter 제공하지 않음:
     * - 생성일시와 수정일시는 시스템에서 자동 관리
     * - 개발자가 임의로 수정할 수 없도록 보장
     * - 데이터 무결성 및 감사 추적 신뢰성 확보
     */

    // === 확장 가능한 감사 필드들 ===

    /**
     * 향후 추가 고려 가능한 감사 필드들:
     *
     * @CreatedBy
     * private String createdBy;    // 생성자 정보
     *
     * @LastModifiedBy
     * private String modifiedBy;   // 수정자 정보
     *
     * private String version;      // 엔티티 버전 (낙관적 락)
     * private boolean deleted;     // 소프트 삭제 여부
     * private LocalDateTime deletedAt; // 삭제 일시
     *
     * 추가 시 JpaConfig에서 AuditorAware 빈 등록 필요:
     * @Bean
     * public AuditorAware<String> auditorProvider() {
     *     return () -> {
     *         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
     *         return Optional.of(auth != null ? auth.getName() : "SYSTEM");
     *     };
     * }
     */
}

// === BaseEntity 활용 예시 ===

/**
 * BaseEntity를 상속받는 엔티티 예시:
 *
 * @Entity
 * public class Post extends BaseEntity {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *
 *     private String title;
 *     private String content;
 *
 *     @ManyToOne(fetch = FetchType.LAZY)
 *     private UserAccount author;
 *
 *     // createdAt, updatedAt은 BaseEntity에서 자동 상속
 * }
 *
 * @Entity
 * public class UserAccount extends BaseEntity {
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.IDENTITY)
 *     private Long id;
 *
 *     private String username;
 *     private String password;
 *     private String role;
 *
 *     // createdAt, updatedAt은 BaseEntity에서 자동 상속
 * }
 */

// === 데이터베이스 스키마 예시 ===

/**
 * BaseEntity를 상속받은 엔티티의 실제 테이블 구조:
 *
 * CREATE TABLE user_account (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     username VARCHAR(50) NOT NULL UNIQUE,
 *     password VARCHAR(255) NOT NULL,
 *     role VARCHAR(20) NOT NULL,
 *     created_at DATETIME NOT NULL,    -- BaseEntity에서 상속
 *     updated_at DATETIME NOT NULL     -- BaseEntity에서 상속
 * );
 *
 * CREATE TABLE post (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     title VARCHAR(255) NOT NULL,
 *     content TEXT NOT NULL,
 *     author_id BIGINT NOT NULL,
 *     created_at DATETIME NOT NULL,    -- BaseEntity에서 상속
 *     updated_at DATETIME NOT NULL,    -- BaseEntity에서 상속
 *     FOREIGN KEY (author_id) REFERENCES user_account(id)
 * );
 */

// === Auditing 동작 시점 ===

/**
 * JPA 라이프사이클과 Auditing 동작:
 *
 * 1. @PrePersist (엔티티 최초 저장 전):
 *    - @CreatedDate: 현재 시간으로 설정
 *    - @LastModifiedDate: 현재 시간으로 설정 (생성 시에도 설정됨)
 *    - @CreatedBy: 현재 사용자로 설정 (설정된 경우)
 *
 * 2. @PreUpdate (엔티티 수정 전):
 *    - @LastModifiedDate: 현재 시간으로 갱신
 *    - @LastModifiedBy: 현재 사용자로 갱신 (설정된 경우)
 *    - @CreatedDate: 변경되지 않음 (불변)
 *
 * 3. 실제 SQL 실행 예시:
 *    INSERT: created_at과 updated_at 모두 현재 시간
 *    UPDATE: updated_at만 현재 시간으로 갱신
 */

// === 성능 및 인덱스 고려사항 ===

/**
 * 1. 인덱스 전략:
 * - created_at에 인덱스 추가 고려 (최신 데이터 조회 시)
 * - 복합 인덱스: (entity_type, created_at) 또는 (status, updated_at)
 * - 파티셔닝: 대용량 데이터의 경우 날짜 기준 파티셔닝
 *
 * 2. 쿼리 최적화:
 * - 날짜 범위 조회 시 BETWEEN 연산자 활용
 * - 최신 N개 데이터 조회 시 ORDER BY created_at DESC LIMIT N
 * - 수정된 데이터 추적 시 updated_at > ? 조건 활용
 *
 * 3. 데이터 관리:
 * - 오래된 데이터 아카이빙 정책 수립
 * - 불필요한 UPDATE 방지로 성능 최적화
 * - 배치 작업 시 updated_at 기준 증분 처리
 */

// === 모니터링 및 분석 활용 ===

/**
 * BaseEntity의 시간 정보를 활용한 비즈니스 분석:
 *
 * 1. 사용자 행동 분석:
 * - 가입 시점별 사용자 분포 (created_at)
 * - 활동 패턴 분석 (updated_at)
 * - 사용자 생명주기 추적
 *
 * 2. 콘텐츠 분석:
 * - 게시물 작성 패턴 (created_at)
 * - 수정 빈도 분석 (updated_at)
 * - 인기 콘텐츠 생명주기
 *
 * 3. 시스템 모니터링:
 * - 데이터 증가율 추적
 * - 수정 활동 모니터링
 * - 성능 이슈 감지 및 분석
 */