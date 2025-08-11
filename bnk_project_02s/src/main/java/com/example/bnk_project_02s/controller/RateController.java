package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.dto.ForexChartDto;
import com.example.bnk_project_02s.dto.ForexRateDiffDto;
import com.example.bnk_project_02s.entity.Rate;
import com.example.bnk_project_02s.service.EximRateService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class RateController {

    @Autowired
    private RateService rateService;
    
    @Autowired
    private EximRateService eximRateService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/forex")
    public String showRates(Model model) {
        System.out.println("📄 [환율 조회] /forex 진입");

        List<ForexRateDiffDto> todayRates = rateService.getTodayRateViewDtos();
        model.addAttribute("rates", todayRates);

        return "forexView"; // 👉 templates/forexView.html
    }

    // ✅ [상세] 환율 상세 페이지
    @GetMapping("/forex/detail")
    public String getForexDetail(@RequestParam("currency") String currencyCode, Model model) throws JsonProcessingException {
        System.out.println("📄 [상세 환율] /forex/detail?currency=" + currencyCode);

        // 서비스가 '최근 영업일' 기준으로 돌려줌
        Rate today = rateService.getTodayRate(currencyCode);
        Rate yesterday = rateService.getYesterdayRate(currencyCode);

        if (today == null) { // DB에 데이터 자체가 없는 경우
            model.addAttribute("error", "환율 정보를 불러올 수 없습니다.");
            return "error";
        }

        // 전일이 없으면(첫 수집일 등) 전일=당일로 계산
        BigDecimal base = (yesterday != null) ? yesterday.getRvalue() : today.getRvalue();
        BigDecimal diff = today.getRvalue().subtract(base);
        BigDecimal diffPercent = (base.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ZERO
                : diff.divide(base, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        // 차트용 데이터 (최근 7일 / 30일) - 항상 JSON 문자열로 전달
        List<ForexChartDto> weekDto = rateService.getPastWeekRates(currencyCode).stream()
                .map(r -> new ForexChartDto(r.getRdate().toString(), r.getRvalue()))
                .collect(Collectors.toList());

        List<ForexChartDto> monthDto = rateService.getPastMonthRates(currencyCode).stream()
                .map(r -> new ForexChartDto(r.getRdate().toString(), r.getRvalue()))
                .collect(Collectors.toList());

        model.addAttribute("currencyCode", currencyCode);
        model.addAttribute("currencyName", getCurrencyName(currencyCode)); // 예: "미국 달러"
        model.addAttribute("todayRate", today.getRvalue());
        model.addAttribute("diff", diff.abs());
        model.addAttribute("diffPercent", diffPercent.abs().setScale(2, RoundingMode.HALF_UP));
        model.addAttribute("weekRatesJson", objectMapper.writeValueAsString(weekDto));
        model.addAttribute("monthRatesJson", objectMapper.writeValueAsString(monthDto));

        // 표시용 고시일(서비스의 '최근 영업일')
        model.addAttribute("displayDate", today.getRdate());
        // 전일 고시일(없을 수 있음)
        model.addAttribute("prevDisplayDate", (yesterday != null) ? yesterday.getRdate() : null);

        return "ForexDetailView"; // 👉 templates/ForexDetailView.html
    }
	 // ✅ 수동 수집 (재시도 포함)
    @GetMapping("/forex/collect")
    public String collectRatesManually(Model model) {
        System.out.println("⚡ [관리자 수동 수집] /forex/collect 호출");

        LocalDate bizDate = rateService.collectTodayWithRetry(); // ⬅️ 변경된 반환값 사용
        if (bizDate != null) {
            eximRateService.fetchAndSaveRatesForDate(bizDate);   // ⬅️ 같은 영업일로 CustomerRate 저장
        }

        List<ForexRateDiffDto> todayRates = rateService.getTodayRateViewDtos();
        model.addAttribute("rates", todayRates);
        return "forexView";
    }

    private String getCurrencyName(String code) {
        return switch (code) {
            case "USD" -> "미국 달러";
            case "JPY(100)" -> "일본 엔(100)";
            case "EUR" -> "유로";
            case "CNH" -> "위안화";
            case "GBP" -> "영국 파운드";
            case "CHF" -> "스위스 프랑";
            default -> code;
        };
    }
}
