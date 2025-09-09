package com.example.boardpjt.service;

import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security에서 사용자 인증 시 사용자 정보를 로드하는 커스텀 서비스
 * UserDetailsService 인터페이스를 구현하여 데이터베이스에서 사용자 정보를 조회하고
 * Spring Security가 요구하는 UserDetails 객체로 변환하여 반환
 */
@Service // Spring의 서비스 빈으로 등록
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입용)
public class CustomUserDetailsService implements UserDetailsService {

    // 사용자 계정 정보를 데이터베이스에서 조회하기 위한 Repository
    private final UserAccountRepository userAccountRepository;

    /**
     * 사용자명(username)을 받아 해당 사용자의 상세 정보를 로드하는 메서드
     * Spring Security의 인증 과정에서 자동으로 호출됨
     * JWT 필터에서도 토큰의 사용자명으로 사용자 정보를 조회할 때 사용
     *
     * @param username 조회할 사용자명 (JWT 토큰에서 추출되거나 로그인 폼에서 입력된 값)
     * @return UserDetails Spring Security에서 사용하는 사용자 정보 객체
     * @throws UsernameNotFoundException 해당 사용자명으로 사용자를 찾을 수 없을 때 발생
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // === 1단계: 데이터베이스에서 사용자 조회 ===
        // Repository를 통해 사용자명으로 UserAccount 엔티티 조회
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                // Optional이 비어있는 경우 (사용자가 존재하지 않는 경우) 예외 발생
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // === 2단계: UserAccount → UserDetails 변환 ===
        // 조회된 UserAccount 엔티티를 Spring Security의 UserDetails 객체로 변환
        // User.builder()는 Spring Security에서 제공하는 UserDetails 구현체 생성 빌더
        return User.builder()
                // 사용자명 설정
                .username(userAccount.getUsername())

                // 암호화된 비밀번호 설정 (이미 PasswordEncoder로 암호화된 상태)
                .password(userAccount.getPassword())

                // === 권한(Role) 설정 - 중요한 변환 과정 ===
                // 데이터베이스에 저장된 role: "ROLE_USER", "ROLE_ADMIN" 등
                // User.roles() 메서드는 자동으로 "ROLE_" 접두사를 추가하므로
                // 기존 "ROLE_" 접두사를 제거해야 중복을 방지할 수 있음
                //
                // 예시:
                // DB 저장값: "ROLE_USER"
                // replace 후: "USER"
                // User.roles("USER") → 최종 권한: "ROLE_USER"
                .roles(userAccount.getRole().replace("ROLE_", ""))

                // === 추가 설정 가능한 옵션들 ===
                // .accountExpired(false)        // 계정 만료 여부
                // .accountLocked(false)         // 계정 잠김 여부
                // .credentialsExpired(false)    // 비밀번호 만료 여부
                // .disabled(false)              // 계정 비활성화 여부

                // UserDetails 객체 생성 완료
                .build();

        // === UserDetailsService의 역할 ===
        // 1. 인증 과정에서 사용자 정보 제공
        // 2. JWT 필터에서 토큰 검증 후 사용자 정보 로드
        // 3. 권한 체크 시 사용자 권한 정보 제공
        // 4. Spring Security Context에 저장될 Authentication 객체 생성에 사용

        // === 호출되는 시점들 ===
        // 1. 사용자가 로그인할 때 (AuthenticationManager가 호출)
        // 2. JWT 필터에서 토큰을 검증할 때 (JwtFilter에서 호출)
        // 3. @PreAuthorize 등 권한 체크 시 필요한 경우 (AOP)
    }
}