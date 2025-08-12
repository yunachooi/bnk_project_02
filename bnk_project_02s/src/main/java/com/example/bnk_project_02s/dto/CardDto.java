package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardDto {
    private String cardno;
    private Integer cardcvc;
    private String cardname;
    private String pano;
    private String cuno;
    private String cardstatus;
    private LocalDateTime carddate;
}