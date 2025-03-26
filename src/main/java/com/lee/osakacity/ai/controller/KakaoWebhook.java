package com.lee.osakacity.ai.controller;

import com.lee.osakacity.ai.infra.repo.KakaoRepo;
import com.lee.osakacity.ai.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoWebhook {
    private final KakaoRepo kakaoRepo;
    private final SearchService searchService;


    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> init(@RequestBody Map<String, Object> payload) {
        return searchService.userInit(payload);
    }

    @PostMapping("/point")
    public ResponseEntity<Map<String,Object>>  filter (@RequestBody Map<String, Object> payload) {
        return searchService.callBack(payload);
    }
    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> start (@RequestBody Map<String, Object> payload) {
        return searchService.searchStart(payload);
    }
    @PostMapping("/detail")
    public ResponseEntity<Map<String,Object>> detail (@RequestBody Map<String, Object> payload) {
        return searchService.detail(payload);
    }
    @PostMapping("/detail/photo")
    public ResponseEntity<Map<String,Object>> detailPhoto (@RequestBody Map<String, Object> payload) {
        return searchService.morePhoto(payload);
    }

//    @PostMapping("/reset")
//    public ResponseEntity<Map<String, Object>> reset (@RequestBody Map<String, Object> payload) {
//        return searchService.reset(payload);
//    }
}
