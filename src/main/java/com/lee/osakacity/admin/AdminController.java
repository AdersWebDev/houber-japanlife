package com.lee.osakacity.admin;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.infra.entity.Post;
import com.lee.osakacity.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 관리자 화면 (로그인, 글 관리, 글쓰기) */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
    private final AdminAuthService auth;
    private final AdminPostService adminPostService;
    private final PostService postService;

    @GetMapping
    public String home() {
        return "redirect:/admin/posts";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (auth.isLoggedIn(session)) return "redirect:/admin/posts";
        model.addAttribute("configured", auth.isConfigured());
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(required = false) String password, HttpServletRequest request, Model model) {
        String error = auth.login(request, password);
        if (error == null) return "redirect:/admin/posts";
        model.addAttribute("configured", auth.isConfigured());
        model.addAttribute("error", error);
        return "admin/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    @GetMapping("/posts")
    public String posts(@RequestParam(required = false) Category category, HttpSession session, Model model) {
        model.addAttribute("rows", adminPostService.list(category));
        model.addAttribute("selected", category);
        model.addAttribute("categories", Category.values());
        model.addAttribute("token", auth.token(session));
        return "admin/posts";
    }

    @GetMapping("/write")
    public String write(HttpSession session, Model model) {
        return writePage(null, session, model);
    }

    @GetMapping("/write/{id}")
    public String edit(@PathVariable Long id, HttpSession session, Model model) {
        return writePage(adminPostService.get(id), session, model);
    }

    /**
     * 미리보기: 저장하지 않은 글을 실제 상세 페이지 모양으로 보여줌 (새 창)
     */
    @PostMapping("/preview")
    public String preview(@RequestParam(required = false) String title, @RequestParam(required = false) Category category,
                          @RequestParam(required = false) String content, Model model) {
        String safe = HtmlSanitizer.clean(content);
        Category c = category == null ? Category.houber_news : category;
        String t = title == null || title.isBlank() ? "(제목 없음)" : title.trim();
        model.addAttribute("title", "[미리보기] " + t);
        model.addAttribute("description", "미리보기");
        model.addAttribute("keywords", "");
        model.addAttribute("siteUrl", "https://houber-japanlife.com/");
        model.addAttribute("thumbnail", "https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("h1", t);
        model.addAttribute("category", c);
        model.addAttribute("view", 0);
        model.addAttribute("new_value", postService.checkUpdate());

        List<Map<String, String>> toc = new ArrayList<>();
        Document doc = Jsoup.parseBodyFragment(safe);
        int index = 0;
        for (Element head : doc.select("h2, h3")) {
            ++index;
            head.attr("id", "header" + index);
            toc.add(Map.of("id", "header" + index, "text", head.tagName().equals("h2") ? head.text() : "- " + head.text()));
        }
        model.addAttribute("mainContent", doc.body().html());
        model.addAttribute("tableOfContents", toc);
        model.addAttribute("moreContents", List.of());
        return "detail";
    }

    private String writePage(Post post, HttpSession session, Model model) {
        model.addAttribute("post", post);
        model.addAttribute("categories", Category.values());
        model.addAttribute("defaultCategory", Category.houber_news);
        model.addAttribute("token", auth.token(session));
        return "admin/write";
    }
}
