package com.example.bnk_project_02s.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildAccountRes {
    private String cano;
    private String pano;
    private String cuno;
    private String cuname;
    private java.math.BigDecimal balance;
}