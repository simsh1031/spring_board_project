package com.example.boardpjt.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 게시물 엔티티 클래스
 * 게시판 시스템의 핵심 도메인 객체로, 사용자가 작성한 게시물 정보를 저장
 * BaseEntity를 상속받아 생성일시(createdAt), 수정일시(updatedAt) 자동 관리
 *
 * 주요 기능:
 * - 게시물 기본 정보 저장 (제목, 내용)
 * - 작성자와의 연관관계 관리 (ManyToOne)
 * - 감사 정보 자동 추적 (생성/수정 시간)
 *
 * 데이터베이스 테이블: post
 * 연관 테이블: user_account (작성자 정보)
 */
@Entity // JPA 엔티티임을 선언, 데이터베이스 테이블과 매핑
@Getter // Lombok: 모든 필드에 대한 getter 메서드 자동 생성
@Setter // Lombok: 모든 필드에 대한 setter 메서드 자동 생성
// === 테이블명 커스터마이징 옵션 ===
// @Table(name = "MY_POST") // 기본 테이블명 "post" 대신 "MY_POST" 사용 시 활성화
// 일반적으로는 기본 네이밍 규칙 사용 권장: 클래스명 Post → 테이블명 post
public class Post extends BaseEntity {
    // BaseEntity에서 상속받는 필드들:
    // - LocalDateTime createdAt (생성일시)
    // - LocalDateTime updatedAt (수정일시)

    /**
     * 게시물 고유 식별자 (Primary Key)
     * 데이터베이스에서 자동으로 증가하는 값으로 설정
     */
    @Id // JPA Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL의 AUTO_INCREMENT 사용
    // GenerationType.IDENTITY: 데이터베이스의 자동 증가 컬럼에 의존
    // MySQL, PostgreSQL 등에서 사용하는 방식
    private Long id;

    /**
     * 게시물 제목
     * 사용자가 입력하는 게시물의 제목으로, 필수 입력 항목
     *
     * 데이터베이스 제약조건:
     * - NULL 값 허용하지 않음 (필수 입력)
     * - 최대 길이 200자로 제한
     * - 인덱스 추가 고려 (검색 성능 향상)
     */
    @Column(nullable = false,    // NULL 값 허용하지 않음 (필수 입력 필드)
            length = 200)        // 최대 길이 200자로 제한 (VARCHAR(200))
    private String title;

    /**
     * 게시물 내용
     * 사용자가 입력하는 게시물의 본문 내용으로, 필수 입력 항목
     *
     * 데이터베이스 제약조건:
     * - NULL 값 허용하지 않음 (필수 입력)
     * - TEXT 타입 사용으로 대용량 텍스트 저장 가능
     * - 일반적으로 65,535자까지 저장 가능 (MySQL 기준)
     */
    @Column(nullable = false,           // NULL 값 허용하지 않음 (필수 입력 필드)
            columnDefinition = "TEXT")  // 데이터베이스 컬럼 타입을 TEXT로 명시적 지정
    // columnDefinition 사용 이유:
    // - 기본 String → VARCHAR(255) 매핑보다 더 큰 텍스트 저장 필요
    // - 게시물 내용은 길어질 수 있으므로 TEXT 타입이 적합
    private String content;


    private String category;
    /**
     * 게시물 작성자 (연관관계 매핑)
     * UserAccount 엔티티와 다대일(ManyToOne) 관계를 형성
     *
     * 연관관계 설명:
     * - 한 명의 사용자(UserAccount)는 여러 개의 게시물(Post)을 작성할 수 있음
     * - 하나의 게시물(Post)은 한 명의 작성자(UserAccount)만 가짐
     * - 현재 엔티티(Post)가 "Many" 쪽, UserAccount가 "One" 쪽
     *
     * 성능 최적화:
     * - LAZY 로딩으로 필요할 때만 작성자 정보 조회
     * - N+1 문제 방지를 위해 fetch join 사용 권장
     */
    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계, 지연 로딩 사용
    // FetchType.LAZY:
    // - 작성자 정보가 실제로 필요할 때까지 데이터베이스에서 조회하지 않음
    // - 메모리 사용량 최적화 및 불필요한 쿼리 방지
    // - 대안: FetchType.EAGER (즉시 로딩, N+1 문제 위험)

    @JoinColumn(name = "user_account_id",  // 외래키 컬럼명 지정
            nullable = false)          // 작성자는 필수 (NULL 불허)
    // 외래키 설정:
    // - post 테이블에 user_account_id 컬럼 생성
    // - user_account 테이블의 id를 참조하는 외래키
    // - 데이터베이스 레벨에서 참조 무결성 보장
    private UserAccount author;

    // === 연관관계 편의 메서드 (구현 고려사항) ===

    /**
     * 게시물과 작성자 간의 양방향 연관관계 설정 편의 메서드
     * 엔티티 간 일관성 유지를 위해 사용
     */
    /*
    public void setAuthor(UserAccount author) {
        this.author = author;
        if (author != null && !author.getPosts().contains(this)) {
            author.getPosts().add(this);
        }
    }
    */

    // === 비즈니스 로직 메서드 (구현 고려사항) ===

    /**
     * 게시물 수정 권한 확인 메서드
     * 현재 사용자가 이 게시물을 수정할 권한이 있는지 확인
     */
    /*
    public boolean canEdit(String username) {
        return this.author != null && this.author.getUsername().equals(username);
    }
    */

    /**
     * 게시물 내용 요약 메서드
     * 목록 화면에서 긴 내용을 요약하여 표시할 때 사용
     */
    /*
    public String getContentSummary(int maxLength) {
        if (this.content == null) return "";
        if (this.content.length() <= maxLength) {
            return this.content;
        }
        return this.content.substring(0, maxLength) + "...";
    }
    */
}

// === 데이터베이스 스키마 ===

/**
 * 실제 생성되는 MySQL 테이블 구조:
 *
 * CREATE TABLE post (
 *     id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     title VARCHAR(200) NOT NULL,
 *     content TEXT NOT NULL,
 *     user_account_id BIGINT NOT NULL,
 *     created_at DATETIME NOT NULL,
 *     updated_at DATETIME NOT NULL,
 *
 *     FOREIGN KEY (user_account_id) REFERENCES user_account(id),
 *     INDEX idx_post_title (title),
 *     INDEX idx_post_created_at (created_at),
 *     INDEX idx_post_author (user_account_id)
 * );
 */

// === 연관관계 매핑 패턴 ===

/**
 * 1. 단방향 연관관계 (현재 구현):
 * Post → UserAccount (ManyToOne)
 * 장점: 단순한 구조, 순환 참조 없음
 * 단점: UserAccount에서 작성한 게시물 목록 조회 시 별도 쿼리 필요
 *
 * 2. 양방향 연관관계 (확장 시):
 * Post ↔ UserAccount (ManyToOne ↔ OneToMany)
 * UserAccount 엔티티에 추가:
 * @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
 * private List<Post> posts = new ArrayList<>();
 *
 * 3. 연관관계 주인:
 * Post 엔티티가 연관관계의 주인 (외래키 관리)
 * UserAccount는 읽기 전용 (mappedBy 사용)
 */

// === 성능 최적화 고려사항 ===

/**
 * 1. N+1 쿼리 문제 해결:
 * - Repository에서 fetch join 사용
 * - @EntityGraph 어노테이션 활용
 * - DTO 변환 시 필요한 데이터만 조회
 *
 * 예시:
 * @Query("SELECT p FROM Post p JOIN FETCH p.author")
 * List<Post> findAllWithAuthor();
 *
 * 2. 인덱스 전략:
 * - title 컬럼: 제목 검색용 인덱스
 * - created_at 컬럼: 최신 게시물 조회용 인덱스
 * - user_account_id 컬럼: 작성자별 게시물 조회용 인덱스
 * - 복합 인덱스: (user_account_id, created_at) 등
 *
 * 3. 캐싱 전략:
 * - 인기 게시물, 최신 게시물 등 자주 조회되는 데이터 캐싱
 * - @Cacheable 어노테이션 활용
 * - Redis 등 외부 캐시 시스템 연동
 */

// === 확장 가능한 기능들 ===

/**
 * 향후 추가 고려 가능한 필드들:
 *
 * @Column
 * private int viewCount = 0;           // 조회수
 *
 * @Column
 * private int likeCount = 0;           // 좋아요 수
 *
 * @Column(length = 50)
 * private String category;             // 카테고리
 *
 * @ElementCollection
 * @CollectionTable(name = "post_tags")
 * private Set<String> tags = new HashSet<>();  // 태그
 *
 * @Column
 * private boolean isPublic = true;     // 공개/비공개 여부
 *
 * @Column
 * private boolean isPinned = false;    // 고정 게시물 여부
 *
 * @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
 * private List<Comment> comments = new ArrayList<>();  // 댓글 목록
 */

// === 비즈니스 로직 분리 ===

/**
 * 도메인 주도 설계(DDD) 관점에서의 고려사항:
 *
 * 1. 엔티티의 책임:
 * - 데이터 저장 및 기본적인 상태 관리
 * - 간단한 비즈니스 규칙 (권한 체크 등)
 * - 연관관계 일관성 유지
 *
 * 2. 서비스 계층의 책임:
 * - 복잡한 비즈니스 로직
 * - 트랜잭션 관리
 * - 여러 엔티티 간의 협력 조정
 *
 * 3. 도메인 서비스 활용:
 * - 게시물 검색 로직
 * - 권한 체크 로직
 * - 통계 계산 로직
 */