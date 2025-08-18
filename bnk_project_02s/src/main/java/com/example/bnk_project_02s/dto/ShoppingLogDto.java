package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingLogDto {
    private Long slno;
    private String uid;
    private String spno;
    private String cardno;
    private String slamount;
    private String slcurrency;
    private String slstatus;
    private String slreason;
    private LocalDateTime slreqat;
    private LocalDateTime slcomat;
}