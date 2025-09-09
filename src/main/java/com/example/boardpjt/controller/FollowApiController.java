package com.example.boardpjt.controller;

import com.example.boardpjt.model.dto.CommentDTO;
import com.example.boardpjt.model.entity.Comment;
import com.example.boardpjt.service.CommentService;
import com.example.boardpjt.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follow")
public class FollowApiController {
    private final FollowService followService;

    @PostMapping("/{userId}")
    public void follow(@PathVariable Long userId,
                       Authentication authentication) {
        followService.followUser(authentication.getName(), userId);
    }

    @DeleteMapping("/{userId}")
    public void unfollow(@PathVariable Long userId,
                         Authentication authentication) {
        followService.unfollowUser(authentication.getName(), userId);
    }

    @GetMapping("/{userId}/followingCount")
    public int followingCount(@PathVariable Long userId) {
        return followService.getFollowingCount(userId);
    }

    @GetMapping("/{userId}/followerCount")
    public int followerCount(@PathVariable Long userId) {
        return followService.getFollowerCount(userId);
    }
}