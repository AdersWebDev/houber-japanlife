package com.lee.osakacity.service;

import com.amazonaws.services.kms.model.NotFoundException;
import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.PostResponseDto;
import com.lee.osakacity.dto.mvc.SearchResponseDto;
import com.lee.osakacity.dto.mvc.SimpleResponse;
import com.lee.osakacity.dto.restful.ImgResponse;
import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.infra.entity.*;
import com.lee.osakacity.infra.repository.PostRepo;
import com.lee.osakacity.infra.repository.SnsContentRepo;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepo postRepo;
    private final JPAQueryFactory jpaQueryFactory;
    private final SnsContentRepo snsContentRepo;
    @Value("${jasypt.encryptor.password}")
    private String KEY;
    @Value("${aders.accessKey}")
    private String ACCESS_KEY;
    private final S3Service s3Service;

    private final RestTemplate restTemplate = new RestTemplate();
    QPost qPost = QPost.post;
    QFile qFile = QFile.file;
    QSnsContent qSnsContent = QSnsContent.snsContent;
    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        String key =  request.getHeader("authorization");
        if ( !key.equals(KEY) )
            throw new IllegalArgumentException();

        Post post = postRepo.findById(id).orElseThrow(()->new NotFoundException("e"));
        s3Service.deleteFileInjection(post.getFileList());
        postRepo.delete(post);
    }
    public List<SimpleResponse> getList (Category category, int limit ,Long cursorId, Integer cursorView, LocalDateTime cursorTime) {
        if (category.equals(Category.japan_review)) {
            List<SimpleResponse> pList = new ArrayList<>(jpaQueryFactory
                    .select(Projections.constructor(SimpleResponse.class,
                            qPost.id,
                            qPost.view,
                            qPost.title,
                            qPost.thumbnailUrl,
                            qPost.modifiedDate,
                            Expressions.constant("/detail/")))
                    .from(qPost)
                    .where(
                            qPost.isShow.isTrue().and(
                                    qPost.category.eq(Category.japan_review).and(
                                            cursorTime != null ? qPost.modifiedDate.lt(cursorTime) : null
                                    )
                            )
                    )
                    .orderBy(qPost.modifiedDate.desc())
                    .limit(limit)
                    .fetch());

            boolean isSnsLoading = false;
            if ( !pList.isEmpty() ) {
                Long id =  pList.get(pList.size() - 1).getId();
                Long lastPostId = jpaQueryFactory
                        .select(qPost.id)
                        .from(qPost)
                        .where(qPost.isShow.isTrue())
                        .orderBy(qPost.id.desc())
                        .limit(1)
                        .fetchOne();

                if ( !(id.equals(lastPostId)) ) {
                    isSnsLoading = true;
                }
            } else {
                isSnsLoading = true;
            }


            List<SimpleResponse> sList = new ArrayList<>();
            if ( isSnsLoading ) {
                sList.addAll(
                        jpaQueryFactory
                                .select(Projections.constructor(SimpleResponse.class,
                                        qSnsContent.id,
                                        qSnsContent.view,
                                        qSnsContent.title,
                                        qSnsContent.thumbnailUrl,
                                        qSnsContent.publishTime,
                                        Expressions.constant("/detail/sns-content/")))
                                .from(qSnsContent)
                                .where(
                                        cursorTime != null ? qSnsContent.publishTime.lt(cursorTime) : null
                                )
                                .orderBy(qSnsContent.publishTime.desc())
                                .limit(limit)
                                .fetch());
            }
            // 📌 SnsContent 데이터 가져오기 (limit + extraFetch)


            // 📌 두 리스트 병합
            List<SimpleResponse> combinedList = new ArrayList<>();
            combinedList.addAll(pList);
            combinedList.addAll(sList);

            // 📌 뷰 기준으로 정렬
            combinedList.sort(Comparator.comparing(SimpleResponse::getCursorTime, Comparator.reverseOrder()));

            // 📌 최종적으로 limit 만큼만 반환
            return combinedList.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

        } else if (category.equals(Category.event)) {

        }
        return jpaQueryFactory
                .select(Projections.constructor(SimpleResponse.class,
                        qPost.id,
                        qPost.view,
                        qPost.title,
                        qPost.thumbnailUrl,
                        Expressions.constant("/detail/")
                ))
                .from(qPost)
                .where(
                        qPost.isShow.isTrue().and(
                                cursorId != null
                                        ? qPost.category.eq(category).and(qPost.id.lt(cursorId))
                                        : qPost.category.eq(category)
                        )
                )
                .orderBy(qPost.id.desc(), qPost.view.desc())
                .limit(limit)
                .fetch();

//        if (category.equals(Category.japan_life)) {
//            // Post 데이터 가져오기
//            return jpaQueryFactory
//                    .select(Projections.constructor(SimpleResponse.class,
//                            qPost.id,
//                            qPost.view,
//                            qPost.title,
//                            qPost.thumbnailUrl,
//                            Expressions.constant("/detail/")
//                    ))
//                    .from(qPost)
//                    .where(
//                            qPost.isShow.isTrue().and(
//                                    qPost.category.in(Category.japan_review)
//                            )
////                            qPost.isShow.isTrue().and(
////                                            (cursorView != null && cursorId != null)
////                                                    ? qPost.view.lt(cursorView)
////                                                    .or(qPost.view.eq(cursorView).and(qPost.id.lt(cursorId)))
////                                                    : null
////                            )
//                    )
//                    .orderBy(qPost.id.desc(), qPost.view.desc())
//                    .limit(limit)
//                    .fetch();
//
//        } else if (category.equals(Category.all)) {
//
//            List<SimpleResponse> pList = new ArrayList<>(jpaQueryFactory
//                    .select(Projections.constructor(SimpleResponse.class,
//                            qPost.id,
//                            qPost.view,
//                            qPost.title,
//                            qPost.thumbnailUrl,
//                            qPost.modifiedDate,
//                            Expressions.constant("/detail/")))
//                    .from(qPost)
//                    .where(
//                            qPost.isShow.isTrue().and(
//                                cursorTime != null ? qPost.modifiedDate.lt(cursorTime) : null
//                            )
//                    )
//                    .orderBy(qPost.modifiedDate.desc())
//                    .limit(limit)
//                    .fetch());
//
//            boolean isSnsLoading = false;
//            if ( !pList.isEmpty() ) {
//                Long id =  pList.get(pList.size() - 1).getId();
//                Long lastPostId = jpaQueryFactory
//                        .select(qPost.id)
//                        .from(qPost)
//                        .where(qPost.isShow.isTrue())
//                        .orderBy(qPost.id.desc())
//                        .limit(1)
//                        .fetchOne();
//
//                if ( !(id.equals(lastPostId)) ) {
//                    isSnsLoading = true;
//                }
//            } else {
//                isSnsLoading = true;
//            }
//
//
//            List<SimpleResponse> sList = new ArrayList<>();
//            if ( isSnsLoading ) {
//                sList.addAll(
//                        jpaQueryFactory
//                        .select(Projections.constructor(SimpleResponse.class,
//                                qSnsContent.id,
//                                qSnsContent.view,
//                                qSnsContent.title,
//                                qSnsContent.thumbnailUrl,
//                                qSnsContent.publishTime,
//                                Expressions.constant("/detail/sns-content/")))
//                        .from(qSnsContent)
//                        .where(
//                                cursorTime != null ? qSnsContent.publishTime.lt(cursorTime) : null
//                        )
//                        .orderBy(qSnsContent.publishTime.desc())
//                        .limit(limit)
//                        .fetch());
//            }
//            // 📌 SnsContent 데이터 가져오기 (limit + extraFetch)
//
//
//            // 📌 두 리스트 병합
//            List<SimpleResponse> combinedList = new ArrayList<>();
//            combinedList.addAll(pList);
//            combinedList.addAll(sList);
//
//            // 📌 뷰 기준으로 정렬
//            combinedList.sort(Comparator.comparing(SimpleResponse::getCursorTime, Comparator.reverseOrder()));
//
//            // 📌 최종적으로 limit 만큼만 반환
//            return combinedList.stream()
//                    .limit(limit)
//                    .collect(Collectors.toList());
//
//        } else if (category.equals(Category.houber_sns)) {
//            return jpaQueryFactory
//                    .select(Projections.constructor(SimpleResponse.class,
//                            qSnsContent.id,
//                            qSnsContent.view,
//                            qSnsContent.title,
//                            qSnsContent.thumbnailUrl,
//                            qSnsContent.publishTime,
//                            Expressions.constant("/detail/sns-content/")))
//                    .from(qSnsContent)
//                    .where(
//                            cursorTime != null ? qSnsContent.publishTime.lt(cursorTime) : null
//                    )
//                    .orderBy(qSnsContent.publishTime.desc())
//                    .limit(limit)
//                    .fetch();
//
//        } else {
//            return jpaQueryFactory
//                    .select(Projections.constructor(SimpleResponse.class,
//                            qPost.id,
//                            qPost.view,
//                            qPost.title,
//                            qPost.thumbnailUrl,
//                            Expressions.constant("/detail/")
//                    ))
//                    .from(qPost)
//                    .where(
//                            qPost.isShow.isTrue().and(
//                                cursorId != null
//                                        ? qPost.category.eq(category).and(qPost.id.lt(cursorId))
//                                        : qPost.category.eq(category)
//                            )
//                    )
//                    .orderBy(qPost.id.desc())
//                    .limit(limit)
//                    .fetch();
//
//        }
    }
    /**
     * 메인 페이지 '하우버 입주자 스토리 & 맨션정보' 영역
     * 입주후기 글을 최신순으로 먼저 보여주고, 제목이 같은 글은 가장 최근 것 하나만 보여준다.
     * 입주후기가 limit보다 적으면 남는 칸을 맨션 영상으로 채운다.
     * (더보기 목록과 무한 스크롤은 기존 getList를 그대로 사용)
     */
    public List<SimpleResponse> getMainReviewList(int limit) {
        List<SimpleResponse> posts = jpaQueryFactory
                .select(Projections.constructor(SimpleResponse.class,
                        qPost.id,
                        qPost.view,
                        qPost.title,
                        qPost.thumbnailUrl,
                        qPost.modifiedDate,
                        Expressions.constant("/detail/")))
                .from(qPost)
                .where(qPost.isShow.isTrue().and(qPost.category.eq(Category.japan_review)))
                .orderBy(qPost.modifiedDate.desc(), qPost.id.desc())
                .fetch();

        // 📌 같은 글이 여러 번 올라간 경우 가장 최근 것 하나만
        List<SimpleResponse> result = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        for (SimpleResponse post : posts) {
            if (result.size() >= limit) break;
            if (titles.add(post.getTitle())) result.add(post);
        }

        // 📌 남는 칸은 맨션 영상으로 채우기
        if (result.size() < limit) {
            result.addAll(jpaQueryFactory
                    .select(Projections.constructor(SimpleResponse.class,
                            qSnsContent.id,
                            qSnsContent.view,
                            qSnsContent.title,
                            qSnsContent.thumbnailUrl,
                            qSnsContent.publishTime,
                            Expressions.constant("/detail/sns-content/")))
                    .from(qSnsContent)
                    .orderBy(qSnsContent.publishTime.desc())
                    .limit(limit - result.size())
                    .fetch());
        }
        return result;
    }
    public List<SimpleResponse> getGuideOnly(int limit) {
        return jpaQueryFactory
                .select(Projections.constructor(SimpleResponse.class,
                        qPost.id,
                        qPost.view,
                        qPost.title,
                        qPost.thumbnailUrl,
                        Expressions.constant("/detail/")
                ))
                .from(qPost)
                .where(qPost.isShow.isTrue())
                .orderBy(Expressions.numberTemplate(Double.class, "function('RAND')").asc())
                .limit(limit)
                .fetch();
    }
    /**
     * 특수기호 제외시키기
     * @param keyword 검색 키워드
     * @return 검색 결과 도출
     */
    public List<SearchResponseDto> search(String keyword, int limit, LocalDateTime cursorTime) {
        if (keyword == null || keyword.isEmpty())
            return null;
        else if( keyword.matches(".*[<>!@#$%^&*(),.?\":{}|].*") )
            return null;

        List<SearchResponseDto> result = new ArrayList<>();
        result.addAll(
                jpaQueryFactory
                .select(Projections.constructor(SearchResponseDto.class,
                        qPost.id,
                        qPost.title,
                        qPost.view,
                        qPost.content,
                        qPost.thumbnailUrl,
                        qPost.createDate,
                        Expressions.constant("/detail/"))
                )
                .from(qPost)
                .where(
                        qPost.isShow.isTrue().and(
                                cursorTime == null
                                        ? qPost.title.contains(keyword)
                                        .or(qPost.keyword.contains(keyword))
                                        .or(qPost.content.contains(keyword))
                                        : qPost.createDate.before(cursorTime).and(
                                        qPost.title.contains(keyword)
                                                .or(qPost.keyword.contains(keyword))
                                                .or(qPost.content.contains(keyword))
                                )
                        )
                )
                .orderBy(qPost.modifiedDate.desc())
                .limit(limit)
                .fetch()
        );
        result.addAll(
                jpaQueryFactory
                        .select(Projections.constructor(SearchResponseDto.class,
                                qSnsContent.id,
                                qSnsContent.title,
                                qSnsContent.view,
                                qSnsContent.description,
                                qSnsContent.thumbnailUrl,
                                qSnsContent.publishTime,
                                Expressions.constant("/detail/sns-content/"))
                        )
                        .from(qSnsContent)
                        .where(
                                cursorTime == null
                                        ? qSnsContent.title.contains(keyword).or(qSnsContent.description.contains(keyword))
                                        : qSnsContent.publishTime.before(cursorTime).and(
                                                qSnsContent.title.contains(keyword)
                                                        .or(qSnsContent.description.contains(keyword)
                                                        )
                                                )

                        )
                        .orderBy(qSnsContent.publishTime.desc())
                        .limit(limit)
                        .fetch()
        );

        result.sort(Comparator.comparing(SearchResponseDto::getDateTime).reversed());
        return result;
    }
    public List<SimpleResponse> moreContents(Category category, long id) {
        List<SimpleResponse> dtoList = new ArrayList<>(
                jpaQueryFactory
                        .select(Projections.constructor(SimpleResponse.class,
                                qPost.id,
                                qPost.view,
                                qPost.title,
                                qPost.thumbnailUrl,
                                Expressions.constant("/detail/")
                        ))
                        .from(qPost)
                        .where(qPost.isShow.isTrue().and(
                                qPost.category.eq(category).and(qPost.id.ne(id))
                        ))
                        .limit(15)
                        .orderBy(qPost.id.asc())
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
    @Transactional
    public PostResponseDto getDetail(Long id) {
        Post post =  postRepo.findById(id)
                .orElseThrow(()->new NotFoundException("404 NOT FOUND"));
        post.increaseView();

        return new PostResponseDto(post);
    }

    //==================
    @Transactional
    public void create(HttpServletRequest request, PostRequestDto dto) {
        String key =  request.getHeader("authorization");
        if ( !key.equals(KEY) )
            throw new IllegalArgumentException();

        List<File> fileList = new ArrayList<>();

        if (!dto.getImgList().isEmpty()) {
            fileList.addAll(
                    jpaQueryFactory
                    .selectFrom(qFile)
                    .where(qFile.id.in(dto.getImgList()))
                    .fetch()
            );
            fileList.stream().forEach(file-> file.isUsed(true));
        }

        Post post = Post.builder()
                .createDate(LocalDateTime.now())
                .modifiedDate(LocalDateTime.now())
                .fileList(fileList)
                .category(dto.getCategory())
                .thumbnailUrl(dto.getThumbnailUrl())
                .title(dto.getTitle())
                .view(0)
                .description(dto.getDescription())
                .keyword(dto.getKeyword())
                .content(dto.getContent())
                .isShow(false)
                .build();

        postRepo.save(post);
    }
    @Transactional
    public String update(Category category) {

        return null;
    }

    @Transactional
    public void tempsns(long id, MultipartFile file) {
        SnsContent s = snsContentRepo.findById(id).orElseThrow();
        ImgResponse i = s3Service.uploadFile(file);
        s.updateThumbnail(i.getUrl());
    }

    public long countRequest() {
        String url = "https://aderspro.com/aders/japanlife/count"; // 실제 URL로 바꾸세요
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        headers.set("Authorization", "Bearer " + ACCESS_KEY); // 혹은 "X-API-KEY", "Api-Key" 등

        try {
            ResponseEntity<Long> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Long.class
            );
            Long count = response.getBody();
            if (count == null) {
                return 57;
            }
            return count;
            // 받아온 count 값으로 필요한 로직 처리
        } catch (Exception e) {
            return 57;
        }
    }

    public Map<Integer, Boolean> checkUpdate() {
        // 관심 있는 카테고리 매핑
        Map<Integer, Category> categoryMap = Map.of(
                0, Category.japan_review,
                1, Category.japan_property,
                2, Category.working_holiday,
                3, Category.japan_life
        );

        // 7일 전 기준
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(7).atStartOfDay();

        // 해당 기간 내 업데이트된 게시글 카테고리들 조회 (중복 제거)
        List<Category> updatedCategories = jpaQueryFactory
                .select(qPost.category)
                .distinct()
                .from(qPost)
                .where(
                        qPost.modifiedDate.goe(sevenDaysAgo),
                        qPost.category.in(categoryMap.values())
                )
                .fetch();

        // 결과 매핑
        Set<Category> updatedCategorySet = new HashSet<>(updatedCategories);

        return categoryMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> updatedCategorySet.contains(entry.getValue())
                ));
    }
//    @Transactional
//    public void tempsns(long id, MultipartFile file) {
//        Post s = postRepo.findById(id).orElseThrow();
//        ImgResponse i = s3Service.uploadFile(file);
//        s.
//    }

}
