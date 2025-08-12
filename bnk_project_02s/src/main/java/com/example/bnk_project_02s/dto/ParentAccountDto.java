package com.example.bnk_project_02s.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParentAccountDto {
    private String pano;
    private String uid;
    private Integer fno;
    private LocalDate pajoin;
    private String pabank;
}