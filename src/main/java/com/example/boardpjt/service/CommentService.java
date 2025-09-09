package com.example.boardpjt.service;

import com.example.boardpjt.model.dto.CommentDTO;
import com.example.boardpjt.model.entity.Comment;
import com.example.boardpjt.model.entity.Post;
import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.CommentRepository;
import com.example.boardpjt.model.repository.PostRepository;
import com.example.boardpjt.model.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service // 스캔
@RequiredArgsConstructor // 생성자 자동 생성 (의존성 주입)
public class CommentService {
    // 의존성 주입을 3개나!
    private final CommentRepository commentRepository;
    private final UserAccountRepository userAccountRepository;
    private final PostRepository postRepository;

    @Transactional
    public Comment addComment(CommentDTO.Request dto) {
        // Controller -> authentication 인증 username. dto username?
        // Service -> username? 존재하는 유저인한건지... postId 존재하는 게시물인지?
        UserAccount user = userAccountRepository.findByUsername(dto.username())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        Post post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new IllegalArgumentException("게시물 없음"));
        Comment comment = new Comment();
        comment.setAuthor(user);
        comment.setPost(post);
        comment.setContent(dto.content());
        // id 자동생성
        return commentRepository.save(comment); // RESTful.
    }

    @Transactional(readOnly = true)
    public List<Comment> findByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Transactional
    public void deleteById(Long id) {
        commentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Comment findById(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
    }
}