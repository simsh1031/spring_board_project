package com.example.boardpjt.controller;

import com.example.boardpjt.model.dto.UserDTO;
import com.example.boardpjt.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 팔로우 관련 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follow")
public class FollowApiController {

    private final FollowService followService;

    /**
     * 사용자 팔로우
     * POST /api/follow/{userId}
     */
    @PostMapping("/{userId}")
    public ResponseEntity<Void> follow(@PathVariable Long userId,
                                       Authentication authentication) {
        followService.followUser(authentication.getName(), userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자 언팔로우
     * DELETE /api/follow/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId,
                                         Authentication authentication) {
        followService.unfollowUser(authentication.getName(), userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 팔로잉 목록 조회 ★
     * GET /api/follow/{userId}/following
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<UserDTO>> getFollowing(@PathVariable Long userId) {
        List<UserDTO> followingList = followService.getFollowingList(userId);
        System.out.println("팔로잉 목록 조회 성공: " + userId + ", 개수: " + followingList.size());
        return ResponseEntity.ok(followingList);
    }

    /**
     * 팔로워 목록 조회 ★
     * GET /api/follow/{userId}/followers
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<UserDTO>> getFollowers(@PathVariable Long userId) {
        List<UserDTO> followerList = followService.getFollowerList(userId);
        System.out.println("팔로워 목록 조회 성공: " + userId + ", 개수: " + followerList.size());
        return ResponseEntity.ok(followerList);
    }

    /**
     * 팔로잉 수 조회
     * GET /api/follow/{userId}/followingCount
     */
    @GetMapping("/{userId}/followingCount")
    public ResponseEntity<Integer> followingCount(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowingCount(userId));
    }

    /**
     * 팔로워 수 조회
     * GET /api/follow/{userId}/followerCount
     */
    @GetMapping("/{userId}/followerCount")
    public ResponseEntity<Integer> followerCount(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowerCount(userId));
    }
}