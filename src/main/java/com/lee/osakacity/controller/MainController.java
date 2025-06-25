package com.lee.osakacity.controller;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.PostResponseDto;
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
        model.addAttribute("title", "하우버 | 일본 생활의 모든 정보를 한눈에");
        model.addAttribute("description", "일본 생활 정보, 워킹홀리데이, 유학등 모든 콘텐츠를 알려드려요!");
        model.addAttribute("keywords", "houber, 하우버, japan-life, 일본, 일본 워홀, 워킹홀리데이, 일본 워킹홀리데이");
        model.addAttribute("siteUrl", "https://houber-japanlife.com");
        model.addAttribute("thumbnail","https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("japan_review",postService.getList(Category.japan_review, 10, null, null, null));
        model.addAttribute("event",postService.getList(Category.event, 10, null, null, null));
        model.addAttribute("japan_property",postService.getList(Category.japan_property, 10, null, null, null));
        model.addAttribute("working_holiday",postService.getList(Category.working_holiday, 10, null, null, null));
        model.addAttribute("japan_life",postService.getList(Category.japan_life, 10, null, null, null));

        return "index";
    }

    @GetMapping("/list")
    public String listPage(@RequestParam Category category, Model model) {

        model.addAttribute("title",  "하우버 | " + category.getTitle() + "콘텐츠 보기");
        model.addAttribute("description", category.getTitle() +"에서 최신 일본 워킹홀리데이 및 생활 정보를 확인하세요. 하우버가 전하는 생생한 포스트를 만나보세요!");
        model.addAttribute("keywords", "하우버, "+category.getTitle()+", 일본, 일본 정보, 일본 워홀, 워킹홀리데이");
        model.addAttribute("siteUrl", "https://houber-japanlife.com/list?category="+category);
        model.addAttribute("thumbnail","https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("mainContent",postService.getList(category, 20, null, null, null));
        model.addAttribute("content_h1",category.getTitle());
        model.addAttribute("content_p",category.getDescription());

        return "listPage";
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam String keyword, Model model) {
        model.addAttribute("title", "하우버 |" +keyword +"검색결과");
        model.addAttribute("description", "houber"+keyword+"검색 결과입니다");
        model.addAttribute("keywords", "houber, 하우버, japan-life, 일본, 일본 워홀, 워킹홀리데이, 일본 워킹홀리데이");
        model.addAttribute("siteUrl", "https://houber-japanlife.com/search?keyword=houber");
        //위에 cancol 수정
        model.addAttribute("thumbnail","https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("search_keyword",keyword);
        model.addAttribute("searched_list",postService.search(keyword, 20, null));

        return "searchPage";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model) {

        PostResponseDto dto = postService.getDetail(id);
        model.addAttribute("title",dto.getTitle());
        model.addAttribute("description", "houber-"+dto.getDescription());
        model.addAttribute("keywords", dto.getKeyword());
        model.addAttribute("siteUrl", "https://houber-japanlife.com/detail/"+id);
        model.addAttribute("thumbnail",dto.getThumbnailUrl());
        model.addAttribute("h1",dto.getTitle());
        model.addAttribute("category", dto.getCategory());
        model.addAttribute("view",dto.getView());

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
                    headingMap.put("text", head.text()); // h2는 번호 추가
                } else {
                    headingMap.put("text", "- " + head.text()); // h3는 - 추가
                }

                toc.add(headingMap);
            }
            model.addAttribute("mainContent", doc.body().html());
        }

        model.addAttribute("tableOfContents", toc);
        model.addAttribute("moreContents", postService.moreContents(dto.getCategory(), dto.getId()));
        return "detail";
    }
    @GetMapping("/detail/sns-content/{id}")
    public String detailSnsPage(@PathVariable Long id, Model model) {
        SnsContentResponseDto dto = snsContentService.getDetail(id);

        model.addAttribute("title", "하우버 | "+dto.getTitle());
        model.addAttribute("description","houber | "+ dto.getDescription());
        model.addAttribute("keywords", dto.getKeyword());
        model.addAttribute("siteUrl", "https://houber-japanlife.com/detail/sns-content/"+id);
        model.addAttribute("thumbnail", dto.getThumbnailUrl());

        model.addAttribute("url", "https://www.youtube.com/embed/"
                +dto.getContent()
                +"?loop=1&playlist=" + dto.getContent()
                + "&modestbranding=1&rel=0&iv_load_policy=3"
        );
        model.addAttribute("class", dto.getSnsCategory().getClassValue());
        model.addAttribute("moreContents", snsContentService.moreContents(dto.getId()));
//        if (dto.getSnsCategory().equals(SnsCategory.INSTAGRAM))
//            return "instagramPage";
//        else
            return "youtubePage";

    }

}
