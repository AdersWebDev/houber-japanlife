package com.lee.osakacity.ai.service;

import com.lee.osakacity.ai.dto.custom.RoomType;
import com.lee.osakacity.ai.dto.custom.Status;
import com.lee.osakacity.ai.dto.custom.Structure;
import com.lee.osakacity.ai.infra.Building;
import com.lee.osakacity.ai.infra.repo.BuildingRepo;
import com.lee.osakacity.ai.infra.Room;
import com.lee.osakacity.ai.infra.repo.RoomRepo;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealProService {
    private final RestTemplate restTemplate;
    private final BuildingRepo buildingRepo;
    private final RoomRepo roomRepo;
    @Value("${realnetpro.id}")
    private String realId;
    @Value("${realnetpro.pw}")
    private String realPw;

    @Transactional
    public void realNetReport() {
        String base_url = "https://www.realnetpro.com";

        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36");
        headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // form-data 본문 데이터 생성
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id",realId);
        formData.add("pass",realPw);

        HttpEntity<MultiValueMap<String, String>> loginForm = new HttpEntity<>(formData,headers);
        ResponseEntity<String> loginResponse = restTemplate.exchange(base_url+"/index.php", HttpMethod.POST, loginForm, String.class);

        Random random = new Random();
        try {
            int delay = 1000 + random.nextInt(1000);  // 1초 ~ 2초 사이 랜덤
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<String> cookieValue = new ArrayList<>();
        if (loginResponse.getHeaders().get("Set-Cookie") != null) {
            cookieValue.addAll(loginResponse.getHeaders().get("Set-Cookie"));
        } else {
            throw new RuntimeException("loginFail");
        }
        ////////////////////////
        headers.put(HttpHeaders.COOKIE, cookieValue);
        formData.add("ini_pref_name","大阪府");
        formData.add("ini_pref","27");
//        formData.add("keyword",keyword);
        formData.add("page_method","estate");
        formData.add("page_type","building");
        formData.add("company_id", "41096,19069,23474,32478,11208");
//        formData.add("cnt","1");
        HttpEntity<MultiValueMap<String,String>> searchForm= new HttpEntity<>(formData,headers);
        ResponseEntity<String> firstResponse = restTemplate.exchange(base_url+"/search_cookie.php",HttpMethod.POST,searchForm, String.class);
        try {
            int delay = 1500 + random.nextInt(1000);  // 1.5초 ~ 2.5초 사이 랜덤
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HttpHeaders headers2 = new HttpHeaders();
        headers2.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36");
        headers2.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

        List<String> finalValue = new ArrayList<>();

        if (loginResponse.getHeaders().get("Set-Cookie") != null) {
            finalValue.addAll(firstResponse.getHeaders().get("Set-Cookie"));
        } else {
            throw new RuntimeException("loginFail");
        }
        headers2.put(HttpHeaders.COOKIE, finalValue);

        HttpEntity<String> finalForm= new HttpEntity<>(headers);
        ResponseEntity<String> searchResponse = restTemplate.exchange(base_url+"/main.php?method=estate&display=building&cnt=200&page=3",HttpMethod.GET,finalForm, String.class);


        Document doc = Jsoup.parse(searchResponse.getBody());

        // 데이터 추출 (예: CSS 셀렉터를 통해 특정 데이터 가져오기)
        Element mainContent = doc.selectFirst("div.main_contents");
        Elements buildingElement = mainContent.select("div.one_building");

        //dto 매핑
        for (Element b : buildingElement) {
            Building building = this.buildingConstruct(b);

            Elements roomElements = b.select("tr.room_info_tr");
            List<Room> rooms = this.roomConstruct(building, roomElements);

            building.getRooms().addAll(rooms);
        }

    }
    private Building buildingConstruct(Element buildingElement) {
//        BuildingDto building = new BuildingDto();
        Building b = new Building();
        //match building id
        String buildIdHref = buildingElement.selectFirst("td.building_data > a").attr("href");
        b.setBuildingId(
                Long.parseLong(
                        buildIdHref.replaceAll((".+id=(\\d+)"), "$1")
                )
        );
        //match building img
        b.setImageUrl(buildingElement.selectFirst("img.building_photo").attr("src"));
        //match building name
        b.setBuildingName(
                Optional.ofNullable(buildingElement.selectFirst("div.building_name"))
                        .map(Element::ownText)
                        .orElse("No building name")
        );
        // Address와 Line Info 추출
        Elements infoDivs = buildingElement.select("td.building_info > div");
        if (infoDivs.size() > 1) {  // 두 번째 div가 있는지 확인
            Element addressLineElement = infoDivs.get(1); // 두 번째 div 요소 선택
            String[] addressLineSplit = addressLineElement.html().split("<br>");
            b.setAddress(
                    addressLineSplit.length > 0 ? addressLineSplit[0].replace("住所：", "").trim() : "No address"
            );
            b.setLineInfo(
                    addressLineSplit.length > 1 ? addressLineSplit[1].replace("沿線：", "").trim() : "No line info"
            );
        }

        // Contact Info 및 Tel 추출
        Element contactElement = buildingElement.selectFirst("td.building_company");
        if (contactElement != null) {
            String contactText = contactElement.text();
            String[] contactSplit = contactText.split("Tel :");
            b.setContactInfo(contactSplit[0].replace("お問合せ先", "").trim());
            b.setContactTel(contactSplit.length > 1 ? contactSplit[1].trim() : "No contact number");
        }
        buildingRepo.save(b);
        return b;
    }
    private List<Room> roomConstruct (Building b,Elements roomElements) {
        List<Room> rList = new ArrayList<>();

        for (Element roomElement : roomElements) {
            Room r = new Room();
            String onClickValue = roomElement.selectFirst("td.history_bg span.browsing_date").attr("id");
            r.setRoomId(
                    Long.parseLong(
                            onClickValue.replaceAll("\\D+", "")
                    )
            );
            r.setCreateDate(LocalDate.now());
            r.setUpdateTime(LocalDateTime.now());

            r.setRoomNumber(roomElement.selectFirst("a.room_number").text());
            r.setEnquiryDesignation(roomElement.select("td").get(2).text());
            r.setThumbnail(roomElement.selectFirst("img.room_layout_image").attr("src"));

            Elements statusElements = roomElement.select("td.st_td span.st");

        // 상태 텍스트 리스트로 변환
            List<String> stateTexts = statusElements.stream()
                    .map(Element::text)
                    .collect(Collectors.toList());

        // 상태가 없을 경우 기본값 처리
            if (stateTexts.isEmpty()) {
                r.setStatus(Status.T9);  // 기본 상태 "不可"
            } else {
                // 마지막 상태 텍스트
                String lastStateText = stateTexts.get(stateTexts.size() - 1);

                // 텍스트를 Status Enum으로 매핑
                Status status = Arrays.stream(Status.values())
                        .filter(s -> s.getTitle().equals(lastStateText))
                        .findFirst()
                        .orElse(Status.T9);  // 일치하는 게 없으면 "不可"

                // DB에 상태 저장
                r.setStatus(status);
            }

            List<String> infoSplit = new ArrayList<>();
            for (int i =0 ; i < 5; i ++) {
                String[] data = roomElement.select("td").get(i+5).html().split("<br>");

                for (int j =0; j < 2; j++) {
                    infoSplit.add(data[j].replaceAll("<[^>]*>", "").trim());
                }
            }
            r.setDateOfMoveIn(infoSplit.get(0));
            r.setDateOfPreliminaryInspection(infoSplit.get(1));
            r.setFloorPlan(
                    RoomType.of(infoSplit.get(2)
                    )
            );
            String areaStr = infoSplit.get(3).trim().replace("㎡", "").trim();
            float area = Float.parseFloat(areaStr);
            r.setArea(area);

// rentFee 처리
            String rentStr = infoSplit.get(4).trim().replaceAll("[^\\d]", "");
            int rentFee = rentStr.isEmpty() ? 0 : Integer.parseInt(rentStr);
            r.setRentFee(rentFee);

// managementFee 처리
            String mgmtStr = infoSplit.get(5).trim().replaceAll("[^\\d]", "");
            int managementFee = mgmtStr.isEmpty() ? 0 : Integer.parseInt(mgmtStr);
            r.setManagementFee(managementFee);

            r.setDeposit(infoSplit.get(6));
            r.setServiceFee(infoSplit.get(7));
            r.setDeposit2(infoSplit.get(8));
            r.setRepaymentAndReturn(infoSplit.get(9));

            r.setAdvertiseTip(roomElement.select("td").get(10).text().trim());
            r.setBuilding(b);

            rList.add(r);
        }
        roomRepo.saveAll(rList);
        return rList;
    }

    @Transactional
    public void realNetDetail() {
        String base_url = "https://www.realnetpro.com";

        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36");
        headers.add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // form-data 본문 데이터 생성
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id",realId);
        formData.add("pass",realPw);

        HttpEntity<MultiValueMap<String, String>> loginForm = new HttpEntity<>(formData,headers);
        ResponseEntity<String> loginResponse = restTemplate.exchange(base_url+"/index.php", HttpMethod.POST, loginForm, String.class);

        List<String> cookieValue = new ArrayList<>();
        if (loginResponse.getHeaders().get("Set-Cookie") != null) {
            cookieValue.addAll(loginResponse.getHeaders().get("Set-Cookie"));
        } else {
            throw new RuntimeException("loginFail");
        }
        Random random = new Random();
        List<Room> roomList = roomRepo.findAll();
        headers.put(HttpHeaders.COOKIE, cookieValue);
        for (Room r : roomList) {
            System.out.println(r.getId());

            HttpEntity<MultiValueMap<String,String>> detailForm= new HttpEntity<>(headers);
            ResponseEntity<String> searchResponse = restTemplate.exchange(base_url+"/room_detail.php?id="+r.getRoomId(),HttpMethod.GET, detailForm, String.class);
            Document doc = Jsoup.parse(searchResponse.getBody());
            // body에만 문구가 있는 경우
            String bodyText = doc.body().text();

            if (bodyText.contains("この部屋の情報は入居中であるか公開されていません。")) {
                System.out.println("비공개/입주중인 방입니다. Room ID: " + r.getId());

                r.setStatus(Status.T9);
                continue;
            }

            photoHandler(r, doc.selectFirst("div.image_list"));
            infoHandler(r, doc.selectFirst("table.basic_table > tbody"));
            mapHandler(r, doc.getElementById("maparea2"));
            optionHandler(r, doc.getElementById("eq"));

            try {
                int delay = 2500 + random.nextInt(500);  // 1.5초 ~ 2.5초 사이 랜덤
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                break;
            }

        }
    }
    private void photoHandler(Room r, Element photoContent) {

        Elements images = photoContent.select("div.image_list img.img");
        //ThumbNail selector
        if (!images.isEmpty()) {
            Element firstImage = images.get(0);
            String thumbnailUrl = firstImage.attr("src");
            r.setThumbnail(thumbnailUrl);
            images.remove(0); // 첫 번째 이미지를 리스트에서 제거하여 나머지 이미지만 처리
        }
        //normal photo separator
        Iterator<Element> iterator = images.iterator();
        while (iterator.hasNext()) {
            Element img = iterator.next();
            String imageUrl = img.attr("src");
            String title = img.attr("title");

            if (title.equals("間取図_none")) {
                r.setFloorPlanImg(imageUrl);
                iterator.remove(); // 안전하게 제거
            } else if (title.equals("その他_none")) {
                iterator.remove(); // 안전하게 제거
            }
        }
        for (int i = 0; i < 8; i++) {
            String imageUrl = null;
            if (i < images.size()) {
                imageUrl = images.get(i).attr("src");
            }

            switch (i) {
                case 0: r.setImg1(imageUrl); break;
                case 1: r.setImg2(imageUrl); break;
                case 2: r.setImg3(imageUrl); break;
                case 3: r.setImg4(imageUrl); break;
                case 4: r.setImg5(imageUrl); break;
                case 5: r.setImg6(imageUrl); break;
                case 6: r.setImg7(imageUrl); break;
                case 7: r.setImg8(imageUrl); break;
            }
        }

    }
    private void infoHandler(Room r, Element roomInfo) {
        Elements rows = roomInfo.select("tr"); // 모든 행(tr) 가져오기

        for (Element row : rows) {
            Element labelCell = row.selectFirst("td.td_m"); // 항목 이름
            Element valueCell = row.select("td").last();    // 항목 값 (labelCell 다음 td)

            if (labelCell == null || valueCell == null) continue;

            String label = labelCell.text().trim();
            String value = valueCell.text().trim();

            switch (label) {

                case "構造":
                    if ( value.contains("鉄筋") ) {
                        r.setStructure(Structure.REBAR);
                    } else if ( value.contains("鉄骨") ) {
                        r.setStructure(Structure.IRON_FRAME);
                    } else if ( value.contains("木造") ) {
                        r.setStructure(Structure.WOOD_CARVING);
                    } else {
                        r.setStructure(Structure.OTHER);
                    }
                    break;

                case "方位":
                    String rawDirection = value.replace("向き", "").trim(); // "向き" 제거 및 공백 제거
                    r.setDirection(translateDirection(rawDirection)); // 번역된 방향 저장
                    break;
            }
        }
    }
    private void mapHandler(Room r, Element mapInfo) {
        String mapLatLon = mapInfo.attr("src");
        // src 문자열에서 좌표를 추출
        String coordinates = mapLatLon.split("q=")[1].split("&")[0]; // "q=" 다음 부분과 "&" 이전 부분 추출
        String[] latLng = coordinates.split(","); // 쉼표를 기준으로 위도와 경도 분리

        if (latLng.length >= 2) {
            r.setLat(safeParseDouble(latLng[0].trim()));
            r.setLon(safeParseDouble(latLng[1].trim()));
        }
    }
    private Double safeParseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Double 변환 실패! 값: " + value);
            return null;
        }
    }
    public void optionHandler(Room r, Element eq) {
        // "one_eq_name" 클래스에 해당하는 모든 <span> 태그 가져오기
        Elements spans = eq.select(".one_eq_name");

        for (Element span : spans) {
            String text = span.text(); // <span> 내부 텍스트
            if (text.contains("○")) { // "○"가 포함된 텍스트 처리
                // "○"를 기준으로 분리하고, 각 항목에서 공백 제거 후 처리
                String[] parts = text.split("○");
                for (String part : parts) {
                    String trimmedPart = part.trim();
                    if (!trimmedPart.isEmpty()) { // 빈 문자열 무시
                        if (trimmedPart.contains("ネット使用料不要"))
                            r.setFreeInternet(true);
                        else if (trimmedPart.contains("ペット相談"))
                            r.setPetsAllowed(true);
                        else if (trimmedPart.contains("2人入居可能"))
                            r.setMorePeople(true);
                        else if (trimmedPart.contains("駅近"))
                            r.setCloseToStation(true);
                    }
                }
            }
        }
    }
    private String translateDirection(String rawDirection) {
        if (rawDirection == null || rawDirection.isBlank()) {
            return "알 수 없음";
        }

        Map<String, String> directionCharMap = new HashMap<>();
        directionCharMap.put("北", "북");
        directionCharMap.put("南", "남");
        directionCharMap.put("東", "동");
        directionCharMap.put("西", "서");

        // Set을 사용해서 중복 방지 및 순서 정렬 가능
        Set<String> translatedParts = new LinkedHashSet<>();

        // 문자 단위로 자르기
        for (int i = 0; i < rawDirection.length(); i++) {
            String ch = String.valueOf(rawDirection.charAt(i));
            String translated = directionCharMap.get(ch);
            if (translated != null) {
                translatedParts.add(translated);
            }
        }

        if (translatedParts.isEmpty()) {
            return "알 수 없음";
        }

        StringBuilder result = new StringBuilder();
        for (String part : translatedParts) {
            result.append(part);
        }

        result.append("향");

        return result.toString();
    }
}
