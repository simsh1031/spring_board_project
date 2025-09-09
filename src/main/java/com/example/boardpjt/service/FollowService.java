package com.example.boardpjt.service;

import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final UserAccountRepository userAccountRepository;
    // UserAccount -> follow, unfollow, following, followers ...

    // 4개
    @Transactional
    public void followUser(String followerUsername, Long targetId) {
        // followerUsername -> Authentication에 있는 username.
        UserAccount follower = userAccountRepository
                .findByUsername(followerUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        UserAccount target = userAccountRepository
                .findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("대상 없음"));
        if (follower.equals(target)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }
        follower.follow(target);
    }

    @Transactional
    public void unfollowUser(String followerUsername, Long targetId) {
        // followerUsername -> Authentication에 있는 username.
        UserAccount follower = userAccountRepository
                .findByUsername(followerUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        UserAccount target = userAccountRepository
                .findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("대상 없음"));
        follower.unfollow(target);
    }

    public int getFollowingCount(Long userId) {
        return userAccountRepository.findById(userId).orElseThrow().getFollowingCount();
    }

    public int getFollowerCount(Long userId) {
        return userAccountRepository.findById(userId).orElseThrow().getFollowerCount();
    }
}