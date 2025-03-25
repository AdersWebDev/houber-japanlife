package com.lee.osakacity.ai.dto;

import com.lee.osakacity.ai.dto.custom.RoomType;
import com.lee.osakacity.ai.dto.custom.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SimpleRoom {
    private Long id;
    private String thumbnailImg;
    private String roomNum;
    private Status status;
    private String dateOfMoveIn; // 특정 상태시
    private String dateOfPreliminaryInspection;
    private RoomType floorPlan;
    private float area;
    private int rentFee;
    private int managementFee;
    public SimpleRoom(Long id, String thumbnailImg, String roomNum, Status status, String dateOfMoveIn, String dateOfPreliminaryInspection,
                      RoomType floorPlan, float area, int rentFee, int managementFee) {
        this.id = id;
        this.thumbnailImg = thumbnailImg;
        this.roomNum = roomNum;
        this.status = status;
        this.dateOfMoveIn = dateOfMoveIn;
        this.dateOfPreliminaryInspection = dateOfPreliminaryInspection;
        this.floorPlan = floorPlan;
        this.area = area;
        this.rentFee = rentFee;
        this.managementFee = managementFee;
    }
}
