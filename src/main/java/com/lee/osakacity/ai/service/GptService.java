package com.lee.osakacity.ai.service;

import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lee.osakacity.ai.dto.SearchWebHook;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GptService {

    @Value("${ai.key}")
    private String ACCESS_KEY;

    private static final int MAX_REQUEST = 10;
    private static final int SLEEP = 1000;

    public SearchWebHook createSearchFilter(String userInput, SearchWebHook sw) {
        String THREAD_URL = "https://api.openai.com/v1/threads";
        String MESSAGE_URL_TEMPLATE = "https://api.openai.com/v1/threads/%s/messages";
        String RUN_URL_TEMPLATE = "https://api.openai.com/v1/threads/%s/runs";
        String GET_MESSAGES_URL_TEMPLATE = "https://api.openai.com/v1/threads/%s/messages";
        String ASSISTANT_ID = "asst_GzchHhL97UCKggTY09duPJNz"; // 너가 만든 Assistant ID

        // 공통 헤더 세팅
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ACCESS_KEY);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("OpenAI-Beta", "assistants=v2");

        RestTemplate restTemplate1 = new RestTemplate();

        try {
            // 1. Thread 생성
            HttpEntity<String> threadRequest = new HttpEntity<>("{}", headers);
            ResponseEntity<String> threadResponse = restTemplate1.postForEntity(THREAD_URL, threadRequest, String.class);

            String threadId = new JSONObject(threadResponse.getBody()).getString("id");
            // 2. 사용자 메시지 추가
            String messageUrl = String.format(MESSAGE_URL_TEMPLATE, threadId);
            //2-1 프롬프트 생성
            String prevConditionJson = Jackson.toJsonString(sw);
            String prompt = String.format("""
                기존 조건은 아래와 같습니다.
                %s
        
                사용자가 새로운 조건을 입력했습니다:
                "%s"
        
                기존 조건 중에서 사용자의 입력을 반영해 수정해야 할 부분만 바꾸고, 전체 조건을 JSON 형식으로 다시 보내주세요.
        """, prevConditionJson, userInput);


            Map<String, Object> userMessagePayload = Map.of(
                    "role", "user",
                    "content", prompt
            );
            HttpEntity<Map<String, Object>> messageRequest = new HttpEntity<>(userMessagePayload, headers);
            restTemplate1.postForEntity(messageUrl, messageRequest, String.class);

            // 3. Run 실행
            String runUrl = String.format(RUN_URL_TEMPLATE, threadId);
            Map<String, Object> runPayload = Map.of(
                    "assistant_id", ASSISTANT_ID
            );
            HttpEntity<Map<String, Object>> runRequest = new HttpEntity<>(runPayload, headers);
            ResponseEntity<String> runResponse = restTemplate1.postForEntity(runUrl, runRequest, String.class);

            String runId = new JSONObject(runResponse.getBody()).getString("id");

            // 4. Run 상태 확인
            String runStatusUrl = String.format("https://api.openai.com/v1/threads/%s/runs/%s", threadId, runId);

            String status = "";
            int retries = 0;

            while (retries < MAX_REQUEST) {
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
                ResponseEntity<String> runStatusResponse = restTemplate1.exchange(
                        runStatusUrl,
                        HttpMethod.GET,
                        httpEntity,
                        String.class
                );
                JSONObject runStatusJson = new JSONObject(runStatusResponse.getBody());
                status = runStatusJson.getString("status");

                if ("completed".equals(status)) {
                    break;
                }

                Thread.sleep(SLEEP); // 0.5초 대기 후 재시도
                retries++;
            }

            if (!"completed".equals(status)) {
                throw new RuntimeException("GPT Assistant 응답 시간 초과");
            }

            // 5. 결과 메시지 조회
            String getMessageUrl = String.format(GET_MESSAGES_URL_TEMPLATE, threadId);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<String> messagesResponse = restTemplate1.exchange(
                    getMessageUrl,
                    HttpMethod.GET,
                    httpEntity,
                    String.class
            );

            // 응답 파싱
            JSONObject messageJson = new JSONObject(messagesResponse.getBody());
            JSONArray dataArray = messageJson.getJSONArray("data");
            JSONObject latestMessage = dataArray.getJSONObject(0);
            JSONArray contentArray = latestMessage.getJSONArray("content");
            JSONObject textObj = contentArray.getJSONObject(0).getJSONObject("text");

            String assistantResponse = textObj.getString("value");

            // 6. JSON 응답 → EstateSearchFilter로 변환
            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readValue(assistantResponse, SearchWebHook.class);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
