package com.example.bnk_project_02s.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ForexChartDto {
	private String date;
	private BigDecimal rvalue;

}
