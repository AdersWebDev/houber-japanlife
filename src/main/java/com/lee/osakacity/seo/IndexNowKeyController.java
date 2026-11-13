package com.lee.osakacity.seo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** IndexNow 키 파일: https://houber-japanlife.com/{키}.txt → 내용은 키 그대로 */
@RestController
@RequiredArgsConstructor
public class IndexNowKeyController {
    private final IndexNowService indexNow;

    @GetMapping(value = "/{key:[a-zA-Z0-9-]{8,128}}.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> keyFile(@PathVariable String key) {
        if (!indexNow.isKey(key)) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(indexNow.key());
    }
}
