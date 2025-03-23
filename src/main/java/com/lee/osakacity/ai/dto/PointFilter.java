package com.lee.osakacity.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointFilter {
    /**
     * 사용자가 요청한 중심 지역명 (예: 혼마치)
     */
    private String location;

    /**
     * 이동 수단 (도보, 차량, 지하철)
     */
    private String transport;

    /**
     * 이동 시간 (예: '30분')
     */
    private String duration;

    /**
     * 계산된 반경 (예: '2.4km')
     */
    private String radius;

    /**
     * 영역의 최소 위도
     */
    private Double minLat;

    /**
     * 영역의 최대 위도
     */
    private Double maxLat;

    /**
     * 영역의 최소 경도
     */
    private Double minLon;

    /**
     * 영역의 최대 경도
     */
    private Double maxLon;
}
