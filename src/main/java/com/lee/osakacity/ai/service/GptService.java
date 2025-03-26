package com.lee.osakacity.ai.service;

import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lee.osakacity.ai.dto.SearchWebHook;
import com.lee.osakacity.ai.dto.custom.Status;
import com.lee.osakacity.ai.infra.QRoom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GptService {

    @Value("${ai.key}")
    private String ACCESS_KEY;

    private final RedisService redisService;
    private final JPAQueryFactory jpaQueryFactory;
    QRoom qRoom = QRoom.room;
    private static final int MAX_REQUEST = 10;
    private static final int SLEEP = 1000;
    @Async
    public void createSearchFilter(String userId, String userInput, String callbackUrl, SearchWebHook sw) {
        log.info(userInput);
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
        SearchWebHook newSw = new SearchWebHook();

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
                    
                            기존 조건 중에서 사용자의 입력을 반영해 수정해야 할 부분만 바꾸고, 전체 조건을 정확한 JSON 형식으로 다시 보내주세요.
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
            log.info("Gpt 콜 빠져나옴");
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

            newSw = objectMapper.readValue(assistantResponse, SearchWebHook.class);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        this.roomCount(userId, newSw, callbackUrl);
    }
    public void roomCount(String userId, SearchWebHook sw, String callbackUrl) {
        redisService.saveSearchSession(userId, sw);

        BooleanExpression predicate = this.predicated(sw);

        long count =  jpaQueryFactory
                .selectFrom(qRoom)
                .where(predicate)
                .fetchCount();

        StringBuilder conditionText = new StringBuilder();


        conditionText.append("\n총 ").append(count).append("개의 매물이 검색됩니다!");

//        // 5. simpleText로 출력
//        Map<String, Object> simpleText = Map.of(
//                "simpleText", Map.of(
//                        "text", conditionText.toString()
//                )
//        );

        // 6. 버튼 두 개 세로 배치 (basicCard)
        Map<String, Object> textCard = Map.of(
                "textCard", Map.of(
                        "title", count +"개의 집을 보여드릴 준비가 되었어요!",
                        "buttons", List.of(
                                Map.of(
                                        "action", "block",
                                        "label", "시작 하기",
                                        "blockId", "67e154775676f43ad024afe8"
                                ),
                                Map.of(
                                        "action", "block",
                                        "label", "조건을 다시 설정할래요!",
                                        "blockId", "67e255c75676f43ad024f402"
                                )
                        )
                )
        );

        // 7. 최종 응답 템플릿 구성
        Map<String, Object> template = Map.of(
                "outputs", List.of("textCard", textCard)
        );

        Map<String, Object> response = Map.of(
                "version", "2.0",
                "template", template
        );

        try {
            // 1. JSON 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. HttpEntity로 래핑
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(response, headers);
            RestTemplate restTemplate = new RestTemplate();
            // 4. POST 요청 보내기
            ResponseEntity<String> response2 = restTemplate.postForEntity(callbackUrl, entity, String.class);

            // 5. 응답 출력
            System.out.println("응답 코드: " + response2.getStatusCode());
            System.out.println("응답 바디: " + response2.getBody());

        } catch (Exception e) {
            System.err.println("POST 요청 중 오류: " + e.getMessage());

        }
    }
    private BooleanExpression predicated(SearchWebHook sw) {
        BooleanExpression predicate = qRoom.status.notIn(Status.T9, Status.T6);

        if (sw.getMinLat() != null && sw.getMaxLat() != null
                && sw.getMinLon() != null && sw.getMaxLon() != null) {

            predicate = predicate.and(qRoom.lat.between(sw.getMinLat(),sw.getMaxLat())
                    .and(qRoom.lon.between(sw.getMinLon(), sw.getMaxLon())));
        }
        if (sw.getFloorPlan() != null && !sw.getFloorPlan().isEmpty())
            predicate = predicate.and(qRoom.floorPlan.in(sw.getFloorPlan()));

        if (sw.getMinArea() != 0)
            predicate = predicate.and(qRoom.area.goe(sw.getMinArea()));

        if (sw.getMaxArea() != 0)
            predicate = predicate.and(qRoom.area.loe(sw.getMaxArea()));

        if (sw.getMinRentFee() != 0)
            predicate = predicate.and(qRoom.rentFee.goe(sw.getMaxRentFee()));

        if (sw.getMaxRentFee() != 0)
            predicate = predicate.and(qRoom.rentFee.loe(sw.getMaxRentFee()));

        if (sw.isFreeInternet())
            predicate = predicate.and(qRoom.freeInternet.isTrue());

        if (sw.isMorePeople())
            predicate = predicate.and(qRoom.morePeople.isTrue());

        if (sw.isPetsAllowed())
            predicate = predicate.and(qRoom.petsAllowed.isTrue());

        if (sw.getDeAllowedStructure() != null && !sw.getDeAllowedStructure().isEmpty())
            predicate = predicate.and(qRoom.structure.notIn(sw.getDeAllowedStructure()));

        return predicate;
    }
}
