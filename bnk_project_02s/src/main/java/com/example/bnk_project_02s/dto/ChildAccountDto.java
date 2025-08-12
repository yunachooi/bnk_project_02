package com.example.bnk_project_02s.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChildAccountDto {
    private String cano;
    private String pano;
    private String cuno;
    private LocalDate cajoin;
    private String cabalance;
    private String pabank;
}