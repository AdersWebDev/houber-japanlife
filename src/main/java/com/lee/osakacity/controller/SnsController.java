package com.lee.osakacity.controller;

import com.lee.osakacity.service.SnsContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/update")

public class SnsController {
    private final SnsContentService snsContentService;
    @PostMapping("/youtube")
    public void youtubeUpdate(@RequestParam String channelId) {
        snsContentService.youtubeUpdate(channelId);
    }

}
