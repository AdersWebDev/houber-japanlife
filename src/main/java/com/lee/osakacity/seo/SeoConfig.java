package com.lee.osakacity.seo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class SeoConfig {
    /** 응답 헤더 Content-Language를 서버 기본값(en-US) 대신 ko-KR로 (페이지는 모두 한국어) */
    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.KOREA);
    }
}
