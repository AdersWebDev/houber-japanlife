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
    private Double minLat;
    private Double maxLat;
    private Double minLon;
    private Double maxLon;
    private float area;
    private int rentFee;
    private boolean freeInternet;
    private boolean morePeople;
    private boolean petsAllowed;
}
