package com.example.boardpjt.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 메인 페이지 및 사용자 페이지를 담당하는 컨트롤러
 * 애플리케이션의 홈페이지와 인증된 사용자의 마이페이지를 제공
 */
@Controller // Spring MVC 컨트롤러로 등록 (ViewResolver를 통해 뷰 이름을 실제 뷰로 변환)
public class MainController {

    /**
     * 애플리케이션의 홈페이지(메인 페이지)를 보여주는 메서드
     * 루트 경로("/") 접근 시 호출되며, 모든 사용자가 접근 가능
     *
     * @return String - 렌더링할 템플릿 파일명 (templates/index.html)
     */
    @GetMapping // 경로를 명시하지 않으면 기본적으로 루트 경로("/")에 매핑됨
    public String index() {
        // ViewResolver가 "index"를 templates/index.html로 변환하여 렌더링
        // 일반적으로 애플리케이션 소개, 로그인/회원가입 링크 등이 포함된 메인 페이지
        return "index";
    }

    /**
     * 인증된 사용자의 마이페이지를 보여주는 메서드
     * 로그인한 사용자만 접근 가능하며, 사용자 정보를 템플릿에 전달
     *
     * @param model Spring MVC의 Model 객체 - 뷰에 데이터를 전달하기 위한 컨테이너
     * @param authentication Spring Security의 Authentication 객체 - 현재 인증된 사용자 정보
     * @return String - 렌더링할 템플릿 파일명 (templates/my-page.html)
     */
    @GetMapping("/my-page") // GET /my-page 요청 처리
    public String myPage(Model model, Authentication authentication) {

        // === 인증 상태 확인 및 사용자 정보 전달 ===
        if (authentication != null) {
            // Authentication 객체가 존재하는 경우 (정상적으로 인증된 사용자)

            // authentication.getName()은 사용자의 Principal(주체)을 반환
            // 일반적으로 사용자명(username) 또는 이메일이 반환됨
            model.addAttribute("username", authentication.getName());
            model.addAttribute("role", authentication.getAuthorities().iterator().next().getAuthority()); // 첫번째 Role
            // 추가로 사용할 수 있는 Authentication 정보들:
            // authentication.getAuthorities() - 사용자의 권한(역할) 목록
            // authentication.getDetails() - 추가적인 인증 세부정보 (IP 주소, 세션 ID 등)
            // authentication.getCredentials() - 인증 자격 증명 (보통 null, 보안상 이유로)
            // authentication.getPrincipal() - 주체 정보 (UserDetails 객체 또는 사용자명)
        }

        // Model에 추가된 "username" 속성은 Thymeleaf 템플릿에서 ${username}으로 접근 가능
        // 예: <span th:text="${username}">사용자명</span>

        // ViewResolver가 "my-page"를 templates/my-page.html로 변환하여 렌더링
        // 일반적으로 사용자 프로필, 설정, 개인 정보 등이 표시되는 페이지
        return "my-page";
    }
}