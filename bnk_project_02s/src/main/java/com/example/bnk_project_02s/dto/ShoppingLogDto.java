package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShoppingLogDto {
    private Long slno;
    private String uid;
    private String spno;
    private String cardno;
    private String slamount;
    private String slcurrency;
    private String slexrate;
    private String slstatus;
    private LocalDateTime slrequest;
    private LocalDateTime sldate;
}