package com.lee.osakacity.ai.infra;

import com.lee.osakacity.ai.dto.custom.RoomType;
import com.lee.osakacity.ai.dto.custom.Status;
import com.lee.osakacity.ai.dto.custom.Structure;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate createDate;
    private LocalDateTime updateTime;
    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(unique = true)
    private Long roomId;

    private String roomNumber; //部屋名
    private String enquiryDesignation; //問合せ指定
    private Status status; //状態

    private String dateOfMoveIn; //入居可能日
    private String dateOfPreliminaryInspection; //内覧可能日
    private RoomType floorPlan; //間取
    private float area; //間取
    private int rentFee; //賃料
    private int managementFee; //管理費・共益費
    private String deposit; //敷金
    private String serviceFee; //礼金
    private String deposit2; //保証金
    private String repaymentAndReturn; //償却・敷引
    private String advertiseTip;

    //추가할 부분
    private Double lat;
    private Double lon;
    private String direction;
    private Structure structure;

    private boolean freeInternet;
    private boolean petsAllowed;
    private boolean morePeople;
    private boolean closeToStation;

    private String thumbnail;
    private String floorPlanImg;
    private String img1;
    private String img2;
    private String img3;
    private String img4;
    private String img5;
    private String img6;
    private String img7;
    private String img8;
}
