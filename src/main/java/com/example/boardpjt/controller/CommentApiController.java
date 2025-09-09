package com.example.boardpjt.controller;

import com.example.boardpjt.model.dto.CommentDTO;
import com.example.boardpjt.model.entity.Comment;
import com.example.boardpjt.model.entity.Post;
import com.example.boardpjt.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentApiController {
    private final CommentService commentService;

    @PostMapping("/{postId}")
    public ResponseEntity<Comment> create(@PathVariable Long postId,
                                          // JSON Body -> 변환
                                          @RequestBody CommentDTO.Request dto,
                                          Authentication authentication) {
        System.out.println("dto = " + dto);
        try {
            if (!postId.equals(dto.postId())) {
                throw new IllegalArgumentException("postId 불일치");
            }
            //
            if (!dto.username().equals(authentication.getName())) {
                throw new SecurityException("작성자와 불일치");
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(commentService.addComment(dto));
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (SecurityException ex) {
            System.err.println(ex.getMessage());
            // 401 : 인증 - 정보 없음? -> 아예 jwt가 없거나 비로그인.
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            // 403 : 인가 - 권한 없음 (level 문제)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{postId}")
//    public ResponseEntity<List<Comment>> list(@PathVariable Long postId) {
    public ResponseEntity<List<CommentDTO.Response>> list(
            @PathVariable Long postId) {
        // 그대로 내보내면 serializer 에러
        // -> Comment -> UserAccount, Post
        // CommentDTO.Response
        return ResponseEntity.ok(
                commentService.findByPostId(postId)
                        .stream().map(c -> new CommentDTO.Response(
                                c.getId(),
                                c.getPost().getId(),
                                c.getContent(),
                                c.getAuthor().getUsername(),
                                c.getCreatedAt().toString()
                        )).toList());

    }

    @DeleteMapping("{id}") // commentId
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       Authentication authentication) {
        // 현재 이 댓글의 작성자와 삭제하려고 하는 사람이 일치하는지
        Comment comment = commentService.findById(id);
        if (!comment.getAuthor().getUsername().equals(authentication.getName())) {
            // throw 꼭 할 필요는 없음
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        commentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}