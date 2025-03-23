package com.lee.osakacity.ai.infra;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
public class Building {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private Long buildingId;
    private String imageUrl;
    private String buildingName;
    private String address;
    private String lineInfo;
    private String contactInfo;
    private String contactTel;

    @ManyToOne
    @JoinColumn(name = "district_id")
    private District district;
    @OneToMany(mappedBy = "building", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

}
