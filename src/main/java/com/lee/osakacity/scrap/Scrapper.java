package com.lee.osakacity.scrap;

import com.lee.osakacity.custom.Category;

import com.lee.osakacity.dto.restful.PostRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/scarp")
public class Scrapper {
    private final ScrapService scrapService;
    @PostMapping("/blog")
    public PostRequestDto houberBlogUpdate(@RequestParam long id, @RequestParam Category category, HttpServletRequest request) throws IOException {
        return scrapService.houberBlogScrapper(id, category, request);
    }
}
