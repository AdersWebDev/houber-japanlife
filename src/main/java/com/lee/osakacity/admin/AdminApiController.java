package com.lee.osakacity.admin;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.restful.ImgResponse;
import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.scrap.ScrapService;
import com.lee.osakacity.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 관리자 글쓰기 화면이 쓰는 API (AdminInterceptor가 로그인·토큰을 확인한 뒤에만 들어옴) */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api")
public class AdminApiController {
    private static final int MAX_UPLOAD = 8 * 1024 * 1024;
    private static final Pattern BLOG_ID = Pattern.compile("(?:blog\\.naver\\.com/houber/|logNo=)(\\d{6,})|^(\\d{6,})$");

    private final AdminPostService adminPostService;
    private final S3Service s3Service;
    private final ScrapService scrapService;

    /** 사진 올리기 (브라우저에서 화면용 크기로 줄인 파일) */
    @PostMapping("/image")
    public ImgResponse upload(@RequestParam MultipartFile file, @RequestParam(required = false) String alt) throws Exception {
        if (file.isEmpty() || file.getSize() > MAX_UPLOAD) throw bad("사진은 8MB 이하만 올릴 수 있습니다");
        byte[] data = file.getBytes();
        String type = AdminImageTypes.sniff(data);
        if (type == null) throw bad("JPG, PNG, WEBP, GIF 사진만 올릴 수 있습니다");
        return s3Service.uploadBytes(data, type, AdminImageTypes.ext(type), alt);
    }

    /** 가져오기: 인터넷 주소의 사진을 대신 받아 브라우저로 넘김 (브라우저가 크기를 줄인 뒤 /image로 올림) */
    @GetMapping("/fetch-image")
    public ResponseEntity<byte[]> fetchImage(@RequestParam String url) throws Exception {
        ImageFetcher.Fetched f = ImageFetcher.fetch(url);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType())).body(f.data());
    }

    @PostMapping("/posts")
    public Map<String, Long> create(@RequestBody AdminPostRequest req) {
        return Map.of("id", adminPostService.save(null, req));
    }

    @PutMapping("/posts/{id}")
    public Map<String, Long> update(@PathVariable Long id, @RequestBody AdminPostRequest req) {
        return Map.of("id", adminPostService.save(id, req));
    }

    @PatchMapping("/posts/{id}/show")
    public Map<String, Boolean> show(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean show = Boolean.TRUE.equals(body.get("show"));
        adminPostService.setShow(id, show);
        return Map.of("show", show);
    }

    @DeleteMapping("/posts/{id}")
    public Map<String, Boolean> delete(@PathVariable Long id) {
        adminPostService.delete(id);
        return Map.of("deleted", true);
    }

    /** 가져오기: 하우버 네이버 블로그 글을 사이트 형식으로 변환 (저장은 하지 않고 편집기로 보냄) */
    @PostMapping("/import/blog")
    public Map<String, Object> importBlog(@RequestBody Map<String, String> body) throws Exception {
        String url = body.getOrDefault("url", "").trim();
        Matcher m = BLOG_ID.matcher(url);
        if (!m.find()) throw bad("하우버 블로그 글 주소를 넣어 주세요 (예: https://blog.naver.com/houber/224384295502)");
        long id = Long.parseLong(m.group(1) != null ? m.group(1) : m.group(2));
        PostRequestDto dto;
        try {
            dto = scrapService.parseBlog(id, Category.houber_news);
        } catch (Exception e) {
            log.warn("blog import failed: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "네이버 블로그에서 글을 가져오지 못했습니다. 주소를 확인하거나 잠시 후 다시 시도해 주세요. (" + e.getClass().getSimpleName() + ")");
        }
        return Map.of(
                "title", dto.getTitle() == null ? "" : dto.getTitle(),
                "content", HtmlSanitizer.clean(dto.getContent()),
                "description", dto.getDescription() == null ? "" : dto.getDescription(),
                "thumbnailUrl", dto.getThumbnailUrl() == null ? "" : dto.getThumbnailUrl(),
                "images", dto.getImgList().size());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> onStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "요청을 처리하지 못했습니다" : e.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> onError(Exception e) {
        log.error("admin api error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "서버 오류: " + e.getClass().getSimpleName()));
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
