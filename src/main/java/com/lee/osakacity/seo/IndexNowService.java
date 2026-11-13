package com.lee.osakacity.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * IndexNow: 글을 발행·수정·숨김·삭제하면 Bing·네이버 등 검색엔진에 바뀐 주소를 바로 알린다. (구글은 사용하지 않음)
 * 키는 서버의 admin.yml에 indexnow.key 로 둔다. 키가 없으면 아무것도 하지 않는다 (로컬 개발 등).
 * 키 파일은 https://houber-japanlife.com/{키}.txt 로 공개된다 (IndexNow 규칙: 검색엔진이 키 주인을 확인하는 용도).
 */
@Slf4j
@Service
public class IndexNowService {
    private static final Pattern VALID_KEY = Pattern.compile("^[a-zA-Z0-9-]{8,128}$");
    private static final String HOST = "houber-japanlife.com";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String key;
    private final String endpoint;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .proxy(ProxySelector.getDefault())
            .build();

    public IndexNowService(@Value("${indexnow.key:}") String key,
                           @Value("${indexnow.endpoint:https://api.indexnow.org/indexnow}") String endpoint) {
        String k = key == null ? "" : key.trim();
        if (!k.isEmpty() && !VALID_KEY.matcher(k).matches()) {
            log.warn("IndexNow: indexnow.key 형식이 올바르지 않아 사용하지 않습니다 (영문·숫자·- 8~128자)");
            k = "";
        }
        this.key = k;
        this.endpoint = endpoint;
    }

    public boolean enabled() {
        return !key.isEmpty();
    }

    /** 키 파일 주소용: 요청한 키가 설정된 키와 같은지 */
    public boolean isKey(String candidate) {
        return enabled() && key.equals(candidate);
    }

    public String key() {
        return key;
    }

    /**
     * 주소(사이트 안 경로, 예: "/detail/121")를 알린다.
     * DB 저장이 끝난(커밋된) 뒤에 보내고, 응답을 기다리지 않는다 (관리자 화면이 느려지지 않게).
     */
    public void notifyChanged(String... paths) {
        if (!enabled() || paths.length == 0) return;
        List<String> urls = java.util.Arrays.stream(paths).map(p -> "https://" + HOST + p).distinct().toList();
        Runnable send = () -> send(urls);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private void send(List<String> urls) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("host", HOST);
            body.put("key", key);
            body.put("keyLocation", "https://" + HOST + "/" + key + ".txt");
            body.put("urlList", urls);
            HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                    .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((res, err) -> {
                        if (err != null) log.warn("IndexNow 전송 실패 {}: {}", urls, err.toString());
                        else if (res.statusCode() == 200 || res.statusCode() == 202) log.info("IndexNow 전송 {} → {}", urls, res.statusCode());
                        else log.warn("IndexNow 응답 {} {}", res.statusCode(), urls);
                    });
        } catch (Exception e) {
            log.warn("IndexNow 전송 준비 실패: {}", e.toString());
        }
    }
}
