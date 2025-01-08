package com.lee.osakacity.controller;

import com.lee.osakacity.service.PostService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
public class GlobalErrorController implements ErrorController {

    private final PostService postService;

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == 404) {
                return setHtml(model);
            } else if (statusCode == 500) {
                model.addAttribute("title", "서버 오류입니다.");
                model.addAttribute("description", "서버에서 오류가 발생했습니다.");
                model.addAttribute("keywords", "");
                model.addAttribute("siteUrl", "https://houber-japanlife.com");
                model.addAttribute("thumbnail", "https://houber-japanlife.com/asset/logo.png");
                model.addAttribute("all_guide", postService.getGuideOnly(8));
                return "error/500";
            } else {
                return setHtml(model);
            }
        }
        return "error/404";
    }

    private String setHtml(Model model) {
        model.addAttribute("title", "찾을 수 없는 페이지입니다.");
        model.addAttribute("description", "잘못된 페이지로 접속하셨습니다.");
        model.addAttribute("keywords", "");
        model.addAttribute("siteUrl", "https://houber-japanlife.com");
        model.addAttribute("thumbnail", "https://houber-japanlife.com/asset/logo.png");
        model.addAttribute("all_guide", postService.getGuideOnly(8));
        return "error/404";
    }
}
