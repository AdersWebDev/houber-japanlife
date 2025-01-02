package com.lee.osakacity.controller;

import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostRestController {
    private final PostService postService;

    @PostMapping()
    public void create(@RequestBody PostRequestDto dto, HttpServletRequest request){
        postService.create(request, dto);
    }
    @PatchMapping()
    public void update(){}
    @DeleteMapping
    public void delete(){}
}
