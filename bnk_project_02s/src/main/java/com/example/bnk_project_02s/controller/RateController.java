package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.dto.ForexChartDto;
import com.example.bnk_project_02s.dto.ForexRateDiffDto;
import com.example.bnk_project_02s.entity.Rate;
import com.example.bnk_project_02s.service.RateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RateController {

    @Autowired
    private RateService rateService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/forex")
    public String showRates(Model model) {
        System.out.println("📄 [환율 조회] /forex 진입");

        // ✅ 오늘 + 전일 비교된 DTO 리스트 조회
        List<ForexRateDiffDto> todayRates = rateService.getTodayRateViewDtos();

        model.addAttribute("rates", todayRates);
        return "forexView"; // 👉 templates/forexView.html
    }
 // ✅ [상세] 환율 상세 페이지
    @GetMapping("/forex/detail")
    public String getForexDetail(@RequestParam("currency") String currencyCode, Model model) throws JsonProcessingException {
        System.out.println("📄 [상세 환율] /forex/detail?currency=" + currencyCode);

        // 1. 오늘과 어제 환율 조회
        Rate today = rateService.getTodayRate(currencyCode);
        Rate yesterday = rateService.getYesterdayRate(currencyCode);

        if (today == null || yesterday == null) {
            model.addAttribute("error", "환율 정보를 불러올 수 없습니다.");
            return "error"; // 👉 error.html로 이동하거나 예외 처리
        }

        // 2. 변동 계산
        BigDecimal diff = today.getRvalue().subtract(yesterday.getRvalue());
        BigDecimal diffPercent = BigDecimal.ZERO;
        if (yesterday.getRvalue().compareTo(BigDecimal.ZERO) != 0) {
            diffPercent = diff
                    .divide(yesterday.getRvalue(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        // 3. 차트용 데이터 (최근 7일 / 30일)
        List<ForexChartDto> weekDto = rateService.getPastWeekRates(currencyCode).stream()
                .map(r -> new ForexChartDto(r.getRdate().toString(), r.getRvalue()))
                .collect(Collectors.toList());

        List<ForexChartDto> monthDto = rateService.getPastMonthRates(currencyCode).stream()
                .map(r -> new ForexChartDto(r.getRdate().toString(), r.getRvalue()))
                .collect(Collectors.toList());

        // 4. 모델에 담기
        model.addAttribute("currencyName", getCurrencyName(currencyCode)); // 예: "미국 달러"
        model.addAttribute("todayRate", today.getRvalue());
        model.addAttribute("diff", diff.abs());
        model.addAttribute("diffPercent", diffPercent.abs().setScale(2, RoundingMode.HALF_UP));
        model.addAttribute("weekRatesJson", objectMapper.writeValueAsString(weekDto));
        model.addAttribute("monthRatesJson", objectMapper.writeValueAsString(monthDto));

        return "ForexDetailView"; // 👉 templates/ForexDetailView.html
    }
    
    
    private String getCurrencyName(String code) {
        return switch (code) {
            case "USD" -> "미국 달러";
            case "JPY(100)" -> "일본 엔(100)";
            case "EUR" -> "유로";
            case "CNH" -> "위안화";
            case "GBP" -> "영국 파운드";
            case "CHF" -> "스위스 프랑";
            default -> code; // 기본값으로 코드 그대로 사용
        };
    }

}
