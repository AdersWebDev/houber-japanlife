package com.lee.osakacity.controller;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.PostResponseDto;
import com.lee.osakacity.custom.SnsCategory;
import com.lee.osakacity.dto.SnsContentResponseDto;
import com.lee.osakacity.service.PostService;
import com.lee.osakacity.service.SnsContentService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;


@Controller
@RequiredArgsConstructor
public class MainController {
    private final PostService postService;
    private final SnsContentService snsContentService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "메인 페이지");
        model.addAttribute("description", "여기는 메인 페이지입니다.");
        model.addAttribute("keywords", "메인, Thymeleaf, Spring Boot");
        model.addAttribute("siteUrl", "https://houber-japanlife.com");
        model.addAttribute("thumbnail","https://houber-japanlife.com/favicon.png");

        return "index";
    }
//    @GetMapping("/{category}/list")
//    public String listPage(@PathVariable String category, Model model) {
//        Category enumCategory = this.convertCategory(category);
//        model.addAttribute("title", "리스트 페이지");
//        model.addAttribute("description", "여기는 메인 페이지입니다.");
//        model.addAttribute("keywords", "메인, Thymeleaf, Spring Boot");
//        model.addAttribute("mainContent",postService.getList(enumCategory, null, null));
//
//        return "listPage";
//    }
    @GetMapping("/{category}/list")
    public String listPage(@PathVariable String category,
                           @RequestParam(required = false) Long cursorId,
                           @RequestParam(required = false) Integer cursorView,
                           Model model) {
        Category enumCategory = this.convertCategory(category);
        model.addAttribute("title", "리스트 페이지");
        model.addAttribute("description", "여기는 메인 페이지입니다.");
        model.addAttribute("keywords", "메인, Thymeleaf, Spring Boot");
        model.addAttribute("siteUrl", "https://houber-japanlife.com/"+category+"/list");
        model.addAttribute("thumbnail","https://houber-japanlife.com/favicon.png");
        model.addAttribute("mainContent",postService.getList(enumCategory, cursorId, cursorView));

        return "listPage";
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam String keyword, Model model) {
        model.addAttribute("title", "메인 페이지");
        model.addAttribute("description", "여기는 메인 페이지입니다.");
        model.addAttribute("keywords", "메인, Thymeleaf, Spring Boot");
        model.addAttribute("siteUrl", "https://houber-japanlife.com/search");
        model.addAttribute("thumbnail","https://houber-japanlife.com/favicon.png");
        return "searchPage";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model) {

        PostResponseDto dto = postService.getDetail(id);
        model.addAttribute("title", dto.getTitle());
        model.addAttribute("description", dto.getDescription());
        model.addAttribute("keywords", dto.getKeyword());
        model.addAttribute("siteUrl", "https://houber-japanlife.com/detail/"+id);
        model.addAttribute("thumbnail",dto.getThumbnailUrl());

        List<Map<String, String>> toc = new ArrayList<>();

        if (dto.getContent() != null && !dto.getContent().isEmpty()) {
            Document doc = Jsoup.parse(dto.getContent());
            Elements headings = doc.select("h2, h3");

            int index = 0;
            for (Element head : headings) {
                ++index;
                head.attr("id", "header" + index); // ID 속성 추가

                Map<String, String> headingMap = new HashMap<>();
                headingMap.put("id", "header"+index); // ID 추가

                if (head.tagName().equals("h2")) {
                    headingMap.put("text", index + ". " + head.text()); // h2는 번호 추가
                } else {
                    headingMap.put("text", "- " + head.text()); // h3는 - 추가
                }

                toc.add(headingMap);
            }
            model.addAttribute("mainContent", doc.body().html());
        }

        model.addAttribute("tableOfContents", toc);
        return "detail";
    }
    @GetMapping("/detail/sns-content/{id}")
    public String detailSnsPage(@PathVariable Long id, Model model) {
        SnsContentResponseDto dto = snsContentService.getDetail(id);

        model.addAttribute("title", dto.getTitle());
        model.addAttribute("description", dto.getDescription());
        model.addAttribute("keywords", dto.getKeyword());
        model.addAttribute("siteUrl", "https://houber-japanlife.com/detail/sns-content/"+id);
        model.addAttribute("thumbnail", dto.getThumbnailUrl());

        model.addAttribute("url", "https://www.youtube.com/embed/"
                +dto.getContent()
                +"?modestbranding=1&rel=0&iv_load_policy=3"
        );
        model.addAttribute("class", dto.getSnsCategory().getClassValue());

        if (dto.getSnsCategory().equals(SnsCategory.INSTAGRAM))
            return "instagramPage";
        else
            return "youtubePage";

    }
    private Category convertCategory (String category) {
        return switch (category) {
            case "hot-post" : yield Category.HOT_POST;
            case "working-holiday": yield Category.WORKING_HOLIDAY;
            case "japan-study": yield Category.JAPAN_STUDY;
            case "japan-life": yield Category.JAPAN_LIFE;
            case "houber-sns" : yield Category.HOUBER_SNS_CONTENT;
            case "view-all": yield Category.VIEW_ALL;
            default: throw new IllegalArgumentException();
        };
    }


}
