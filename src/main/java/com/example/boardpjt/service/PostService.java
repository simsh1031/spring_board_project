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
        return postRepository.save(post);
    }
    // 2-1. findAll
    @Transactional(readOnly = true)
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    // 2-1-2. paging & search
    @Transactional(readOnly = true)
    public Page<Post> findWithPagingAndSearch(String keyword, int page) {
        // 키워드 구현은 조금 이따가...
        // TODO : keyword
        Pageable pageable = PageRequest.of(page, 5);
//        return postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
        return postRepository.findByTitleContainingOrContentContainingOrderByIdDesc(keyword, keyword, pageable);
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
        Post post = findById(id); // 없으면 예외처리로...
        // 작성자와 수정을 하려는 사람이 다르다
        if (!post.getAuthor().getUsername().equals(dto.getUsername())) {
            throw new SecurityException("작성자만 수정 가능");
        }
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        postRepository.save(post);
    }
}
