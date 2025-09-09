package com.example.boardpjt.model.dto;

public class CommentDTO {
    public record Request(
            Long postId,
            String content,
            String username
    ) {}
    public record Response(
            Long id, // 댓글의 ID
            Long postId, // 댓글이 소속된 글
            String content, // 댓글 내용
            String username, // 댓글 작성자
            String createdAt // 댓글 작성일
    ) {}
}