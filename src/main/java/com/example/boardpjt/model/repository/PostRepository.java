package com.example.boardpjt.model.repository;

import com.example.boardpjt.model.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Post 엔티티에 대한 Repository 인터페이스
 *
 * 기존 메서드:
 * - findByTitleContainingOrContentContaining()
 * - findByTitleContainingOrContentContainingOrderByIdDesc()
 * - findByTitleContainingOrContentContainingAndCategoryOrderByIdDesc()
 * - findByCategoryOrderByIdDesc()
 * - findAllByOrderByIdDesc()
 *
 * 추가 메서드 (작성자 검색용): ★
 * - findByAuthorUsernameContainingOrderByIdDesc()
 * - findByAuthorUsernameContainingAndCategoryOrderByIdDesc()
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    // ============ 기존 메서드 ============

    /**
     * 제목 또는 내용으로 검색
     */
    Page<Post> findByTitleContainingOrContentContaining(
            String title, String content, Pageable pageable);

    /**
     * 제목 또는 내용으로 검색 (최신순)
     */
    Page<Post> findByTitleContainingOrContentContainingOrderByIdDesc(
            String title, String content, Pageable pageable);

    /**
     * 제목 또는 내용, 그리고 카테고리로 검색 (최신순)
     */
    Page<Post> findByTitleContainingOrContentContainingAndCategoryOrderByIdDesc(
            String title, String content, String category, Pageable pageable);

    /**
     * 카테고리로만 검색 (최신순)
     */
    Page<Post> findByCategoryOrderByIdDesc(String category, Pageable pageable);

    /**
     * 전체 게시물을 최신순으로 조회
     */
    Page<Post> findAllByOrderByIdDesc(Pageable pageable);

    // ============ 새로 추가할 메서드 (작성자 검색) ★ ============

    /**
     * 작성자 이름으로 검색 (최신순) ★
     *
     * 예: 작성자명이 "kim"을 포함하는 모든 게시물 조회
     *
     * SQL: SELECT * FROM post
     *      WHERE user_account_id IN (
     *          SELECT id FROM user_account
     *          WHERE username LIKE %keyword%
     *      )
     *      ORDER BY id DESC
     *
     * Post의 author 필드는 UserAccount 엔티티와 ManyToOne 관계이므로,
     * author.username으로 접근하여 검색 가능
     */
    Page<Post> findByAuthorUsernameContainingOrderByIdDesc(
            String username, Pageable pageable);

    /**
     * 작성자 이름 + 카테고리로 검색 (최신순) ★
     *
     * 예: 작성자명이 "kim"이고, 카테고리가 "국내"인 게시물 조회
     *
     * SQL: SELECT * FROM post
     *      WHERE user_account_id IN (
     *          SELECT id FROM user_account
     *          WHERE username LIKE %keyword%
     *      )
     *      AND category = ?
     *      ORDER BY id DESC
     */
    Page<Post> findByAuthorUsernameContainingAndCategoryOrderByIdDesc(
            String username, String category, Pageable pageable);
}

/**
 * PostRepository 메서드 추가 요약
 *
 * 기존 메서드 (제목+내용 검색):
 * ✓ findByTitleContainingOrContentContaining()
 * ✓ findByTitleContainingOrContentContainingOrderByIdDesc()
 * ✓ findByTitleContainingOrContentContainingAndCategoryOrderByIdDesc()
 * ✓ findByCategoryOrderByIdDesc()
 * ✓ findAllByOrderByIdDesc()
 *
 * 새로 추가할 메서드 (작성자 검색): ★
 * + findByAuthorUsernameContainingOrderByIdDesc()
 * + findByAuthorUsernameContainingAndCategoryOrderByIdDesc()
 *
 * 이 메서드들도 Spring Data JPA의 메서드명 규칙으로 자동 구현됩니다.
 * 인터페이스에만 선언하면 되고, 구현 코드는 필요 없습니다.
 */