package com.example.bnk_project_02s.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ShoppingLogDto {
	private Long slno;
    private Long spno;
    private String uno;
    private String pano;
    private String cuno;

    private Double slamount;
    private String slcurrency;
    private String slstatus;
    private String slmsg;
    private LocalDateTime slreqat;
    private LocalDateTime slcomat;
}
