package com.lee.osakacity.controller;

import com.lee.osakacity.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class S3Controller {
    private final S3Service s3Service;
    @PostMapping()
    public String uploadFile(@ModelAttribute MultipartFile file) {
        return s3Service.uploadFile(file);
    }
}
