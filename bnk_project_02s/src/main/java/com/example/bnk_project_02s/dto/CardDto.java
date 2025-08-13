package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardDto {
    private String cardno;
    private String cano;
    private String uid;
    private Integer cardcvc;
    private String cardname;
    private String cardstatus;
    private LocalDateTime carddate;
}