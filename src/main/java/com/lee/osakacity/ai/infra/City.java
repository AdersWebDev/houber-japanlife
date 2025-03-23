package com.lee.osakacity.ai.infra;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "city")
    private List<District> districtList = new ArrayList<>();


    private String kName;
    private String jName;
}

