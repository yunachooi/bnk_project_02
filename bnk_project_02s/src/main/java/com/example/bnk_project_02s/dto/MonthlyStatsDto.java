package com.example.bnk_project_02s.dto;

import java.util.List;
//월별 리뷰수/평점 차트용
public record MonthlyStatsDto(List<String> labels, List<Integer> counts, List<Double> avgRatings) {}
