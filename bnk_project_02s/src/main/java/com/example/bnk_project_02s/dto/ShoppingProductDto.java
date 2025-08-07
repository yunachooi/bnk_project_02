package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ShoppingProductDto {
	private String spno;
    private String spname;
    private String spdescription;
    private Double spprice;
    private String spcurrency;
    private Double sprating;
    private Integer spreviews;
    private String spurl;
    private LocalDateTime spat;
}
