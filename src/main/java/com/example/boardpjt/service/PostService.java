package com.example.boardpjt.service;

import com.example.boardpjt.model.dto.PostDTO;
import com.example.boardpjt.model.entity.Post;
import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.PostRepository;
import com.example.boardpjt.model.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;

    // 1. create
    @Transactional
    public Post createPost(PostDTO.Request dto) {
        UserAccount userAccount = userAccountRepository
                .findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Post post = new Post();
        post.setAuthor(userAccount);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCategory(dto.getCategory());
        return postRepository.save(post);
    }

    // 2-1. findAll
    @Transactional(readOnly = true)
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    /**
     * 페이징 + 검색 + 카테고리 필터링 (검색 타입 포함) ★
     *
     * 변경사항:
     * - 기존: findWithPagingAndSearch(String keyword, String category, int page)
     * - 수정: findWithPagingAndSearch(String keyword, String category, String searchType, int page) ★
     *
     * searchType:
     * - "title": 제목 + 내용 검색
     * - "author": 작성자 검색
     *
     * 로직:
     * 1. searchType에 따라 다른 Repository 메서드 호출
     * 2. 카테고리 필터링 적용 (선택적)
     */
    @Transactional(readOnly = true)
    public Page<Post> findWithPagingAndSearch(String keyword, String category, String searchType, int page) {
        Pageable pageable = PageRequest.of(page, 5);

        // ★ 검색 타입이 "author"인 경우 - 작성자로 검색
        if ("author".equals(searchType)) {
            // 카테고리 필터링 포함
            if (!keyword.isEmpty() && !category.isEmpty()) {
                return postRepository.findByAuthorUsernameContainingAndCategoryOrderByIdDesc(
                        keyword, category, pageable);
            }
            // 카테고리 없음 - 작성자만 검색
            if (!keyword.isEmpty()) {
                return postRepository.findByAuthorUsernameContainingOrderByIdDesc(
                        keyword, pageable);
            }
            // 검색어 없음
            if (!category.isEmpty()) {
                return postRepository.findByCategoryOrderByIdDesc(category, pageable);
            }
            return postRepository.findAllByOrderByIdDesc(pageable);
        }

        // 기본값: searchType이 "title"인 경우 - 제목 + 내용으로 검색
        // 키워드 + 카테고리
        if (!keyword.isEmpty() && !category.isEmpty()) {
            return postRepository.findByTitleContainingOrContentContainingAndCategoryOrderByIdDesc(
                    keyword, keyword, category, pageable);
        }

        // 키워드만
        if (!keyword.isEmpty()) {
            return postRepository.findByTitleContainingOrContentContainingOrderByIdDesc(
                    keyword, keyword, pageable);
        }

        // 카테고리만
        if (!category.isEmpty()) {
            return postRepository.findByCategoryOrderByIdDesc(category, pageable);
        }

        // 검색 없음
        return postRepository.findAllByOrderByIdDesc(pageable);
    }

    // 2-2. findOne (byId...)
    @Transactional(readOnly = true)
    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시물 없음"));
    }

    @Transactional
    public void deleteById(Long id) {
        if (!postRepository.existsById(id)) {
            throw new IllegalArgumentException("게시물 없음");
        }
        postRepository.deleteById(id);
    }

    @Transactional
    public void updatePost(Long id, PostDTO.Request dto) {
        Post post = findById(id);
        if (!post.getAuthor().getUsername().equals(dto.getUsername())) {
            throw new SecurityException("작성자만 수정 가능");
        }
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCategory(dto.getCategory());
        postRepository.save(post);
    }
}