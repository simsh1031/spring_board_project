package com.example.boardpjt.service;

import com.example.boardpjt.model.dto.UserDTO;
import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final UserAccountRepository userAccountRepository;

    /**
     * 사용자 팔로우
     */
    @Transactional
    public void followUser(String followerUsername, Long targetId) {
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

    /**
     * 사용자 언팔로우
     */
    @Transactional
    public void unfollowUser(String followerUsername, Long targetId) {
        UserAccount follower = userAccountRepository
                .findByUsername(followerUsername)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        UserAccount target = userAccountRepository
                .findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("대상 없음"));
        follower.unfollow(target);
    }

    /**
     * 팔로잉 수 조회
     */
    @Transactional(readOnly = true)
    public int getFollowingCount(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return user.getFollowingCount();
    }

    /**
     * 팔로워 수 조회
     */
    @Transactional(readOnly = true)
    public int getFollowerCount(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return user.getFollowerCount();
    }

    /**
     * 팔로잉 목록 조회 ★
     * @Transactional 내에서 Lazy Loading 컬렉션 접근
     *
     * @param userId 팔로잉 목록을 조회할 사용자 ID
     * @return 해당 사용자가 팔로우하는 사람들의 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getFollowingList(Long userId) {
        UserAccount user = userAccountRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // ★ @Transactional 내에서 Lazy Loading 컬렉션 접근
        // 트랜잭션 종료 전에 DTO로 변환해야 LazyInitializationException 방지
        return user.getFollowing()
                .stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    /**
     * 팔로워 목록 조회 ★
     *
     * @param userId 팔로워 목록을 조회할 사용자 ID
     * @return 해당 사용자를 팔로우하는 사람들의 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getFollowerList(Long userId) {
        UserAccount user = userAccountRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return user.getFollowers()
                .stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    /**
     * UserAccount를 UserDTO로 변환
     * 민감한 정보(비밀번호 등)를 제외하고 변환
     */
    private UserDTO convertToUserDTO(UserAccount userAccount) {
        return new UserDTO(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getRole()
        );
    }
}