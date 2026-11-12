package com.lee.osakacity.service;

import com.amazonaws.services.kms.model.NotFoundException;
import com.lee.osakacity.custom.SnsCategory;
import com.lee.osakacity.dto.YouTubeSearchItem;
import com.lee.osakacity.dto.SnsContentResponseDto;
import com.lee.osakacity.dto.YoutubeResponse;
import com.lee.osakacity.dto.mvc.SimpleResponse;
import com.lee.osakacity.infra.entity.QSnsContent;
import com.lee.osakacity.infra.entity.SnsContent;
import com.lee.osakacity.infra.repository.SnsContentRepo;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SnsContentService {
    private final SnsContentRepo snsContentRepo;
    private final RestTemplate restTemplate;
    private final JPAQueryFactory jpaQueryFactory;
    @Value("${google.console.key}")
    private String KEY;

    QSnsContent qSnsContent = QSnsContent.snsContent;
    @Transactional
    public SnsContentResponseDto getDetail(Long id) {
        SnsContent snsContent =  snsContentRepo.findById(id)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); // 없는 영상은 404 페이지
        snsContent.increaseView();
        return new SnsContentResponseDto(snsContent);
    }
    @Transactional
    public void youtubeUpdate(String channelId) {
        String url = "https://www.googleapis.com/youtube/v3/search?key="
                + KEY
                + "&channelId=" + channelId
                + "&part=snippet,id&order=date&maxResults=50";

        ResponseEntity<YoutubeResponse> response = restTemplate.getForEntity(url, YoutubeResponse.class);
        List<YouTubeSearchItem> items = Optional.of(response)
                .map(ResponseEntity::getBody)  // getBody()가 null일 경우 Optional.empty() 반환
                .map(YoutubeResponse::getItems)  // getItems()가 null일 경우 Optional.empty() 반환
                .orElse(Collections.emptyList());  // 모든 단계가 null이면 null 반환

        if (items.isEmpty()) return;

        List<SnsContent> newSnsContent = new ArrayList<>();
        for (YouTubeSearchItem y : items) {
            if (y.getId().getVideoId() == null || snsContentRepo.existsByContent(y.getId().getVideoId()) )
                continue;

            Boolean value = isShortVideo(y.getId().getVideoId());
            SnsCategory snsCategory;
            if ( value == null)
                continue;
            else if (value)
                snsCategory = SnsCategory.YOUTUBE_SHORT;
             else
                snsCategory = SnsCategory.YOUTUBE;


            newSnsContent.add(
                    SnsContent.builder()
                            .publishTime(y.getSnippet().getPublishedAt())
                            .snsCategory(snsCategory)
                            .thumbnailUrl(y.getSnippet().getThumbnails().getHigh().getUrl())
                            .title(y.getSnippet().getTitle())
                            .description(y.getSnippet().getDescription())
                            .keyword("test")
                            .content(y.getId().getVideoId())
                            .build()
            );
        }
        snsContentRepo.saveAll(newSnsContent);

    }
    /**
     * 🕒 Shorts 여부 판별
     * @param videoId - 확인할 YouTube Video ID
     * @return true: Shorts / false: 일반 영상
     */
    private Boolean isShortVideo(String videoId) {
        String url = "https://www.googleapis.com/youtube/v3/videos?key="
                + KEY
                + "&id=" + videoId
                + "&part=contentDetails"
                + "&fields=items(contentDetails(duration))";

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        // JSON 데이터 추출
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("items")) {
            return null;
        }

        // "items" 배열에서 첫 번째 요소의 "contentDetails.duration" 추출
        Object items = responseBody.get("items");
        if (items instanceof java.util.List<?> itemList && !itemList.isEmpty()) {
            Map<String, Object> firstItem = (Map<String, Object>) itemList.get(0);
            Map<String, Object> contentDetails = (Map<String, Object>) firstItem.get("contentDetails");

            Duration videoDuration = Duration.parse((String) contentDetails.get("duration")); // ISO 8601 형식 파싱
            long seconds = videoDuration.getSeconds(); // 초 단위로 변환
            return seconds <= 60; // 60초 이하인지 확인
        }

        return null;
    }

    public List<SimpleResponse> moreContents(Long id) {
        List<SimpleResponse> dtoList = new ArrayList<>(
                jpaQueryFactory
                        .select(Projections.constructor(SimpleResponse.class,
                                qSnsContent.id,
                                qSnsContent.view,
                                qSnsContent.title,
                                qSnsContent.thumbnailUrl,
                                Expressions.constant("/detail/sns-content/")
                        ))
                        .from(qSnsContent)
                        .where(qSnsContent.id.ne(id))
                        .limit(15)
                        .orderBy(qSnsContent.id.asc())
                        .fetch()
        );

        // 2. 리스트 분리 및 재정렬
        List<SimpleResponse> greaterList = dtoList.stream()
                .filter(dto -> dto.getId() > id)
                .toList();

        List<SimpleResponse> lesserList = dtoList.stream()
                .filter(dto -> dto.getId() < id)
                .toList();

        // 3. 최종 리스트 합치기 (큰 값이 먼저, 그 뒤 작은 값)
        return Stream.concat(greaterList.stream(), lesserList.stream())
                .collect(Collectors.toList());
    }
}
