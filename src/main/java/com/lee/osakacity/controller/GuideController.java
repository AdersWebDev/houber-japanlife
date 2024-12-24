package com.lee.osakacity.controller;

import com.lee.osakacity.dto.Category;
import com.lee.osakacity.dto.ContentHeading;
import com.lee.osakacity.dto.GuideResponseDto;
import com.lee.osakacity.dto.GuideSimpleResponse;
import com.lee.osakacity.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/guide")
@RequiredArgsConstructor
public class GuideController {
    private final GuideService guideService;

    @GetMapping("/more")
    public String getMoreGuide (@RequestParam Category category, Model model) {
        List<GuideSimpleResponse> resultDto = guideService.getMoreGuide(category);
        model.addAttribute("moreContents", List.of(
                new GuideSimpleResponse("인기글 1", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 2", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1"),
                new GuideSimpleResponse("인기글 3", "https://via.placeholder.com/120x80", "/guide/detail/1")

                ));
        return "listGuide";
    }
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model) {

        GuideResponseDto dto = guideService.getDetailPage(id);

        model.addAttribute("pageDescription", dto.getDescription());
        model.addAttribute("pageKeywords", dto.getKeyword());
        model.addAttribute("pageContent", dto.getContent());

        // Jsoup으로 HTML 파싱
        Document doc = Jsoup.parse(dto.getContent());
        Elements headings = doc.select("h2, h3");

        List<ContentHeading> headingList = new ArrayList<>();
        int counter = 1;

        // h2, h3 태그에 ID 부여 및 리스트 생성
        for (Element heading : headings) {
            String contentId = "heading-" + counter++; // 고유 ID 생성
            heading.attr("id", contentId); // HTML 태그에 ID 추가

            ContentHeading h = ContentHeading.builder()
                    .id(contentId)
                    .text(heading.text())
                    .level(heading.tagName())
                    .build();

            headingList.add(h);
        }

        // 수정된 HTML
        model.addAttribute("pageContent", doc.body().html());
        model.addAttribute("headings", headingList);

//        model.addAttribute("pageTitle", "타이틀 입니다.");
//        model.addAttribute("pageDescription", "설명부분으로 들어갈예정");
//        model.addAttribute("pageKeywords","안녕, 워홀, 부동산, 기모찌");

        return "detail-guide";
    }
}
