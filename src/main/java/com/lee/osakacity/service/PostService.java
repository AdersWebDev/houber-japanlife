package com.lee.osakacity.service;

import com.amazonaws.services.kms.model.NotFoundException;
import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.PostResponseDto;
import com.lee.osakacity.dto.mvc.SimpleResponse;
import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.infra.entity.*;
import com.lee.osakacity.infra.repository.PostRepo;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepo postRepo;
    private final JPAQueryFactory jpaQueryFactory;
    @Value("${jasypt.encryptor.password}")
    private String KEY;

    QPost qPost = QPost.post;
    QFile qFile = QFile.file;
    QSnsContent qSnsContent = QSnsContent.snsContent;

    public List<SimpleResponse> getList (Category category,Long cursorId, Integer cursorView, LocalDateTime cursorTime) {
        int limit = 10;

        if (category.equals(Category.hot_post)) {
            // Post 데이터 가져오기
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
                            (cursorView != null && cursorId != null)
                                    ? qPost.view.lt(cursorView)
                                        .or(qPost.view.eq(cursorView).and(qPost.id.lt(cursorId)))
                                    : null
                    )
                    .orderBy(qPost.view.desc(), qPost.id.desc())
                    .limit(limit)
                    .fetch();

        } else if (category.equals(Category.all)) {

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
                            cursorTime != null ? qPost.modifiedDate.lt(cursorTime) : null
                    )
                    .orderBy(qPost.modifiedDate.desc())
                    .limit(limit / 2)
                    .fetch());

            // 📌 SnsContent 데이터 가져오기 (limit + extraFetch)
            List<SimpleResponse> sList = new ArrayList<>(jpaQueryFactory
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
                    .limit(limit / 2)
                    .fetch());

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

        } else if (category.equals(Category.houber_sns)) {
            return jpaQueryFactory
                    .select(Projections.constructor(SimpleResponse.class,
                            qSnsContent.id,
                            qSnsContent.view,
                            qSnsContent.title,
                            qSnsContent.thumbnailUrl,
                            Expressions.constant("/detail/sns-content")
                    ))
                    .from(qSnsContent)
                    .where(
                            cursorId != null ? qSnsContent.id.lt(cursorId) : null
                    )
                    .orderBy(qSnsContent.id.desc())
                    .limit(limit)
                    .fetch();

        } else {
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
                            cursorId != null
                                    ? qPost.category.eq(category).and(qPost.id.lt(cursorId))
                                    : qPost.category.eq(category)
                    )
                    .orderBy(qPost.id.desc())
                    .limit(limit)
                    .fetch();

        }
    }

    public PostResponseDto getDetail(Long id) {
        Post post =  postRepo.findById(id)
                .orElseThrow(()->new NotFoundException("404 NOT FOUND"));
        post.increaseView();

        return new PostResponseDto(post);
    }

    //==================
    @Transactional()
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
                .build();

        postRepo.save(post);
    }
    public String update(Category category) {

        return null;
    }
    public void delete(Long id) {

    }

}
