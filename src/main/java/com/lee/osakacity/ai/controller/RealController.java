package com.lee.osakacity.ai.controller;

import com.lee.osakacity.ai.service.RealProService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class RealController {
    private final RealProService realProService;

    @PostMapping
    public void go () {
        realProService.realNetReport();
    }
    @PatchMapping()
    public void detail() {
        realProService.realNetDetail();
    }
}
