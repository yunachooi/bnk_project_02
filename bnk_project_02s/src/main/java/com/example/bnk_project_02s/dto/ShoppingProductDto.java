package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ShoppingProductDto {
    private String spno;
    private String spname;
    private String spnameKo;
    private String spdescription;
    private String spdescriptionKo;
    private Double spprice;
    private String spcurrency;
    private Double sprating;
    private Integer spreviews;
    private String spimgurl;
    private String spurl;
    private LocalDateTime spat;
}