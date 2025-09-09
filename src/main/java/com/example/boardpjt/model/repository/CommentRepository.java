package com.example.boardpjt.model.repository;

import com.example.boardpjt.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // JPA Query Method
    // 게시물에 속한 댓글을 찾는 메서드
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    // findBy
    // PostId -> 속한 join -> 게시물
    // OrderBy / CreatedAt (audit, base entity) / Asc -> 오름차순정렬
}
