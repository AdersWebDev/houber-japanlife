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


        List<String> mainContent = new ArrayList<>();
        mainContent.add( this.headerSeparator(postListBody, postRequestDto) );
        mainContent.addAll( this.test(postListBody, postRequestDto) );

        List<String> blankFilterList = this.processMainContent(mainContent);

        postRequestDto.setContent(String.join(System.lineSeparator(), blankFilterList));

        postService.create(request, postRequestDto);
        return postRequestDto;

    }
    public List<String> processMainContent(List<String> mainContent) {
        List<String> processedContent = new ArrayList<>();
        String previousType = null;
        int sameTypeCount = 0;

        for (String element : mainContent) {
            String currentType = getUnifiedType(element);

            if (currentType != null && currentType.equals(previousType)) {
                // 동일한 타입 그룹의 요소가 연속된 경우
                sameTypeCount++;
                if (sameTypeCount > 2) {
                    // 2개 초과 시 요소를 건너뜀 (삭제 효과)
                    continue;
                }
            } else {
                // 다른 타입 그룹이 나타나면 카운트 초기화
                sameTypeCount = 1;
            }

            // 요소를 결과 리스트에 추가
            processedContent.add(element);
            previousType = currentType;
        }

        return processedContent;
    }

    private String getUnifiedType(String element) {
        // 같은 그룹으로 처리할 타입 정의
        if (element.contains("<div class=\"blank\"></div>") || element.contains("<p class=\"center-text\"> \u200B</p>")) {
            return "group-blank-or-center";
        }
        return null; // 다른 요소는 null 처리
    }
    private Element divRemover(Document document) {

        Element postListBody = document.getElementById("postListBody");

        // 특정 텍스트를 포함하는 <span> 태그 찾기
        Elements elementsToRemove = document.select(
                "span:contains(하우버 블로그로 이동 (▲이미지 클릭)), " +
                        "span:contains(임대,매매,점포까지 모든 매물 정보를 하우버에서 확인해보세요), " +
                        "span:contains(하우버 카카오톡 채널로 이동 (▲이미지 클릭))"
        );

        int deleteCount = 0; // 삭제된 요소 개수를 카운트

        for (int i = elementsToRemove.size() - 1; i >= 0; i--) {
            Element element = elementsToRemove.get(i);
            Element parentComponent = element.closest(".se-component");

            if (parentComponent != null) {
                parentComponent.remove(); // 요소 삭제
                deleteCount++; // 삭제된 요소 개수 증가

                // 3개를 삭제하면 루프 종료
                if (deleteCount == 3) {
                    break;
                }
            }
        }

        Elements elementsToRemoveSticker = postListBody != null ? postListBody.select("div.se-module-sticker") : null;

        if (elementsToRemoveSticker != null) {
            for (Element element : elementsToRemoveSticker) {
                element.remove();
            }
        }


        return postListBody;
    }

    private List<String> test (Element postListBody, PostRequestDto postRequestDto) {
        List<String> mainContent = new ArrayList<>();


        Elements targetContent = postListBody.select("div.se-component");

        for (Element e : targetContent) {
            this.headSeparator(postRequestDto, e, mainContent);
        }

        return mainContent;
    }
    private void headSeparator(PostRequestDto dto, Element e, List<String> mainContent) {
        String className = e.className();
        if (className.contains("se-text")) {
            mainContent.addAll( this.seTextSeparator(e, mainContent) );
        } else if (className.contains("se-quotation")) {
            this.seQuotationSeparator(e, mainContent);
        } else if (className.contains("se-horizontalLine")) {
            this.seHorizontalLine(e, mainContent);
        } else if (className.contains("se-oglink")) { //링크
            this.seOgLinkSeparator(e, mainContent);
        } else if (className.contains("se-image") || className.contains("se-imageStrip")) {
            this.seImageSeparator(e, mainContent, dto);
        } else if (className.contains("se-table")) {
            this.seTableCreator(e, mainContent);
        }
        else {
            mainContent.addAll( this.seTextSeparator(e, mainContent) );
        }
    }

    private void seTableCreator(Element e, List<String> mainContent) {
        Element eTable = e.selectFirst("table");
        String table = "<table class=\"detail-table\"> <tbody>";
        Elements eTr = eTable.getElementsByTag("tr");

        for (Element tr : eTr) {
            table += "<tr>";
            Elements eTd = tr.getElementsByTag("td");

            for (Element td : eTd) {
                // 스타일 정보 가져오기
                String style = td.attr("style");
                table += "<td style=\"" + style + "\">";

                // <td> 내부의 <span> 태그 HTML만 추출
                StringBuilder spanContent = new StringBuilder();
                List<String> pList = this.seTextSeparator(td, mainContent);


                for (String p : pList) {
                    table += p;
                }
                table += "</td>";
            }

            table += "</tr>";
        }

        table += "</tbody></table>";

        mainContent.add(table);
    }

    private void seOgLinkSeparator(Element e, List<String> mainContent) {
        Element a = e.selectFirst("a");
        String title = e.getElementsByClass("se-oglink-title").text();
        String description = e.getElementsByClass("se-oglink-summary").text();
        String ogLinkBox =
                "<div class=\"embed-box\">\n" +
                    "<a href=\"" + a.attr("href")+ "\">" +
                        "<img src=\"" + a.selectFirst("img").attr("src") +"\"" +
                        " alt=\"short_cut_" + title + "\">" +
                        "<div class=\"embed-box text-area\">" +
                            "<p class=\"embed-box text-area text-title\">" + title + "</p>" +
                            "<p class=\"embed-box text-area text-description\">" + description +"</p>" +
                        "</div>" +
                    "</a>" +
                "</div>";
        mainContent.add(ogLinkBox);
    }
    //a href처리
    private void seImageSeparator(Element e, List<String> mainContent, PostRequestDto dto) {
        String className = e.className();
        if (className.contains("se-imageStrip2")) {
            Elements imgTag = e.getElementsByTag("img");

            List<ImgResponse> imgList = new ArrayList<>();
            for (Element img : imgTag) {
                imgList.add( this.convertImgSrcToMultipartFile( img.attr("src") ) );
            }
            String imgCol = "<div class=\"img-col\">";

            for (int i = 0; i < imgList.size(); i += 2) {
                imgCol += "<div class=\"img-row\">";

                // 현재와 다음 이미지를 추가 (다음 이미지가 존재할 경우만)
                imgCol += "<img src=\"" + imgList.get(i).getUrl() + "\" alt=\""+imgList.get(i).getAlt()+"\">";
                if (i + 1 < imgList.size()) {
                    imgCol += "<img src=\"" + imgList.get(i + 1).getUrl() + "\" alt=\""+imgList.get(i + 1).getAlt()+"\">";
                }
                imgCol += "</div>"; // img-row 닫기
            }
            imgCol += "</div>";

            for (ImgResponse localFile : imgList) {
                dto.getImgList().add(localFile.getId());
            }

            mainContent.add(imgCol);
        }
        else {
            Elements imgTag = e.getElementsByTag("img");
            for (Element img : imgTag) {
                ImgResponse localFie = this.convertImgSrcToMultipartFile(img.attr("src"));
                mainContent.add("<img src=\"" + localFie.getUrl() + "\"alt=\"" + localFie.getAlt() + "\"" + ">");
                dto.getImgList().add(localFie.getId());
            }
        }

    }

    private String headerSeparator(Element postListBody, PostRequestDto postRequestDto) {
        Element header = postListBody.selectFirst("div.se-component");
        Element title = header.selectFirst("div.se-title-text");
        postRequestDto.setTitle(title.text());
        header.remove();

        Element imgDivTag = postListBody.selectFirst("div.se-component");
        Element img = imgDivTag.selectFirst("img");

        ImgResponse localFile = this.convertImgSrcToMultipartFile( img.attr("src") );

        postRequestDto.setThumbnailUrl(localFile.getUrl());
        postRequestDto.getImgList().add(localFile.getId());

        imgDivTag.remove();

        return "<div class=\"detail-thumbnail\" style=\"background: none; aspect-ratio: auto\">"
                        +"<img style=\"max-width: 80%\" src=\""
                        +localFile.getUrl()
                        +"\" alt=\""
                        +localFile.getAlt()
                        +"\"> </div>";


    }
    private List<String> seTextSeparator(Element e, List<String> mainContent) {
        Elements tagP = e.getElementsByTag("p");
        List<String> pList = new ArrayList<>();
        for (Element p : tagP) {
            if (p.className().contains("se-text-paragraph-align-center")) {
                pList.add(spanSeparator(p, "<p class=\"center-text\">", "</p>", mainContent));
            } else if (p.className().contains("se-text-paragraph-align-left")) {
                pList.add(spanSeparator(p, "<p>", "</p>", mainContent));
            } else if (p.className().contains("se-text-paragraph-align-right")) {
                pList.add(spanSeparator(p, "<p style=\"text-align: right\"", "</p>", mainContent));
            } else {
                pList.add(spanSeparator(p, "<p>", "</p>", mainContent));
            }
        }
        return pList;

    }
    private String spanSeparator(Element p, String openTag, String closeTag, List<String> mainContent) {
       Elements tagSpan = p.getElementsByTag("span");
       if ( tagSpan.first().text().isBlank() ) {
           mainContent.add("<div class=\"blank\"></div>");
       }

       String tagPInnerHtml = openTag;

       for (Element span : tagSpan) {
           tagPInnerHtml += " "+span.html();
       }
       tagPInnerHtml += closeTag;

       return tagPInnerHtml;
    }
    private void seHorizontalLine(Element e, List<String> mainContent) {
        String className = e.className();

        if (className.contains("se-l-default"))
            mainContent.add("<hr class=\"line1\">");

         else if (className.contains("se-l-line1"))
            mainContent.add("<hr class=\"line2\">");

         else if (className.contains("se-l-line2"))
            mainContent.add("<hr class=\"line3\">");

         else if (className.contains("se-l-line3"))
            mainContent.add("<hr class=\"line4\">");

         else if (className.contains("se-l-line7"))
            mainContent.add("<hr class=\"line8\">");

         else mainContent.add("<hr class=\"line1\">");
    }

    private void seQuotationSeparator(Element e, List<String> mainContent) {
        String className = e.className();
        String quotation;
        if (className.contains("se-l-default")) {
            quotation =
                    "<div class=\"quote-container center\">\n"
                            + "<blockquote class=\"quote\">\n";
            List<String> pList = this.seTextSeparator(e, mainContent);
            for (String p : pList) {
                quotation += p+"\n";
            }

            quotation +=
                    "</blockquote>\n"
                    +"</div>";
            mainContent.add(quotation);

        }
        else if (className.contains("se-l-quotation_line")) {
            quotation = "<h3>";
            Elements spanList = e.getElementsByTag("span");
            for (Element s : spanList) {
                quotation += s.html();
            }
            quotation +="</h3>";
            mainContent.add(quotation);
        }
        else if (className.contains("se-l-quotation_corner")) {
            quotation = "<div class=\"middle-title-box\">\n<h2>";
            Elements spanList = e.getElementsByTag("span");
            for (Element s : spanList) {
                quotation += s.html();
            }
            quotation +="</h2>\n</div>";
            mainContent.add(quotation);
        } else { //나머지 전부 포스트잇
            quotation = "<div class=\"quote-container postit\">";

            List<String> pList = this.seTextSeparator(e, mainContent);
            for (String p : pList) {
                quotation += p +"\n";
            }
            quotation +="\n</div>";
            mainContent.add(quotation);
        }

    }

//    private List<String> extractMainContent(Element postListBody, PostRequestDto postRequestDto) {
//
//        List<String> mainContent = new ArrayList<>();
//        //타이틀
//        Element title = postListBody.selectFirst("div.se-title-text");
//        postRequestDto.setTitle(title.text());
//        title.remove();
//
//
//        Elements targetContent = postListBody.select("p.se-text-paragraph span, img.se-image-resource");
//        boolean isFirstImage = true;
//        for (Element t : targetContent) {
//            if (t.tagName().equals("span")) {
//                // <p> 태그의 텍스트 추가
//                mainContent.add("<p class=\"center-text\">" + t.text() + "</p>");
//                t.remove();
//            }
//            else if (t.tagName().equals("img")) {
//                // <img> 태그의 src값 처리
//                ImgResponse localFile = this.convertImgSrcToMultipartFile(t.attr("src"));
//
//                // 이미지 리스트에 추가
//                postRequestDto.getImgList().add(localFile.getId());
//
//                // 첫 번째 이미지를 썸네일로 설정
//                if (isFirstImage) {
//                    postRequestDto.setThumbnailUrl(localFile.getUrl());
//                    isFirstImage = false; // 첫 번째 이미지 처리 완료
//
//                    mainContent.add(0,
//                            "<div class=\"detail-thumbnail\" style=\"background-color: none\">"
//                            +"<img src=\""
//                            +localFile.getUrl()
//                            +"\" alt=\""
//                            +localFile.getAlt()
//                            +"\"> </div>"
//                    );
//                } else {
//                    mainContent.add(
//                            "<img src=\""
//                            + localFile.getUrl()
//                            + "\" alt=\""
//                            + localFile.getAlt()
//                            + "\">"
//                    );
//                }
//
//            }
//        }
//
//        return mainContent;
//    }
//
private ImgResponse convertImgSrcToMultipartFile(String imageUrl) {
    try {
        String editedUrl = editImageUrl(imageUrl);
        return downloadAndUploadImage(editedUrl);
    } catch (IOException e) {
        // 로그 출력
        System.err.println("첫 번째 요청 실패: " + e.getMessage());
        try {
            // 원본 URL로 재요청
            System.out.println("원본 URL로 재시도: " + imageUrl);
            return downloadAndUploadImage(imageUrl);
        } catch (IOException retryException) {
            // 두 번째 요청도 실패한 경우 예외 던지기
            System.err.println("두 번째 요청 실패: " + retryException.getMessage());
            throw new RuntimeException("이미지 처리 실패: " + retryException.getMessage(), retryException);
        }
    }
}

    private ImgResponse downloadAndUploadImage(String url) throws IOException {
        // HTTP 요청으로 이미지 다운로드
        InputStream in = new URL(url).openStream();
        byte[] imageBytes = in.readAllBytes();
        String fileName = url.substring(url.lastIndexOf("/") + 1);

        MultipartFile multipartFile = new CustomMultipartFile(
                imageBytes,
                fileName,
                fileName,
                "image/jpeg"
        );

        return s3Service.uploadFile(multipartFile);
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
