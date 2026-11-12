package com.lee.osakacity.controller;

import com.lee.osakacity.admin.AdminAuthService;
import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.PostResponseDto;
import com.lee.osakacity.dto.SnsContentResponseDto;
import com.lee.osakacity.service.PostService;
import com.lee.osakacity.seo.DuplicatePosts;
import com.lee.osakacity.seo.SeoText;
import com.lee.osakacity.service.SnsContentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;


@Controller
@RequiredArgsConstructor
public class MainController {
    private static final String SITE = "https://houber-japanlife.com";
    /** 카카오톡 채널 프로필 주소 (본문 링크는 바로 상담 채팅으로 연결) */
    private static final java.util.regex.Pattern KAKAO_PROFILE =
            java.util.regex.Pattern.compile("^https?://pf\\.kakao\\.com/_qTEPj/?$");
    private final PostService postService;
    private final SnsContentService snsContentService;
    private final AdminAuthService adminAuthService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "하우버 | 일본 생활의 모든 정보를 한눈에");
        model.addAttribute("description", "일본 생활 정보, 워킹홀리데이, 유학등 모든 콘텐츠를 알려드려요!");
        model.addAttribute("keywords", "houber, 하우버, japan-life, 일본, 일본 워홀, 워킹홀리데이, 일본 워킹홀리데이");
        model.addAttribute("siteUrl", SITE + "/"); // 사이트맵의 홈 주소와 같은 형식 (끝 슬래시)
        model.addAttribute("thumbnail","https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("houber_news",postService.getList(Category.houber_news, 10, null, null, null));
        model.addAttribute("japan_review",postService.getReviewList(0, 10));
        model.addAttribute("event",postService.getList(Category.event, 10, null, null, null));
        model.addAttribute("japan_property",postService.getList(Category.japan_property, 10, null, null, null));
        model.addAttribute("working_holiday",postService.getList(Category.working_holiday, 10, null, null, null));
        model.addAttribute("japan_life",postService.getList(Category.japan_life, 10, null, null, null));
        model.addAttribute("new_value",postService.checkUpdate());

        return "index";
    }

    @GetMapping("/list")
    public String listPage(@RequestParam(required = false) String category, Model model, HttpServletResponse response) {
        // 카테고리 없이 들어오면 전체 글 목록으로 영구 이동
        if (category == null || category.isBlank()) {
            return movedPermanently(response, "/list?category=all");
        }
        if ("all".equals(category)) {
            model.addAttribute("title", "전체 글 | " + SeoText.BRAND);
            model.addAttribute("description", "하우버가 전하는 일본 워킹홀리데이, 일본 생활, 일본 부동산 정보를 최신 글부터 모두 모았습니다.");
            model.addAttribute("keywords", "하우버, 일본, 일본 정보, 일본 워홀, 워킹홀리데이, 일본 생활");
            model.addAttribute("siteUrl", SITE + "/list?category=all");
            model.addAttribute("thumbnail","https://houber-japanlife.com/asset/logo.png");
            model.addAttribute("mainContent", postService.getAllList(20, null));
            model.addAttribute("content_h1", "전체 글");
            model.addAttribute("content_p", "하우버의 모든 일본 생활 정보를 최신 글부터 모았습니다.");
            model.addAttribute("new_value",postService.checkUpdate());
            return "listPage";
        }
        Category cat;
        try {
            cat = Category.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND); // 없는 카테고리
        }

        model.addAttribute("title", cat.getTitle() + " | " + SeoText.BRAND);
        model.addAttribute("description", cat.getTitle() +"에서 최신 일본 워킹홀리데이 및 생활 정보를 확인하세요. 하우버가 전하는 생생한 포스트를 만나보세요!");
        model.addAttribute("keywords", "하우버, "+cat.getTitle()+", 일본, 일본 정보, 일본 워홀, 워킹홀리데이");
        model.addAttribute("siteUrl", SITE + "/list?category="+cat);
        model.addAttribute("thumbnail","https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("mainContent",postService.getList(cat, 20, null, null, null));
        model.addAttribute("content_h1",cat.getTitle());
        model.addAttribute("content_p",cat.getDescription());
        model.addAttribute("new_value",postService.checkUpdate());

        return "listPage";
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam String keyword, Model model) {
        model.addAttribute("title", "'" + keyword + "' 검색 결과 | " + SeoText.BRAND);
        model.addAttribute("description", "'" + keyword + "' 검색 결과 - 하우버");
        model.addAttribute("keywords", "houber, 하우버, japan-life, 일본, 일본 워홀, 워킹홀리데이, 일본 워킹홀리데이");
        model.addAttribute("siteUrl", SITE + "/search");
        model.addAttribute("noindex", true); // 검색 결과 페이지는 검색엔진에 올리지 않음
        model.addAttribute("thumbnail","https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("search_keyword",keyword);
        model.addAttribute("searched_list",postService.search(keyword, 20, null));
        model.addAttribute("new_value",postService.checkUpdate());

        return "searchPage";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model, HttpServletRequest request, HttpServletResponse response) {
        // 같은 내용으로 여러 번 올라간 글은 남길 글로 영구 이동
        Long target = DuplicatePosts.REDIRECTS.get(id);
        if (target != null) {
            return movedPermanently(response, "/detail/" + target);
        }

        // 숨김 글은 관리자만 볼 수 있음 (글쓰기 미리보기). 방문자에게 세션을 새로 만들지 않도록 getSession(false)
        PostResponseDto dto = postService.getDetail(id, adminAuthService.isLoggedIn(request.getSession(false)));
        model.addAttribute("title", SeoText.pageTitle(dto.getTitle()));
        model.addAttribute("description", SeoText.metaDescription(dto.getDescription(), dto.getContent()));
        model.addAttribute("keywords", dto.getKeyword());
        model.addAttribute("siteUrl", SITE + "/detail/"+id);
        model.addAttribute("thumbnail",dto.getThumbnailUrl());
        model.addAttribute("ogType", "article");
        model.addAttribute("h1", SeoText.cleanTitle(dto.getTitle()));
        model.addAttribute("category", dto.getCategory());
        model.addAttribute("view",dto.getView());
        model.addAttribute("new_value",postService.checkUpdate());

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

            // 본문 이미지: 첫 이미지는 바로, 나머지는 화면에 가까워질 때 불러옴
            Elements images = doc.select("img");
            for (int i = 0; i < images.size(); i++) {
                if (i > 0) images.get(i).attr("loading", "lazy");
                images.get(i).attr("decoding", "async");
            }
            // 본문의 카카오톡 채널 링크는 프로필 대신 상담 채팅으로 바로 연결
            for (Element link : doc.select("a[href]")) {
                if (KAKAO_PROFILE.matcher(link.attr("href").trim()).matches()) {
                    link.attr("href", "https://pf.kakao.com/_qTEPj/chat");
                }
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

        model.addAttribute("title", SeoText.pageTitle(dto.getTitle()));
        model.addAttribute("description", SeoText.metaDescription(dto.getDescription(), null));
        model.addAttribute("ogType", "article");
        model.addAttribute("keywords", dto.getKeyword());
        model.addAttribute("siteUrl", SITE + "/detail/sns-content/"+id);
        model.addAttribute("thumbnail", dto.getThumbnailUrl());
        model.addAttribute("new_value",postService.checkUpdate());

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

    /** 301 영구 이동. nginx 뒤에서도 https 주소가 그대로 나가도록 전체 주소를 직접 넣는다 */
    private static String movedPermanently(HttpServletResponse response, String path) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", SITE + path);
        return null;
    }
}
