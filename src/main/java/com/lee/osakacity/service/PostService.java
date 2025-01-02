package com.lee.osakacity.service;

import com.amazonaws.services.kms.model.NotFoundException;
import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.mvc.PostResponseDto;
import com.lee.osakacity.dto.mvc.SimpleResponse;
import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.infra.entity.File;
import com.lee.osakacity.infra.entity.Post;
import com.lee.osakacity.infra.entity.QFile;
import com.lee.osakacity.infra.entity.QPost;
import com.lee.osakacity.infra.repository.PostRepo;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
//    public List<SimpleResponse> getTopView() {
//
//    }
    public List<SimpleResponse> getList (Category category, Long cursorId, Integer cursorView) {

        List<Post> eList;

        if (category.equals(Category.HOT_POST)) {
            eList = jpaQueryFactory
                    .selectFrom(qPost)
                    .where(
                            (cursorId != null && cursorView != null)
                            ? qPost.view.lt(cursorView)
                                    .or(qPost.view.eq(cursorView).and(qPost.id.lt(cursorId)))
                                    : null
                    )
                    .orderBy(qPost.view.desc(), qPost.id.desc())
                    .limit(20)
                    .fetch();

        } else if (category.equals(Category.VIEW_ALL)) {

            eList = jpaQueryFactory
                    .selectFrom(qPost)
                    .where(
                            cursorId != null
                                ? qPost.id.lt(cursorId)
                                : null
                    )
                    .orderBy(qPost.id.desc())
                    .limit(20)
                    .fetch();
        } else {
            eList = jpaQueryFactory
                    .selectFrom(qPost)
                    .where(
                            cursorId != null
                                ? qPost.id.lt(cursorId)
                                : qPost.category.eq(category)
                    )
                    .orderBy(qPost.id.desc())
                    .limit(20)
                    .fetch();
        }


        return eList.stream().map(SimpleResponse::new).toList();
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
                .createDate(LocalDate.now())
                .modifiedDate(LocalDate.now())
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
