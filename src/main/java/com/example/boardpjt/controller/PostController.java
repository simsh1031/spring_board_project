package com.example.boardpjt.controller;

import com.example.boardpjt.model.dto.PostDTO;
import com.example.boardpjt.model.entity.Post;
import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.service.PostService;
import com.example.boardpjt.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 게시물 관련 컨트롤러
 * 게시판의 핵심 기능인 CRUD(Create, Read, Update, Delete) 작업을 처리
 * 사용자 인증을 통한 권한 기반 게시물 관리 제공
 *
 * 주요 기능:
 * - 게시물 목록 조회
 * - 게시물 상세 보기
 * - 게시물 작성
 * - 게시물 삭제 (작성자만 가능)
 */
@Controller // Spring MVC 컨트롤러로 등록 - 컴포넌트 스캔 대상
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입용)
@RequestMapping("/posts") // 모든 요청 경로에 "/posts" 접두사 추가
public class PostController {

    // 게시물 관련 비즈니스 로직을 처리하는 서비스
    private final PostService postService;

    // 참고: UserAccountService는 import되어 있지만 현재 사용되지 않음 (정리 권장)

    /**
     * 게시물 목록 페이지
     * 모든 게시물을 조회하여 목록 형태로 표시
     * Entity를 DTO로 변환하여 뷰에 필요한 데이터만 전달
     *
     * @param model Spring MVC의 Model 객체 - 뷰에 데이터 전달
     * @return String - 렌더링할 템플릿 파일명 (templates/post/list.html)
     */
    @GetMapping
    public String list(Model model,
//                       @RequestParam(defaultValue = "1", required = false) int page) {
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String keyword) {
        if (!StringUtils.hasText(keyword)) {
            keyword = ""; // 명백히 빈 텍스트 (null 이런거 처리)
        }
        // 유저는 페이지가 1씩 시작하는게 자연스러워요
        Page<Post> postPage = postService.findWithPagingAndSearch(keyword, page - 1);
        // 현재 페이지
        model.addAttribute("currentPage", page);
        // 전체 페이지
        model.addAttribute("totalPages", postPage.getTotalPages());
        // 전달할 게시물 데이터
        model.addAttribute("posts",
//                postService.findAll()
                // Page<Post>
                postPage.getContent() // list화
                        .stream().map(p -> new PostDTO.Response(
                                p.getId(),                          // 게시물 ID
                                p.getTitle(),                       // 제목
                                p.getContent(),                     // 내용
                                p.getAuthor().getUsername(),        // 작성자명
                                p.getCreatedAt().toString()         // 작성일시 (문자열 변환)
                        ))
        );

        return "post/list"; // templates/post/list.html 렌더링
    }

    private final UserAccountService userAccountService;

    @GetMapping("/{id}") // GET /posts/123 형태의 요청 처리
    public String detail(@PathVariable Long id, Model model,
                         Authentication authentication) {
        // @PathVariable: URL 경로의 {id} 부분을 메서드 매개변수로 바인딩
        UserAccount userAccount = userAccountService.findByUsername(authentication.getName());
        Post post = postService.findById(id);
        boolean followCheck = post.getAuthor().getFollowers().contains(userAccount);

        model.addAttribute("followCheck", followCheck);

        // === 개별 게시물 조회 ===
        // PostService를 통해 특정 ID의 게시물 조회
        // 존재하지 않는 ID인 경우 Service에서 예외 처리 필요
        model.addAttribute("post", post);

        // === '내 게시물' 표시 방법 고려사항 ===
        // 현재는 Entity를 직접 전달하고 있으며, 뷰에서 작성자 확인 가능한 방법들:
        //
        // 방법 1: 컨트롤러에서 처리 (권장)
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // boolean isMyPost = post.getAuthor().getUsername().equals(auth.getName());
        // model.addAttribute("isMyPost", isMyPost);
        //
        // 방법 2: Thymeleaf에서 처리
        // <div th:if="${#authentication.name == post.author.username}">
        //     내 게시물 표시 또는 수정/삭제 버튼
        // </div>
        //
        // 방법 3: DTO에 포함
        // PostDTO.DetailResponse에 isMyPost 필드 추가

        return "post/detail"; // templates/post/detail.html 렌더링
    }

    /**
     * 게시물 작성 폼 페이지
     * 새로운 게시물을 작성할 수 있는 폼을 제공
     * 현재 로그인한 사용자 정보를 미리 설정
     *
     * @param model Spring MVC의 Model 객체
     * @param authentication Spring Security의 인증 정보 (현재 로그인한 사용자)
     * @return String - 렌더링할 템플릿 파일명 (templates/post/form.html)
     */
    @GetMapping("/new") // GET /posts/new 요청 처리
    public String createForm(Model model, Authentication authentication) {

        // === 빈 DTO 객체 생성 및 사용자 정보 설정 ===
        PostDTO.Request dto = new PostDTO.Request();

        // 현재 인증된 사용자의 이름을 DTO에 설정
        // authentication.getName(): JWT 토큰에서 추출된 사용자명
        dto.setUsername(authentication.getName());

        // 폼 바인딩을 위해 빈 DTO를 Model에 추가
        // Thymeleaf에서 th:object="${post}"로 폼 데이터 바인딩
        model.addAttribute("post", dto);

        return "post/form"; // templates/post/form.html 렌더링
    }

    /**
     * 게시물 작성 처리
     * POST 요청으로 전송된 게시물 데이터를 받아서 저장
     *
     * @param dto 폼에서 전송된 게시물 데이터 (@ModelAttribute로 자동 바인딩)
     * @param authentication Spring Security의 인증 정보
     * @return String - 작성 완료 후 리다이렉트할 경로
     */
    @PostMapping("/new") // POST /posts/new 요청 처리
    public String create(@ModelAttribute PostDTO.Request dto, Authentication authentication) {
        // @ModelAttribute: HTTP 요청 파라미터를 DTO 객체에 자동 바인딩
        // 폼의 name 속성과 DTO의 필드명이 일치해야 함

        // === 보안 검증: 사용자 정보 재설정 ===
        // 클라이언트에서 전송된 username을 무시하고 인증된 사용자 정보로 덮어씀
        // 이는 보안상 매우 중요: 다른 사용자 이름으로 게시물 작성 방지
        dto.setUsername(authentication.getName());

        // === 추가 검증 고려사항 ===
        // 폼에서 전송된 username과 인증된 사용자가 다른 경우 에러 처리:
        // if (!dto.getUsername().equals(authentication.getName())) {
        //     throw new SecurityException("권한이 없습니다.");
        // }

        // === 게시물 생성 ===
        // PostService를 통해 게시물 저장 처리
        // DTO → Entity 변환 및 데이터베이스 저장
        postService.createPost(dto);

        // === PRG 패턴 적용 ===
        // Post-Redirect-Get 패턴으로 중복 제출 방지
        // 새로고침 시 게시물이 중복 생성되는 것을 방지
        return "redirect:/posts";
    }

    /**
     * 게시물 삭제 처리
     * 작성자만 자신의 게시물을 삭제할 수 있음
     *
     * @param id 삭제할 게시물의 ID
     * @param authentication Spring Security의 인증 정보
     * @return String - 삭제 완료 후 리다이렉트할 경로
     */
    @PostMapping("/{id}/delete") // POST /posts/123/delete 형태의 요청 처리
    public String delete(@PathVariable Long id, Authentication authentication) {

        try {
            // === 1단계: 게시물 조회 및 권한 검증 ===
            Post post = postService.findById(id);

            // 게시물 작성자의 사용자명 추출
            String postUsername = post.getAuthor().getUsername();

            // === 권한 검증: 작성자 본인만 삭제 가능 ===
            if (!postUsername.equals(authentication.getName())) {
                // 현재 인증된 사용자와 게시물 작성자가 다른 경우
                throw new SecurityException("삭제 권한이 없습니다.");
            }

            // === 2단계: 게시물 삭제 실행 ===
            // 권한 검증을 통과한 경우에만 삭제 수행
            // 존재하지 않는 게시물 삭제 시도는 Service에서 예외 처리
            postService.deleteById(id);

        } catch (Exception e) {
            // === 예외 처리 ===
            // 삭제 실패 시 에러 로그 출력
            // 운영환경에서는 적절한 로깅 프레임워크 사용 권장
            System.err.println("게시물 삭제 실패: " + e.getMessage());

            // === 개선 제안 ===
            // 1. 구체적인 예외별 처리:
            //    - SecurityException: 권한 없음 메시지
            //    - EntityNotFoundException: 게시물 없음 메시지
            //    - 기타: 일반적인 오류 메시지
            //
            // 2. 사용자 피드백:
            //    RedirectAttributes로 Flash Message 전달
            //    redirectAttributes.addFlashAttribute("error", "삭제 권한이 없습니다.");
        }

        // === 결과 페이지 리다이렉트 ===
        // 성공/실패와 관계없이 게시물 목록으로 리다이렉트
        // 사용자에게 일관된 경험 제공
        return "redirect:/posts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication) {
        Post post = postService.findById(id);
        if (!post.getAuthor().getUsername().equals(authentication.getName())) {
            return "redirect:/posts/" + id; // 권한 없으면 세부 페이지로 이동
        }
        // form -> audit X
        model.addAttribute("post", post); // binding -> form
        return "post/edit"; // templates/post/edit.html
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute PostDTO.Request dto, Authentication authentication) {
        dto.setUsername(authentication.getName()); // 인증 정보를 바탕으로 편집자 정보를 넣고
        try {
            postService.updatePost(id, dto); // service를 사용해서 수정 저장 처리
        } catch (Exception e) {
            return "redirect:/posts/" + id + "/edit";
        }
        return "redirect:/posts";
    }
}

// === 보안 고려사항 ===

/**
 * 1. 권한 기반 접근 제어:
 * - 게시물 작성: 로그인한 사용자만 가능
 * - 게시물 수정/삭제: 작성자 본인만 가능
 * - 관리자 권한: 모든 게시물 관리 가능 (추후 구현)
 *
 * 2. 입력 검증:
 * - XSS 방지: 게시물 내용 HTML 이스케이프
 * - SQL Injection 방지: JPA 사용으로 기본 보호
 * - 데이터 길이 제한: Entity 제약조건 설정
 *
 * 3. 인증 정보 신뢰:
 * - JWT 토큰에서 추출한 사용자 정보만 신뢰
 * - 클라이언트에서 전송된 username 무시
 */

// === 사용자 경험 개선 ===

/**
 * 1. 피드백 메시지:
 * - 게시물 작성/수정/삭제 성공 시 확인 메시지
 * - 권한 없음, 오류 발생 시 명확한 안내
 *
 * 2. 페이징 처리:
 * - 대량의 게시물에 대한 페이징 구현
 * - Spring Data JPA의 Pageable 활용
 *
 * 3. 검색 및 정렬:
 * - 제목, 내용, 작성자별 검색 기능
 * - 작성일, 조회수, 좋아요 수 등으로 정렬
 */

// === 성능 최적화 ===

/**
 * 1. N+1 문제 해결:
 * - Post와 UserAccount 연관관계 fetch join 사용
 * - @EntityGraph 어노테이션 활용
 *
 * 2. DTO 변환 최적화:
 * - 대량 데이터 처리 시 Stream 병렬 처리 고려
 * - 필요한 필드만 조회하는 Projection 사용
 *
 * 3. 캐싱 적용:
 * - 인기 게시물, 최신 게시물 등 자주 조회되는 데이터 캐싱
 * - @Cacheable 어노테이션 활용
 */