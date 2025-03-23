package com.lee.osakacity.ai.controller;

import com.lee.osakacity.ai.dto.PointFilter;
import com.lee.osakacity.ai.infra.KakaoLog;
import com.lee.osakacity.ai.infra.KakaoRepo;
import com.lee.osakacity.ai.service.GptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
public class KakaoWebhook {
    private final KakaoRepo kakaoRepo;
    private final GptService gptService;

    @PostMapping("/webhook")
    public ResponseEntity<String> kakaoWebhook(@RequestBody Map<String, Object> payload) {

        // 1. userRequest에서 유저 ID와 발화 추출
        Map<String, Object> userRequest = (Map<String, Object>) payload.get("userRequest");
        Map<String, Object> user = (Map<String, Object>) userRequest.get("user");

        String userId = (String) user.get("id"); // 사용자 고유 ID
        String utterance = (String) userRequest.get("utterance"); // 사용자가 입력한 텍스트

        // 2. action에서 블록 이름 추출
        Map<String, Object> action = (Map<String, Object>) payload.get("action");
        String resBlockName = (String) action.get("name"); // 연결된 블록 이름

        // 4. KakaoLog 엔티티로 빌더 패턴 사용해서 생성
        KakaoLog kakaoLog = KakaoLog.builder()
                .userId(userId)
                .createDate(LocalDateTime.now())
                .userReq(utterance)
                .resBlockName(resBlockName)
                .build();

        // 5. 저장 (예시로 JpaRepository 이용)
        kakaoRepo.save(kakaoLog);

        // 6. 카카오에게 응답 (응답 JSON은 자유롭게 수정)
        Map<String, Object> responseBody = Map.of(
                "version", "2.0",
                "template", Map.of(
                        "outputs", List.of(
                                Map.of("simpleText", Map.of("text", "요청이 정상 처리되었습니다!"))
                        )
                )
        );

        return ResponseEntity.ok().body("OK");
    }

    @GetMapping("/district")
    public PointFilter a (@RequestParam String message) {
        return gptService.createSearchFilter(message);
    }
}
