package com.example.boardpjt.model.repository;

import com.example.boardpjt.model.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Post 엔티티에 대한 데이터 접근 계층(Repository) 인터페이스
 * Spring Data JPA를 사용하여 게시물 관련 데이터베이스 작업을 처리
 * JpaRepository를 상속받아 기본적인 CRUD 메서드들을 자동으로 제공받음
 *
 * 제공되는 기본 메서드들:
 * - save(Post entity): 게시물 저장/수정
 * - findById(Long id): ID로 게시물 조회
 * - findAll(): 모든 게시물 조회
 * - deleteById(Long id): ID로 게시물 삭제
 * - count(): 전체 게시물 수 조회
 * - existsById(Long id): 게시물 존재 여부 확인
 */
public interface PostRepository extends JpaRepository<Post, Long> {
    // JpaRepository<엔티티 클래스, Primary Key 타입>
    // - Post: 관리할 엔티티 클래스
    // - Long: Primary Key(id 필드)의 데이터 타입

    // find
    // By -> Id
    // Title -> Containing / SQL like -> %키워드%
    // or Content -> Containing
    Page<Post> findByTitleContainingOrContentContaining(
            String title, String content, Pageable pageable);

    // 최신순으로
    Page<Post> findByTitleContainingOrContentContainingOrderByIdDesc(
            String title, String content, Pageable pageable);
    // Desc -> PK (Long id)
}

// === Repository 확장 시 고려사항 ===

/**
 * 1. 메서드명 패턴:
 * - findBy: 조회 (SELECT)
 * - countBy: 개수 (COUNT)
 * - deleteBy: 삭제 (DELETE)
 * - existsBy: 존재 여부 (EXISTS)
 *
 * 2. 조건 연산자:
 * - And: findByTitleAndContent
 * - Or: findByTitleOrContent
 * - Like/Containing: findByTitleContaining
 * - IgnoreCase: 대소문자 구분 없음
 * - OrderBy: 정렬 (Asc/Desc)
 * - Between: 범위 조건
 * - GreaterThan/LessThan: 크기 비교
 *
 * 3. 성능 고려사항:
 * - Like 검색 시 인덱스 활용도 고려
 * - 페이징 사용으로 대용량 데이터 처리
 * - fetch join으로 N+1 문제 해결
 * - Projection으로 필요한 데이터만 조회
 *
 * 4. 보안 고려사항:
 * - SQL Injection 방지 (JPA가 기본 제공)
 * - 권한 기반 데이터 접근 제어
 * - 민감한 정보 노출 방지
 */