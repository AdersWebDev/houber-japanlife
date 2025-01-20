package com.lee.osakacity.controller;

import com.lee.osakacity.dto.restful.ImgResponse;
import com.lee.osakacity.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class S3Controller {
    private final S3Service s3Service;
    @PostMapping()
    public ImgResponse uploadFile(@ModelAttribute MultipartFile file) {
        return s3Service.uploadFile(file);
    }
    @DeleteMapping()
    public List<Long> deleteUnUseFile() {
        return s3Service.deleteUnUseFile();
    }
}
