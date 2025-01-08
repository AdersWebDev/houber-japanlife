package com.lee.osakacity.controller;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.SearchResponseDto;
import com.lee.osakacity.dto.mvc.SimpleResponse;
import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.service.PostService;
import com.lee.osakacity.service.SitemapService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequiredArgsConstructor
public class MainRestController {
    private final PostService postService;
    private final SitemapService sitemapService;

    @GetMapping("/post/list")
    public List<SimpleResponse> listPage(@RequestParam Category category,
                                         @RequestParam(required = false) Long cursorId,
                                         @RequestParam(required = false) Integer cursorView,
                                         @RequestParam(required = false) LocalDateTime cursorTime) {

       return postService.getList(category,20, cursorId, cursorView, cursorTime);

    }
    @GetMapping("/post/search")
    public List<SearchResponseDto> searchPage(@RequestParam String keyword, @RequestParam LocalDateTime cursorTime) {
        return postService.search(keyword, 20, cursorTime);
    }

    @PostMapping("/post")
    public void create(@RequestBody PostRequestDto dto, HttpServletRequest request){
        postService.create(request, dto);
    }
    @PatchMapping("/post")
    public void update(){}
    @DeleteMapping("/post")
    public void delete(){}

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    @ResponseBody
    public String sitemap() {
        return sitemapService.makeSiteMap();
    }
    @GetMapping(value = "/rss.xml", produces = "application/xml")
    @ResponseBody
    public String rssFeed() {
        return sitemapService.makeRss();
    }
}
