package com.example.bnk_project_02s.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MultiSeriesDto {
  private List<String> labels;
  private List<NamedSeriesDto> series; // [{name:'USD', data:[...]}...]
}