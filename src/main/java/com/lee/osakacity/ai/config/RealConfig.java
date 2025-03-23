package com.lee.osakacity.ai.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class RealConfig {
    @Bean
    public RestTemplate restTemplate() {
        final String Base_URL = "https://www.realnetpro.com";
        // 쿠키 저장소 설정
        BasicCookieStore cookieStore = new BasicCookieStore();

        // 요청 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(true)
                .build();

        // HttpClient 생성
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(requestConfig)
                .setRedirectStrategy(new RedirectStrategy() {
                    @Override
                    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
                        // 리다이렉트 전략을 설정할 수 있습니다.
                        return response.getCode() == HttpStatus.SC_MOVED_PERMANENTLY ||
                                response.getCode() == HttpStatus.SC_MOVED_TEMPORARILY;
                    }
                    @Override
                    public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException {
                        String location = response.getFirstHeader("Location").getValue();
                        try {
                            URI redirectUri = new URI(location);

                            // 상대 경로일 경우 baseUrl과 결합하여 절대 경로로 변환
                            if (!redirectUri.isAbsolute()) {
                                redirectUri = new URIBuilder(Base_URL + location).build();
                            }

                            return redirectUri;
                        } catch (URISyntaxException e) {
                            throw new HttpException("Invalid redirect URI", e);
                        }
                    }
                })
                .build();

        // RestTemplate에 HttpClient 5.x 설정 적용
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
