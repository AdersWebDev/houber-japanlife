package com.lee.osakacity.ai.dto;

import com.lee.osakacity.ai.infra.Building;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BuildingDto {
    private String imageUrl;
    private String address;
    private String lineInfo;
    private List<RoomDto> rooms = new ArrayList<>();

    public BuildingDto (Building b) {
        this.imageUrl = b.getImageUrl();
        this.address = b.getDistrict().getKName();
        this.lineInfo = b.getLineInfo();
        this.rooms.addAll(b.getRooms().stream().map(RoomDto::new).toList());

    }
}
