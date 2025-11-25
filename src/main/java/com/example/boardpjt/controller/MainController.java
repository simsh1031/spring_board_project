package com.example.boardpjt.controller;

import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 메인 페이지 및 사용자 페이지를 담당하는 컨트롤러
 * 애플리케이션의 홈페이지와 인증된 사용자의 마이페이지를 제공
 */
@Controller
@RequiredArgsConstructor // ★ 의존성 주입을 위해 추가
public class MainController {

    private final UserAccountService userAccountService; // ★ 추가

    /**
     * 애플리케이션의 홈페이지(메인 페이지)를 보여주는 메서드
     * 루트 경로("/") 접근 시 호출되며, 모든 사용자가 접근 가능
     *
     * @return String - 렌더링할 템플릿 파일명 (templates/index.html)
     */
    @GetMapping
    public String index() {
        return "index";
    }

    /**
     * 인증된 사용자의 마이페이지를 보여주는 메서드
     * 로그인한 사용자만 접근 가능하며, 사용자 정보를 템플릿에 전달
     *
     * 변경사항:
     * - UserAccountService 주입 ★
     * - userId 조회 및 모델에 추가 ★
     * - UserAccount 엔티티에서 정보 추출
     *
     * @param model Spring MVC의 Model 객체 - 뷰에 데이터를 전달하기 위한 컨테이너
     * @param authentication Spring Security의 Authentication 객체 - 현재 인증된 사용자 정보
     * @return String - 렌더링할 템플릿 파일명 (templates/my-page.html)
     */
    @GetMapping("/my-page")
    public String myPage(Model model, Authentication authentication) {

        // === 인증 상태 확인 및 사용자 정보 전달 ===
        if (authentication != null && authentication.isAuthenticated()) {
            // 인증된 사용자 정보 추출
            String username = authentication.getName();

            System.out.println("마이페이지 접속 - 사용자명: " + username);

            try {
                // ★ UserAccountService를 통해 전체 사용자 정보 조회
                UserAccount userAccount = userAccountService.findByUsername(username);

                System.out.println("사용자 정보 조회 - ID: " + userAccount.getId() + ", Username: " + userAccount.getUsername());

                // ★ 모델에 필요한 정보 추가
                model.addAttribute("username", userAccount.getUsername());
                model.addAttribute("role", userAccount.getRole());
                model.addAttribute("userId", userAccount.getId()); // ★ userId 추가 (팔로잉/팔로워 조회 시 필요)

            } catch (Exception e) {
                System.err.println("사용자 정보 조회 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return "my-page";
    }
}