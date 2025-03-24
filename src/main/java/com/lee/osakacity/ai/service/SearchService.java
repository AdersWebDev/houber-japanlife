package com.lee.osakacity.ai.service;

import com.lee.osakacity.ai.dto.SearchWebHook;
import com.lee.osakacity.ai.dto.SimpleRoom;
import com.lee.osakacity.ai.dto.custom.RoomType;
import com.lee.osakacity.ai.dto.custom.Status;
import com.lee.osakacity.ai.dto.custom.Structure;
import com.lee.osakacity.ai.infra.QBuilding;
import com.lee.osakacity.ai.infra.QRoom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final JPAQueryFactory jpaQueryFactory;
    private final RedisService redisService;
    private final GptService gptService;

    QBuilding qBuilding = QBuilding.building;
    QRoom qRoom = QRoom.room;

    public ResponseEntity<Map<String, Object>> userInit(Map<String, Object> payload) {
        Map<String, Object> userRequest = (Map<String, Object>) payload.get("userRequest");
        Map<String, Object> user = (Map<String, Object>) userRequest.get("user");
        String userId = (String) user.get("id"); // 사용자 고유 ID
        String utterance = (String) userRequest.get("utterance");

        // 초기 SearchWebHook 생성 (기본값으로 초기화)
        SearchWebHook searchWebHook = SearchWebHook.builder()
                .location(null)
                .transport(null)
                .duration(null)
                .radius(null)
                .minLat(null)
                .maxLat(null)
                .minLon(null)
                .maxLon(null)
                .floorPlan(new ArrayList<>())
                .minArea(0f)
                .minRentFee(0)
                .maxRentFee(0)
                .freeInternet(false)
                .morePeople(false)
                .petsAllowed(false)
                .deAllowedStructure(new ArrayList<>())
                .build();

        // Redis에 저장
        redisService.saveSearchSession(userId, searchWebHook);

        Map<String, Object> simpleText = Map.of(
                "simpleText", Map.of(
                        "text", "찾으실 건물의 범위를 알려주세요! ex) 오사카 난바 전철 30분거리"
                )
        );

        Map<String, Object> template = Map.of(
                "outputs", List.of(simpleText)
        );

        Map<String, Object> response = Map.of(
                "version", "2.0",
                "template", template
        );
        return ResponseEntity.ok(response);
    }
//    Map<String, Object> userRequest = (Map<String, Object>) payload.get("userRequest");
    //        Map<String, Object> user = (Map<String, Object>) userRequest.get("user");
//
//        String userId = (String) user.get("id"); // 사용자 고유 ID
//        String utterance = (String) userRequest.get("utterance"); // 사용자가 입력한 텍스트
//
//        // 2. action에서 블록 이름 추출
//        Map<String, Object> action = (Map<String, Object>) payload.get("action");
//        String resBlockName = (String) action.get("name"); // 연결된 블록 이름
//
//        // 4. KakaoLog 엔티티로 빌더 패턴 사용해서 생성
//        KakaoLog kakaoLog = KakaoLog.builder()
//                .userId(userId)
//                .createDate(LocalDateTime.now())
//                .userReq(utterance)
//                .resBlockName(resBlockName)
//                .build();
//
//        // 5. 저장 (예시로 JpaRepository 이용)
//        kakaoRepo.save(kakaoLog);
//
//        // 6. 카카오에게 응답 (응답 JSON은 자유롭게 수정)
//        Map<String, Object> responseBody = Map.of(
//                "version", "2.0",
//                "template", Map.of(
//                        "outputs", List.of(
//                                Map.of("simpleText", Map.of("text", "요청이 정상 처리되었습니다!"))
//                        )
//                )
//        );
    public ResponseEntity<Map<String, Object>>  roomCounter(@RequestBody Map<String, Object> payload) {
        Map<String, Object> userRequest = (Map<String, Object>) payload.get("userRequest");
        Map<String, Object> user = (Map<String, Object>) userRequest.get("user");
        String userId = (String) user.get("id"); // 사용자 고유 ID
        String utterance = (String) userRequest.get("utterance");

        SearchWebHook sw = redisService.getSearchSession(userId);

        sw = gptService.createSearchFilter(utterance, sw);
        redisService.saveSearchSession(userId, sw);

        BooleanExpression predicate = this.predicated(sw);

        long count =  jpaQueryFactory
                .selectFrom(qRoom)
                .where(predicate)
                .fetchCount();

        StringBuilder conditionText = new StringBuilder();

        conditionText.append("현재 선택한 검색 조건입니다.\n\n");

        conditionText.append("지역: ")
                .append(sw.getLocation() != null ? sw.getLocation() : "지정되지 않음").append("\n");

        conditionText.append("거리: ")
                .append(sw.getDuration() != null ? sw.getDuration() : "지정되지 않음").append("\n");

        conditionText.append("방 타입(K/DK/LDK) : ");
        if (sw.getFloorPlan() == null || sw.getFloorPlan().isEmpty()) {
            conditionText.append("선택 안함\n");
        } else {
            String excludedRoomType = sw.getFloorPlan().stream()
                    .map(RoomType::getTitle)  // description 필드 꺼내기
                    .collect(Collectors.joining(", "));

            conditionText.append(excludedRoomType).append("\n");
        }

        // 면적 조건 멘트
        if (sw.getMinArea() > 0 && sw.getMaxArea() > 0) {
            conditionText.append("면적: ")
                    .append(sw.getMinArea()).append("㎡ ~ ").append(sw.getMaxArea()).append("㎡");
        } else if (sw.getMinArea() > 0) {
            conditionText.append("면적: ").append(sw.getMinArea()).append("㎡ 이상");
        } else if (sw.getMaxArea() > 0) {
            conditionText.append("면적: ").append(sw.getMaxArea()).append("㎡ 이하");
        } else {
            conditionText.append("면적: 지정되지 않음");
        }
        conditionText.append("\n");

// 월세 조건 멘트
        if (sw.getMinRentFee() > 0 && sw.getMaxRentFee() > 0) {
            conditionText.append("월세: ")
                    .append(String.format("%,d", sw.getMinRentFee())).append("엔 ~ ")
                    .append(String.format("%,d", sw.getMaxRentFee())).append("엔");
        } else if (sw.getMinRentFee() > 0) {
            conditionText.append("월세: 최소 ")
                    .append(String.format("%,d", sw.getMinRentFee())).append("엔");
        } else if (sw.getMaxRentFee() > 0) {
            conditionText.append("월세: 최대 ")
                    .append(String.format("%,d", sw.getMaxRentFee())).append("엔");
        } else {
            conditionText.append("월세: 지정되지 않음");
        }
        conditionText.append("\n");

        conditionText.append("반려동물 동반: ")
                .append(sw.isPetsAllowed() ? "필요" : "상관없음").append("\n");

        conditionText.append("2인입주: ")
                .append(sw.isMorePeople() ? "필요" : "상관없음").append("\n");

        conditionText.append("제외할 건축 구조(철근/철골/목조): ");
        if (sw.getDeAllowedStructure() == null || sw.getDeAllowedStructure().isEmpty()) {
            conditionText.append("상관 없음\n");
        } else {
            String excludedStructures = sw.getDeAllowedStructure().stream()
                    .map(Structure::getTitle)  // description 필드 꺼내기
                    .collect(Collectors.joining(", "));

            conditionText.append(excludedStructures).append(" 제외\n");
        }

        conditionText.append("\n총 ").append(count).append("개의 매물이 검색됩니다!");

        // 5. simpleText로 출력
        Map<String, Object> simpleText = Map.of(
                "simpleText", Map.of(
                        "text", conditionText.toString()
                )
        );

        // 6. 버튼 두 개 세로 배치 (basicCard)
        Map<String, Object> basicCard = Map.of(
                "basicCard", Map.of(
                        "title", "다음 작업을 선택해 주세요!",
                        "buttons", List.of(
                                Map.of(
                                        "action", "message",
                                        "label", "이 조건으로 검색하기",
                                        "messageText", "검색 시작"
                                ),
                                Map.of(
                                        "action", "message",
                                        "label", "조건 초기화하기",
                                        "messageText", "검색 조건 초기화"
                                )
                        )
                )
        );

        // 7. 최종 응답 템플릿 구성
        Map<String, Object> template = Map.of(
                "outputs", List.of(simpleText, basicCard)
        );

        Map<String, Object> response = Map.of(
                "version", "2.0",
                "template", template
        );

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<Map<String, Object>> searchStart(Map<String, Object> payload) {
        // 1. 사용자 정보 가져오기
        Map<String, Object> userRequest = (Map<String, Object>) payload.get("userRequest");
        Map<String, Object> user = (Map<String, Object>) userRequest.get("user");
        String userId = (String) user.get("id"); // 사용자 고유 ID

        // 2. 파라미터에서 page 값 추출
        Map<String, Object> action = (Map<String, Object>) payload.get("action");
        Map<String, Object> params = (Map<String, Object>) action.get("detailParams");

        int page = 0; // 기본 페이지는 0

        if (params != null && params.containsKey("page")) {
            page = Integer.parseInt((String) params.get("page"));
        }

        // 3. 검색 조건 가져오기
        SearchWebHook sw = redisService.getSearchSession(userId);

        BooleanExpression predicate = this.predicated(
                new SearchWebHook().builder()
                        .location("오사카 난바")
                        .transport("도보")
                        .duration("30분")
                        .radius("2.4km")
                        .minLat(34.6504)
                        .maxLat(34.6954)
                        .minLon(135.4891)
                        .maxLon(135.5361)
                        .minArea(13.2F)
                        .maxArea(22.2F)
                        .freeInternet(false)
                        .morePeople(false)
                        .petsAllowed(false)
                        .floorPlan(null)
                        .deAllowedStructure(null)
                        .build()
        );

        // 4. 페이지네이션 검색 실행
        Page<SimpleRoom> result = queryExecute(predicate, page);

        // 5. 카드 리스트 생성
        List<Map<String, Object>> items = result.getContent().stream()
                .map(this::createKakaoCard)
                .collect(Collectors.toList());

        // 6. 이전/다음 페이지 quickReplies 생성
        List<Map<String, Object>> quickReplies = new ArrayList<>();

        // 이전 페이지가 있을 경우 추가
        if (result.getNumber() > 0) {
            quickReplies.add(Map.of(
                    "label", "이전 페이지",
                    "action", "block",
                    "blockId", "BLOCK_ID_이전 페이지",
                    "extra", Map.of(                // 중첩 Map은 이렇게
                            "page", String.valueOf(result.getNumber() - 1)  // 다음 페이지 넘기기
                    )
                    ));
        }

        // 다음 페이지가 있을 경우 추가
        if (result.hasNext()) {
            quickReplies.add(Map.of(
                    "label", "다음 페이지",
                    "action", "block",
                    "blockId", "BLOCK_ID_다음페이지",  // 이건 실제 카카오 빌더에서 만든 블록 ID
                    "extra", Map.of(                // 중첩 Map은 이렇게
                            "page", String.valueOf(result.getNumber() + 1)  // 다음 페이지 넘기기
                    )
            ));
        }

        // 7. 최종 응답 생성 및 반환
        return ResponseEntity.ok(
                Map.of(
                        "version", "2.0",
                        "template", Map.of(
                                "outputs", List.of(
                                        Map.of("carousel", Map.of(
                                                "type", "basicCard",
                                                "items", items
                                        ))
                                ),
                                "quickReplies", quickReplies
                        )
                )
        );
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

    private Page<SimpleRoom> queryExecute(BooleanExpression predicate, int page) {
        Pageable pageable = PageRequest.of(page, 10);

        JPAQuery<SimpleRoom> query = jpaQueryFactory
                .select(Projections.constructor(SimpleRoom.class,
                        qRoom.thumbnail,
                        qRoom.roomNumber,
                        qRoom.status,
                        qRoom.dateOfMoveIn,
                        qRoom.dateOfPreliminaryInspection,
                        qRoom.floorPlan,
                        qRoom.area,
                        qRoom.rentFee,
                        qRoom.managementFee
                ))
                .from(qRoom)
                .where(predicate);

        // 전체 카운트
        long total = query.fetchCount();

        // 페이징 처리
        List<SimpleRoom> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    public Map<String, Object> createKakaoCard(SimpleRoom room) {
        // 기본 description 내용 구성
        StringBuilder description = new StringBuilder();

        description.append("상태: ").append(room.getStatus().getDescription()).append("\n");
        // 상태가 특정 조건일 경우 입주일/사전 점검일 추가
//        if (room.getStatus().equals(Status.T2)) {
//            description.append("상태 비고: ").append(room.getDateOfMoveIn()).append("\n");
//        } else if (room.getStatus().equals(Status.T8)) {
//            description.append("상태 비고: ").append(room.getDateOfPreliminaryInspection()).append("\n");
//        }
//        description.append("월세: ").append(room.getRentFee()).append("엔").append("\n");
//        description.append("관리비: ").append(room.getManagementFee()).append("엔").append("\n");
//        description.append("면적: ").append(room.getArea()).append("㎡").append("\n");
//        description.append("구조: ").append(room.getFloorPlan().getTitle()).append("\n");

        return Map.of(
                "title", room.getRoomNum() + "호실",
                "description", description.toString().trim(),
                "thumbnail", Map.of(
                        "imageUrl", room.getThumbnailImg()
                ),
                "buttons", List.of(
                        Map.of(
                                "action", "webLink",
                                "label", "자세히 보기",
                                "webLinkUrl", "https://your-site/detail/" + room.getRoomNum()
                        )
                )
        );
    }


}
