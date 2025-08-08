package com.example.bnk_project_02s.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForexRateDiffDto {
	
	private String rcode;
    private String rcurrency;
    private BigDecimal rvalue;
    private BigDecimal ryesterday;
    private int diffFlag; // 1: 상승(빨강), -1: 하락(파랑), 0: 동일

}
