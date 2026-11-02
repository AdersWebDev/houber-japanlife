package com.lee.osakacity.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 가져오기에서 인터넷 주소로 된 사진을 대신 받아온다 (브라우저가 직접 못 받는 사진을 크기 줄이기 전에 받기 위함)
 * 서버 내부 주소(localhost, 사설망, AWS 메타데이터 등)로는 요청하지 않는다
 */
public final class ImageFetcher {
    public static final int MAX_BYTES = 15 * 1024 * 1024;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .proxy(ProxySelector.getDefault())
            .build();

    public record Fetched(byte[] data, String contentType) {}

    private ImageFetcher() {}

    public static Fetched fetch(String url) throws IOException, InterruptedException {
        URI uri = check(url);
        for (int hop = 0; hop < 4; hop++) {
            HttpRequest req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0 (houber-japanlife image import)").GET().build();
            HttpResponse<InputStream> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
            int code = res.statusCode();
            if (code >= 300 && code < 400) {
                res.body().close();
                String loc = res.headers().firstValue("Location").orElseThrow(() -> bad("이동할 주소가 없습니다"));
                uri = check(uri.resolve(loc).toString());
                continue;
            }
            if (code != 200) { res.body().close(); throw bad("사진을 받지 못했습니다 (HTTP " + code + ")"); }
            String type = res.headers().firstValue("Content-Type").orElse("").toLowerCase();
            try (InputStream in = res.body()) {
                byte[] data = readLimited(in);
                String sniffed = AdminImageTypes.sniff(data);
                if (sniffed == null) throw bad("사진 파일이 아닙니다 (" + type + ")");
                return new Fetched(data, sniffed);
            }
        }
        throw bad("주소 이동이 너무 많습니다");
    }

    private static URI check(String url) {
        URI uri;
        try { uri = URI.create(url.trim()); } catch (IllegalArgumentException e) { throw bad("주소가 올바르지 않습니다"); }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) throw bad("http/https 주소만 가져올 수 있습니다");
        try {
            for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
                if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress()
                        || a.isMulticastAddress() || a.getHostAddress().startsWith("fc") || a.getHostAddress().startsWith("fd")
                        || a.getHostAddress().startsWith("100.64."))
                    throw bad("내부 주소의 사진은 가져올 수 없습니다");
            }
        } catch (java.net.UnknownHostException e) {
            throw bad("주소를 찾을 수 없습니다");
        }
        return uri;
    }

    private static byte[] readLimited(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            if (out.size() + n > MAX_BYTES) throw bad("사진이 너무 큽니다 (15MB 넘음)");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
