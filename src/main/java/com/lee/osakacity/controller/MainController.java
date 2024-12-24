package com.lee.osakacity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MainController {
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "메인 페이지");
        model.addAttribute("pageDescription", "여기는 메인 페이지입니다.");
        model.addAttribute("pageKeywords", "메인, Thymeleaf, Spring Boot");

        // 인기 게시글 관련 데이터 추가
//        model.addAttribute("popularPosts", List.of(
//                new GuideSimpleResponse("인기글 1", "https://via.placeholder.com/120x80", "/post/1"),
//                new GuideSimpleResponse("인기글 2", "https://via.placeholder.com/120x80", "/post/2"),
//                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/post/3"),
//                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/post/3"),
//                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/post/3"),
//                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/post/3")
//        ));
//
//        // 가이드 관련 데이터 추가
//        model.addAttribute("guides", List.of(
//                new GuideSimpleResponse("워홀 가이드 1", "https://via.placeholder.com/120x80", "/guide/detail/1"),
//                new GuideSimpleResponse("취업 가이드 1", "https://via.placeholder.com/120x80", "/guide/detail/1"),
//                new GuideSimpleResponse("취업 가이드 2", "https://via.placeholder.com/120x80", "/guide/detail/1"),
//                new GuideSimpleResponse("워홀 가이드 1", "https://via.placeholder.com/120x80", "/guide/detail/1"),
//                new GuideSimpleResponse("취업 가이드 1", "https://via.placeholder.com/120x80", "/guide/detail/1"),
//                new GuideSimpleResponse("취업 가이드 2", "https://via.placeholder.com/120x80", "/guide/detail/1")
//
//        ));
//
//        // 생활 컨텐츠 데이터 추가
//        model.addAttribute("lifeContents", List.of(
//                new GuideSimpleResponse("생활 콘텐츠 1", "https://via.placeholder.com/120x80", "/life/1"),
//                new GuideSimpleResponse("생활 콘텐츠 2", "https://via.placeholder.com/120x80", "/life/2"),
//                new GuideSimpleResponse("생활 콘텐츠 3", "https://via.placeholder.com/120x80", "/life/3"),
//                new GuideSimpleResponse("생활 콘텐츠 1", "https://via.placeholder.com/120x80", "/life/1"),
//                new GuideSimpleResponse("생활 콘텐츠 2", "https://via.placeholder.com/120x80", "/life/2"),
//                new GuideSimpleResponse("생활 콘텐츠 3", "https://via.placeholder.com/120x80", "/life/3")
//        ));
        return "index";
    }
}
