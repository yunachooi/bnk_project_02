package com.example.bnk_project_02s.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NamedSeriesDto {
  private String name;                 // 통화 코드(예: USD/JPN) 또는 cuno
  private List<BigDecimal> data;       // labels 길이 동일
}