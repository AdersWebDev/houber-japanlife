package com.lee.osakacity.admin;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.infra.entity.File;
import com.lee.osakacity.infra.entity.Post;
import com.lee.osakacity.infra.entity.QFile;
import com.lee.osakacity.infra.entity.QPost;
import com.lee.osakacity.infra.repository.PostRepo;
import com.lee.osakacity.service.PostService;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {
    /** 본문 최대 크기. DB 본문 칸이 TEXT(64KB)여도 들어가게 여유를 둠 (사진은 주소만 들어가서 보통 10KB 안팎) */
    static final int MAX_CONTENT_BYTES = 60_000;
    static final String DEFAULT_KEYWORD = "하우버, houber, 일본, 일본 생활, 일본 워홀, 워킹홀리데이, 오사카";

    private final PostRepo postRepo;
    private final PostService postService;
    private final JPAQueryFactory jpaQueryFactory;
    private final com.lee.osakacity.seo.IndexNowService indexNow;
    private final QPost qPost = QPost.post;
    private final QFile qFile = QFile.file;

    @Transactional(readOnly = true)
    public List<AdminPostRow> list(Category category) {
        return jpaQueryFactory
                .select(Projections.constructor(AdminPostRow.class,
                        qPost.id, qPost.title, qPost.category, qPost.isShow, qPost.modifiedDate, qPost.view, qPost.thumbnailUrl))
                .from(qPost)
                .where(category == null ? null : qPost.category.eq(category))
                .orderBy(qPost.id.desc())
                .fetch();
    }

    @Transactional(readOnly = true)
    public Post get(Long id) {
        return postRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "글을 찾을 수 없습니다"));
    }

    @Transactional
    public Long save(Long id, AdminPostRequest req) {
        if (req.category() == null) throw bad("카테고리를 골라 주세요");
        String title = req.title() == null ? "" : req.title().trim();
        if (title.isEmpty()) throw bad("제목을 입력해 주세요");
        if (title.length() > 200) throw bad("제목이 너무 깁니다 (200자 이하)");

        String content = HtmlSanitizer.clean(req.content());
        String text = HtmlSanitizer.text(content);
        List<String> images = HtmlSanitizer.imageUrls(content);
        if (text.isEmpty() && images.isEmpty()) throw bad("본문을 입력해 주세요");
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES)
            throw bad("본문이 너무 깁니다. 글을 두 개로 나눠 주세요");

        String thumbnail = req.thumbnailUrl() == null ? "" : req.thumbnailUrl().trim();
        if (thumbnail.isEmpty() && !images.isEmpty()) thumbnail = images.get(0);
        if (thumbnail.isEmpty()) throw bad("대표 사진을 정해 주세요");
        if (!thumbnail.startsWith("https://") && !thumbnail.startsWith("http://") || thumbnail.length() > 255)
            throw bad("대표 사진 주소가 올바르지 않습니다");

        String description = req.description() == null ? "" : req.description().trim().replaceAll("\\s+", " ");
        if (description.isEmpty()) description = text.length() > 120 ? text.substring(0, 120) + "…" : text;
        if (description.isEmpty()) description = title;
        if (description.length() > 250) description = description.substring(0, 250);

        // 이 글에 쓰인 사진을 '사용 중'으로 바꿔 둔다 (미사용 사진 정리 때 지워지지 않게, 글 삭제 때 함께 지워지게)
        List<String> urls = new ArrayList<>(images);
        urls.add(thumbnail);
        List<File> files = jpaQueryFactory.selectFrom(qFile).where(qFile.fileUrl.in(urls)).fetch();
        files.forEach(f -> f.isUsed(true));

        Post post;
        if (id == null) {
            post = Post.builder()
                    .createDate(LocalDateTime.now())
                    .modifiedDate(LocalDateTime.now())
                    .fileList(new ArrayList<>(files))
                    .category(req.category())
                    .thumbnailUrl(thumbnail)
                    .title(title)
                    .view(0)
                    .description(description)
                    .keyword(DEFAULT_KEYWORD)
                    .content(content)
                    .isShow(req.show())
                    .build();
            postRepo.save(post);
        } else {
            post = get(id);
            String keyword = post.getKeyword() == null || post.getKeyword().isBlank() ? DEFAULT_KEYWORD : post.getKeyword();
            post.update(req.category(), title, thumbnail, description, keyword, content);
            post.setShow(req.show());
            post.addFiles(files);
        }
        // 검색엔진(Bing·네이버 등)에 바뀐 주소 알림 (숨김 글은 404가 되었음을 알리는 용도)
        if (id != null || req.show()) { // 처음부터 숨김으로 저장한 새 글은 알릴 필요 없음
            indexNow.notifyChanged("/detail/" + post.getId(), "/list?category=" + req.category().name());
        }
        return post.getId();
    }

    @Transactional
    public void setShow(Long id, boolean show) {
        Post post = get(id);
        post.setShow(show);
        indexNow.notifyChanged("/detail/" + id, "/list?category=" + post.getCategory().name());
    }

    @Transactional
    public void delete(Long id) {
        Post post = get(id);
        String listPath = "/list?category=" + post.getCategory().name();
        postService.delete(id, null); // 기존 삭제 로직: 글에 연결된 S3 사진도 함께 지움
        indexNow.notifyChanged("/detail/" + id, listPath); // 삭제된 주소(404)도 알림
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
