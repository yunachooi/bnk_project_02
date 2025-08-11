package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardDto {
    private String cano;
    private Integer cacvc;
    private String caname;
    private String pano;
    private String cuno;
    private String castatus;
    private LocalDateTime cadate;
}