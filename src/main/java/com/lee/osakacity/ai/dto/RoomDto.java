package com.lee.osakacity.ai.dto;

import com.lee.osakacity.ai.infra.Room;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomDto {
    private String roomNumber; //部屋名
    private String status; //状態
    private String dateOfMoveIn; //入居可能日
    private String dateOfPreliminaryInspection; //内覧可能日
    private String floorPlan; //間取
    private String area; //間取
    private String rentFee; //賃料
    private String managementFee; //管理費・共益費
    private String deposit; //敷金
    private String serviceFee; //礼金
    private String deposit2; //保証金

    public RoomDto(Room r) {
        this.roomNumber = r.getRoomNumber();
        this.floorPlan = r.getFloorPlanImg();
        this.status = r.getStatus().getDescription();
        this.dateOfMoveIn = r.getDateOfMoveIn();
        this.dateOfPreliminaryInspection = r.getDateOfPreliminaryInspection();
        this.floorPlan = r.getFloorPlan().getTitle();
        this.area = r.getArea() + "㎡";
        this.rentFee = r.getRentFee()+"￥";
        this.managementFee = r.getManagementFee() +"￥";
        this.deposit = r.getDeposit();
        this.serviceFee = r.getServiceFee();
        this.deposit2 = r.getDeposit2();
    }
}
