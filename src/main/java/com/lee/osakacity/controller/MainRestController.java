package com.lee.osakacity.controller;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.SimpleResponse;
import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class MainRestController {
    private final PostService postService;

    @GetMapping("/list")
    public List<SimpleResponse> listPage(@RequestParam Category category,
                                         @RequestParam(required = false) Long cursorId,
                                         @RequestParam(required = false) Integer cursorView,
                                         @RequestParam(required = false) LocalDateTime cursorTime) {

       return postService.getList(category, cursorId, cursorView, cursorTime);

    }

    @PostMapping()
    public void create(@RequestBody PostRequestDto dto, HttpServletRequest request){
        postService.create(request, dto);
    }
    @PatchMapping()
    public void update(){}
    @DeleteMapping
    public void delete(){}
}
