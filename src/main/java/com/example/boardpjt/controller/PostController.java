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

@Controller
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final UserAccountService userAccountService;

    /**
     * 게시물 목록 페이지
     *
     * 변경사항:
     * - @RequestParam String searchType 추가 ★
     *   (searchType: "title" = 제목+내용, "author" = 작성자)
     */
    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "title") String searchType) { // ★ 검색 타입 추가

        if (!StringUtils.hasText(keyword)) {
            keyword = "";
        }

        if (!StringUtils.hasText(category)) {
            category = "";
        }

        // ★ searchType 검증 (title 또는 author만 허용)
        if (!searchType.equals("title") && !searchType.equals("author")) {
            searchType = "title";
        }

        // ★ 검색 타입 포함해서 검색
        Page<Post> postPage = postService.findWithPagingAndSearch(
                keyword, category, searchType, page - 1);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("searchType", searchType); // ★ 선택된 검색 타입 유지

        // ★ 검색 타입 선택지
        model.addAttribute("searchTypes", new String[]{"title", "author"});
        model.addAttribute("searchTypeLabels", new String[]{"제목+내용", "작성자"});

        model.addAttribute("categories", new String[]{"국내", "해외"});

        model.addAttribute("posts",
                postPage.getContent()
                        .stream()
                        .map(p -> new PostDTO.Response(
                                p.getId(),
                                p.getTitle(),
                                p.getContent(),
                                p.getAuthor().getUsername(),
                                p.getCreatedAt().toString(),
                                p.getCategory()
                        ))
                        .toList()
        );

        return "post/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                         Authentication authentication) {
        UserAccount userAccount = userAccountService.findByUsername(authentication.getName());
        Post post = postService.findById(id);
        boolean followCheck = post.getAuthor().getFollowers().contains(userAccount);

        model.addAttribute("followCheck", followCheck);
        model.addAttribute("post", post);

        return "post/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model, Authentication authentication) {

        PostDTO.Request dto = new PostDTO.Request();
        dto.setUsername(authentication.getName());

        model.addAttribute("categories", new String[]{"국내", "해외"});
        model.addAttribute("post", dto);

        return "post/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute PostDTO.Request dto, Authentication authentication) {

        dto.setUsername(authentication.getName());
        postService.createPost(dto);

        return "redirect:/posts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {

        try {
            Post post = postService.findById(id);
            String postUsername = post.getAuthor().getUsername();

            if (!postUsername.equals(authentication.getName())) {
                throw new SecurityException("삭제 권한이 없습니다.");
            }

            postService.deleteById(id);

        } catch (Exception e) {
            System.err.println("게시물 삭제 실패: " + e.getMessage());
        }

        return "redirect:/posts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication authentication) {
        Post post = postService.findById(id);
        if (!post.getAuthor().getUsername().equals(authentication.getName())) {
            return "redirect:/posts/" + id;
        }

        model.addAttribute("categories", new String[]{"국내", "해외"});
        model.addAttribute("post", post);

        return "post/edit";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute PostDTO.Request dto, Authentication authentication) {
        dto.setUsername(authentication.getName());
        try {
            postService.updatePost(id, dto);
        } catch (Exception e) {
            return "redirect:/posts/" + id + "/edit";
        }
        return "redirect:/posts";
    }
}