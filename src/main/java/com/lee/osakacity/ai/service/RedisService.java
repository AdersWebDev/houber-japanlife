package com.lee.osakacity.ai.service;

import com.lee.osakacity.ai.dto.SearchWebHook;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "chatbot:session:";

    // 저장
    public void saveSearchSession(String userKey, SearchWebHook searchWebHook) {
        String redisKey = PREFIX + userKey;
        redisTemplate.opsForValue().set(redisKey, searchWebHook);
        redisTemplate.expire(redisKey, Duration.ofHours(3));
    }

    // 조회
    public SearchWebHook getSearchSession(String userKey) {
        String redisKey = PREFIX + userKey;
        Object data = redisTemplate.opsForValue().get(redisKey);

        if (data instanceof SearchWebHook) {
            return (SearchWebHook) data;
        } else {
            this.saveSearchSession(userKey, new SearchWebHook());
            return new SearchWebHook();
        }
    }

    // 삭제
    public void deleteSearchSession(String userKey) {
        String redisKey = PREFIX + userKey;
        redisTemplate.delete(redisKey);
    }
}
