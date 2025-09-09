package com.example.boardpjt.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * 사용자 계정 정보를 저장하는 JPA 엔티티 클래스
 * 데이터베이스의 user_account 테이블과 매핑되며, 사용자 인증 및 권한 관리에 사용
 */
@Entity // JPA 엔티티임을 선언, 데이터베이스 테이블과 매핑되는 클래스
@Getter // Lombok: 모든 필드에 대한 getter 메서드 자동 생성
@Setter // Lombok: 모든 필드에 대한 setter 메서드 자동 생성
// 참고: Lombok이 기본 생성자도 자동으로 생성함 (매개변수 없는 생성자)

// === 테이블 커스터마이징 옵션 ===
// @Table 어노테이션은 필수가 아니며, 테이블명을 변경하고 싶을 때 사용
// 예: @Table(name = "MY_USER_ACCOUNT") -> 테이블명을 "MY_USER_ACCOUNT"로 지정
// 기본적으로는 클래스명을 snake_case로 변환한 "user_account" 테이블명 사용
public class UserAccount {

    /**
     * 사용자 고유 식별자 (Primary Key)
     * 데이터베이스에서 자동으로 증가하는 값으로 설정
     */
    @Id // JPA Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL의 AUTO_INCREMENT 사용
    // GenerationType.IDENTITY: 데이터베이스의 자동 증가 컬럼에 의존 (MySQL, PostgreSQL 등)
    // 다른 옵션들:
    // - GenerationType.AUTO: JPA가 데이터베이스에 맞게 자동 선택
    // - GenerationType.SEQUENCE: 시퀀스 사용 (Oracle, PostgreSQL 등)
    // - GenerationType.TABLE: 별도 테이블을 사용한 키 생성
    private Long id;

    /**
     * 사용자명 (로그인 시 사용)
     * 중복 불가능하며 최대 50자까지 저장 가능
     */
    @Column(nullable = false,    // NULL 값 허용하지 않음 (필수 입력 필드)
            unique = true,       // 유니크 제약조건 설정 (중복 값 불허)
            length = 50)         // 최대 길이 50자로 제한 (VARCHAR(50))
    private String username;

    /**
     * 사용자 비밀번호 (암호화되어 저장)
     * PasswordEncoder를 통해 암호화된 상태로 데이터베이스에 저장됨
     * 평문 비밀번호는 절대 저장되지 않음
     */
    @Column(nullable = false) // NULL 값 허용하지 않음 (필수 입력 필드)
    // length 설정 없음: PasswordEncoder 암호화 시 길이가 늘어나므로 기본 255자 사용
    // 예: BCrypt는 60자, Argon2는 더 길 수 있음
    private String password;

    /**
     * 사용자 역할/권한 정보
     * Spring Security에서 권한 체크 시 사용되는 역할 정보
     */
    @Column(nullable = false,    // NULL 값 허용하지 않음 (필수 입력 필드)
            length = 20)         // 최대 길이 20자로 제한 (VARCHAR(20))
    // 일반적인 역할 값 예시:
    // - "ROLE_USER": 일반 사용자 권한
    // - "ROLE_ADMIN": 관리자 권한
    // - "ROLE_MODERATOR": 중간 관리자 권한
    // 주의: Spring Security는 역할명이 "ROLE_" 접두사로 시작하는 것을 권장
    private String role;

    // 내가 팔로우한 사람들 (following)
    @ManyToMany // 중개테이블/엔티티 -> 구현. / ManyToMany
    @JoinTable(
            name = "user_follow", // 중간에 생길 가상의 테이블
            joinColumns = @JoinColumn(name = "follower_id"),
            // 현재 엔티티가 '팔로우를 건 사람' -> 현재 엔티티의 id를 follower_id라는 의미로 user_follower에 저장
            inverseJoinColumns = @JoinColumn(name = "following_id")
            // 상대방 엔티티가 팔로루르 당한 사람(following_id)
    )
    // 1 user -> 3 user. follower_id 1 / following_id 3
    private Set<UserAccount> following = new HashSet<>();

    // 나를 팔로우한 사람들 (followers)
    @ManyToMany(mappedBy = "following")
    private Set<UserAccount> followers = new HashSet<>();

    // 1. 서로 팔로우를 안한 경우
    // 2. 한쪽만 팔로우를 한 경우
    // 3. 서로 맞팔로우한 경우

    public void follow(UserAccount target) {
        // 이 엔티티가 target 팔로우 (follower_id) -> (target. following_id)
        following.add(target);
        target.followers.add(this); // this 현재 엔티티로 만들어진 인스턴스 자체
    }

    public void unfollow(UserAccount target) {
        following.remove(target);
        target.followers.remove(this);
    }

    public int getFollowerCount() {
        return followers.size();
    }

    public int getFollowingCount() {
        return following.size();
    }
}