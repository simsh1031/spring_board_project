package com.example.boardpjt.service;

import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 계정 관련 비즈니스 로직을 처리하는 서비스 클래스 (관리자 기능 확장 버전)
 * 회원가입, 사용자 정보 관리, 관리자 전용 기능 등의 핵심 기능을 담당
 * 데이터베이스 트랜잭션과 비밀번호 암호화를 포함한 안전한 사용자 관리 시스템
 *
 * 주요 기능:
 * - 사용자 회원가입 처리
 * - 관리자용 전체 사용자 조회
 * - 관리자용 사용자 강제 탈퇴 처리
 * - 비밀번호 암호화 및 보안 관리
 * - 사용자명/ID로 단일 사용자 조회 ★
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 새로운 사용자를 등록하는 메서드
     * 사용자명 중복 검사, 비밀번호 암호화, 기본 권한 설정을 포함한 완전한 회원가입 처리
     *
     * @param username 등록할 사용자명 (중복 불가)
     * @param password 사용자가 입력한 평문 비밀번호 (암호화되어 저장됨)
     * @return UserAccount 생성된 사용자 계정 엔티티 (id 포함)
     * @throws IllegalArgumentException 이미 존재하는 사용자명일 경우 발생
     */
    @Transactional
    public UserAccount register(String username, String password) {

        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username);
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        userAccount.setPassword(passwordEncoder.encode(password));
        userAccount.setRole("ROLE_USER");

        return userAccountRepository.save(userAccount);
    }

    /**
     * 사용자명으로 단일 사용자 조회
     * 로그인 처리, 팔로우 기능 등에서 사용자 정보가 필요할 때 호출
     *
     * @param username 조회할 사용자명
     * @return UserAccount 조회된 사용자 계정 엔티티
     * @throws Exception 존재하지 않는 사용자명일 경우 발생
     */
    @Transactional(readOnly = true)
    public UserAccount findByUsername(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow();
    }

    /**
     * 사용자 ID로 단일 사용자 조회 ★ (새로 추가)
     * 팔로잉/팔로워 목록 조회, 사용자 프로필 조회 등에서 사용
     *
     * 사용 시나리오:
     * - 마이페이지에서 팔로잉/팔로워 목록 조회
     * - 특정 사용자 ID로 사용자 정보 조회
     * - 관리자 기능에서 사용자 조회
     *
     * @param id 조회할 사용자의 ID (Primary Key)
     * @return UserAccount 조회된 사용자 계정 엔티티
     * @throws EntityNotFoundException 존재하지 않는 사용자 ID인 경우 발생
     */
    @Transactional(readOnly = true)
    public UserAccount findById(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + id));
    }

    /**
     * 전체 사용자 목록을 조회하는 메서드 (관리자 전용 기능)
     * AdminController에서 관리자 페이지의 사용자 목록 표시를 위해 사용
     *
     * 보안 고려사항:
     * - 이 메서드는 관리자 권한(ROLE_ADMIN)을 가진 사용자만 호출해야 함
     * - SecurityConfig에서 /admin/** 경로를 hasRole("ADMIN")으로 보호
     * - 컨트롤러 레벨에서 권한 검증이 이미 완료된 상태에서 호출됨
     *
     * 성능 고려사항:
     * - 대량의 사용자 데이터가 있는 경우 페이징 처리 고려 필요
     * - 민감한 정보(비밀번호)는 이미 암호화되어 있어 안전
     * - 필요시 DTO 변환으로 필요한 정보만 전달 고려
     *
     * @return List<UserAccount> - 모든 사용자 계정 목록 (생성일 기준 정렬 권장)
     */
    @Transactional(readOnly = true)
    public List<UserAccount> findAllUsers() {
        return userAccountRepository.findAll();
    }

    /**
     * 사용자를 강제 탈퇴(삭제)시키는 메서드 (관리자 전용 기능)
     * AdminController에서 관리자가 문제 사용자를 시스템에서 제거할 때 사용
     *
     * 중요한 보안 및 비즈니스 로직:
     * - 관리자만 실행 가능한 매우 민감한 작업
     * - 되돌릴 수 없는 작업이므로 신중한 사용 필요
     * - 관련 데이터 정리도 함께 고려해야 함
     *
     * 삭제 시 처리해야 할 연관 데이터:
     * 1. 해당 사용자가 작성한 게시물 (Post 엔티티)
     * 2. 해당 사용자의 Refresh Token (RefreshToken 엔티티)
     * 3. 향후 댓글, 좋아요 등 추가 기능 시 관련 데이터
     *
     * @param id 삭제할 사용자의 ID (Primary Key)
     * @throws EntityNotFoundException 존재하지 않는 사용자 ID인 경우 발생
     */
    @Transactional
    public void deleteUser(Long id) {
        userAccountRepository.deleteById(id);
    }
}