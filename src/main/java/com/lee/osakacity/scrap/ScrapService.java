package com.lee.osakacity.scrap;

import com.amazonaws.services.kms.model.NotFoundException;
import com.lee.osakacity.config.CustomMultipartFile;
import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.restful.ImgResponse;
import com.lee.osakacity.dto.restful.PostRequestDto;
import com.lee.osakacity.service.PostService;
import com.lee.osakacity.service.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScrapService {
    private final RestTemplate restTemplate;
    private final S3Service s3Service;
    private final PostService postService;
    @Value("${jasypt.encryptor.password}")
    private String KEY;

    public PostRequestDto houberBlogScrapper(long id, Category category, HttpServletRequest request) throws IOException {
        String key =  request.getHeader("authorization");
        if ( !key.equals(KEY) )
            throw new IllegalArgumentException();

        // Step 1: 요청 보내서 메인 페이지 HTML 가져오기
        String mainUrl = "https://blog.naver.com/houber/" + id;
        ResponseEntity<String> response = restTemplate.getForEntity(mainUrl, String.class);
        String mainHtml = response.getBody();

        // Step 2: Jsoup으로 iframe URL 추출
        Document mainDocument = Jsoup.parse(mainHtml);
        Element iframe = mainDocument.selectFirst("iframe#mainFrame");
        if (iframe == null) {
            throw new NotFoundException("error1");
        }

        String iframeUrl = "https://blog.naver.com" + iframe.attr("src");

        // Step 3: iframe URL로 요청 보내기
        Document document = Jsoup.connect(iframeUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();


        PostRequestDto postRequestDto = new PostRequestDto();
        postRequestDto.setCategory(category);
        postRequestDto.setDescription(document.selectFirst("meta[property=og:description]").attr("content"));
        postRequestDto.setKeyword("하우버, houber, 일본, 생활, 일본 생활, 가이드");


        Element postListBody = this.divRemover(document);


        List<String> mainContent = this.extractMainContent(postListBody, postRequestDto);

        postRequestDto.setContent(String.join(System.lineSeparator(), mainContent));

        postService.create(request, postRequestDto);
        return postRequestDto;

    }
    private Element divRemover(Document document) {

        Element postListBody = document.getElementById("postListBody");
        // 특정 클래스를 가진 요소들 선택
        Elements elementsToRemoveBottom = postListBody != null ? postListBody.select("div.se-component-content-normal") : null;
        Elements elementsToRemoveSticker = postListBody != null ? postListBody.select("div.se-module-sticker") : null;

        if (elementsToRemoveBottom != null) {
            // 요소 삭제
            for (Element element : elementsToRemoveBottom) {
                element.remove();
            }
        }
        if (elementsToRemoveSticker != null) {
            for (Element element : elementsToRemoveSticker) {
                element.remove();
            }
        }

        return postListBody;
    }
    /**
     * @param postListBody html
     * @return List<html.text or img.src>
     */
    private List<String> extractMainContent(Element postListBody, PostRequestDto postRequestDto) {

        List<String> mainContent = new ArrayList<>();
        //타이틀
        String title = postListBody.selectFirst("div.se-title-text").text();
        postRequestDto.setTitle(title);


        Elements targetContent = postListBody.select("p.se-text-paragraph span, img.se-image-resource");
        boolean isFirstImage = true;
        for (Element t : targetContent) {
            if (t.tagName().equals("span")) {
                // <p> 태그의 텍스트 추가
                mainContent.add("<p class=\"center-text\">" + t.text() + "</p>");
            } else if (t.tagName().equals("img")) {
                // <img> 태그의 src값 처리
                ImgResponse localFile = this.convertImgSrcToMultipartFile(t.attr("src"));

                // 이미지 리스트에 추가
                postRequestDto.getImgList().add(localFile.getId());

                // 첫 번째 이미지를 썸네일로 설정
                if (isFirstImage) {
                    postRequestDto.setThumbnailUrl(localFile.getUrl());
                    isFirstImage = false; // 첫 번째 이미지 처리 완료

                    mainContent.add(0,
                            "<div class=\"detail-thumbnail\" style=\"background-color: none\">"
                            +"<img src=\""
                            +localFile.getUrl()
                            +"\" alt=\""
                            +localFile.getAlt()
                            +"\"> </div>"
                    );
                } else {
                    mainContent.add(
                            "<img src=\""
                            + localFile.getUrl()
                            + "\" alt=\""
                            + localFile.getAlt()
                            + "\">"
                    );
                }

            }
        }

        return mainContent;
    }

    private ImgResponse convertImgSrcToMultipartFile(String imageUrl) {
        try {
            String editedUrl = editImageUrl(imageUrl);
            // HTTP 요청으로 이미지 다운로드
            InputStream in = new URL(editedUrl).openStream();
            byte[] imageBytes = in.readAllBytes();
            String fileName = editedUrl.substring(imageUrl.lastIndexOf("/") + 1);

            MultipartFile multipartFile = new CustomMultipartFile(
                    imageBytes,
                    fileName,
                    fileName,
                    "image/jpeg"
            );

            return s3Service.uploadFile(multipartFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private String editImageUrl(String imageUrl) {
        // URL에서 '?' 이전까지 잘라내고, 새 파라미터 추가
        int queryIndex = imageUrl.indexOf("?");
        if (queryIndex != -1) {
            imageUrl = imageUrl.substring(0, queryIndex);
        }

        // 새로운 파라미터 추가
        return imageUrl + "?type=w773";
    }
}
