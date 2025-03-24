package com.lee.osakacity.ai.dto;

import com.lee.osakacity.ai.dto.custom.RoomType;
import com.lee.osakacity.ai.dto.custom.Structure;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchWebHook {
    private String location;
    private String transport;
    private String duration;
    private String radius;

    private Double minLat;
    private Double maxLat;
    private Double minLon;
    private Double maxLon;

    private List<RoomType> floorPlan;
    private float minArea;
    private float maxArea;
    private int minRentFee;
    private int maxRentFee;

    private boolean freeInternet;
    private boolean morePeople;
    private boolean petsAllowed;

    private List<Structure> deAllowedStructure; //미허용임
}
