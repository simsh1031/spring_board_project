package com.example.boardpjt.controller;

import com.example.boardpjt.model.entity.RefreshToken;
import com.example.boardpjt.model.repository.RefreshTokenRepository;
import com.example.boardpjt.service.UserAccountService;
import com.example.boardpjt.util.CookieUtil;
import com.example.boardpjt.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 전용 컨트롤러
 * 시스템 관리 기능을 제공하며, ADMIN 역할을 가진 사용자만 접근 가능
 * SecurityConfig에서 "/admin/**" 경로를 hasRole("ADMIN")으로 보호
 *
 * 주요 기능:
 * - 전체 회원 목록 조회
 * - 회원 강제 탈퇴 처리
 * - 기타 관리자 전용 기능들
 */
@Controller // Spring MVC 컨트롤러로 등록 (HTML 뷰 반환)
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입용)
@RequestMapping("/admin") // 모든 요청 경로에 "/admin" 접두사 추가
public class AdminController {

    // 사용자 계정 관련 비즈니스 로직을 처리하는 서비스
    // 회원 조회, 삭제 등의 관리 기능을 제공
    private final UserAccountService userAccountService;

    // === 사용되지 않는 의존성들 (정리 필요) ===
    // 아래 import들은 현재 사용되지 않으므로 제거를 고려해야 함
    // private final RefreshTokenRepository refreshTokenRepository;
    // private final CookieUtil cookieUtil;
    // private final JwtUtil jwtUtil;
    // private final AuthenticationManager authenticationManager;

    /**
     * 관리자 메인 페이지 - 전체 회원 목록 표시
     * GET /admin 요청을 처리하여 모든 사용자 정보를 조회하고 관리 페이지 제공
     *
     * 접근 권한: ROLE_ADMIN 역할을 가진 사용자만 접근 가능
     * SecurityConfig: .requestMatchers("/admin/**").hasRole("ADMIN")
     *
     * @param model Spring MVC의 Model 객체 - 뷰에 데이터를 전달하기 위한 컨테이너
     * @return String - 렌더링할 템플릿 파일명 (templates/admin.html)
     */
    @GetMapping // GET /admin 요청 처리 (기본 경로)
    public String adminPage(Model model) {

        // === 전체 사용자 목록 조회 ===
        // UserAccountService를 통해 데이터베이스에서 모든 사용자 정보 조회
        // 이 메서드는 UserAccountService에 새로 추가되어야 하는 기능
        model.addAttribute("users", userAccountService.findAllUsers());

        // Model에 추가된 "users" 속성은 Thymeleaf 템플릿에서 다음과 같이 사용:
        // <tr th:each="user : ${users}">
        //     <td th:text="${user.id}"></td>
        //     <td th:text="${user.username}"></td>
        //     <td th:text="${user.role}"></td>
        //     <td th:text="${user.createdAt}"></td>
        // </tr>

        // ViewResolver가 "admin"을 templates/admin.html로 변환하여 렌더링
        return "admin";
    }

    /**
     * 회원 강제 탈퇴 처리
     * POST /admin/delete/{id} 요청을 처리하여 특정 사용자를 시스템에서 삭제
     *
     * 보안 주의사항:
     * - 관리자만 실행 가능한 매우 위험한 작업
     * - 삭제 전 확인 절차 필요 (프론트엔드에서 confirm 대화상자 등)
     * - 삭제 로그 기록 권장 (감사 추적)
     *
     * @param id 삭제할 사용자의 ID (URL 경로에서 추출)
     * @return String - 삭제 완료 후 관리자 페이지로 리다이렉트
     */
    @PostMapping("/delete/{id}") // POST /admin/delete/123 형태의 요청 처리
    public String deleteUser(@PathVariable Long id) {
        // @PathVariable: URL 경로의 {id} 부분을 메서드 매개변수로 바인딩
        // 예: /admin/delete/123 → id = 123L

        // === 사용자 삭제 처리 ===
        // UserAccountService를 통해 해당 ID의 사용자 계정 삭제
        // 이 과정에서 다음 작업들이 함께 수행되어야 함:
        // 1. 사용자 계정 삭제 (UserAccount 엔티티)
        // 2. 관련 Refresh Token 삭제 (RefreshToken 엔티티)
        // 3. 사용자가 작성한 게시글/댓글 처리 (CASCADE 설정 또는 별도 처리)
        // 4. 삭제 로그 기록 (감사 목적)
        userAccountService.deleteUser(id);

        // === 삭제 완료 후 리다이렉트 ===
        // PRG (Post-Redirect-Get) 패턴 적용
        // - POST 요청 처리 후 GET 요청으로 리다이렉트
        // - 브라우저 새로고침 시 중복 삭제 방지
        // - 사용자에게 즉시 업데이트된 목록 표시
        return "redirect:/admin";
    }
}

// === 보안 고려사항 ===

/**
 * 1. 권한 검증:
 * - SecurityConfig에서 /admin/** 경로를 ADMIN 역할로 보호
 * - 컨트롤러 레벨에서 추가 권한 검증 고려: @PreAuthorize("hasRole('ADMIN')")
 *
 * 2. 감사 로그:
 * - 관리자 행동 모두 로그 기록
 * - 사용자 삭제, 역할 변경 등 중요한 작업 추적
 *
 * 3. 입력 검증:
 * - Path Variable 검증 (@Valid, @PathVariable 유효성)
 * - SQL Injection 방지 (JPA 사용으로 기본 보호)
 *
 * 4. 에러 처리:
 * - 존재하지 않는 사용자 ID 요청 처리
 * - 자기 자신 삭제 방지
 * - 마지막 관리자 삭제 방지
 */

// === 사용자 경험 개선사항 ===

/**
 * 1. 확인 절차:
 * - JavaScript로 삭제 전 확인 대화상자
 * - 중요한 작업에 대한 이중 확인
 *
 * 2. 피드백 메시지:
 * - 작업 성공/실패 메시지 표시
 * - RedirectAttributes로 Flash Message 전달
 *
 * 3. 페이징 처리:
 * - 대량의 사용자 데이터에 대한 페이징
 * - 검색 및 필터링 기능
 *
 * 4. 정렬 기능:
 * - 가입일, 사용자명, 역할별 정렬
 * - 오름차순/내림차순 토글
 */

// === 성능 최적화 ===

/**
 * 1. 데이터 조회 최적화:
 * - N+1 문제 방지를 위한 fetch join 사용
 * - 필요한 필드만 조회하는 DTO 활용
 *
 * 2. 캐싱 적용:
 * - 자주 조회되는 사용자 통계 정보 캐싱
 * - @Cacheable 어노테이션 활용
 *
 * 3. 배치 처리:
 * - 대량 삭제/수정 작업 시 배치 처리
 * - 비동기 처리 고려
 */