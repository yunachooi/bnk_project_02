package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.dto.ForexChartDto;
import com.example.bnk_project_02s.dto.ForexRateDiffDto;
import com.example.bnk_project_02s.entity.Rate;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.EximRateService;
import com.example.bnk_project_02s.service.RateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
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

    private static final String LOGIN_USER = "LOGIN_USER"; // UserController와 동일 키 사용

    @GetMapping("/forex")
    public String showRates(Model model) {
        System.out.println("📄 [환율 조회] /forex 진입");
        List<ForexRateDiffDto> todayRates = rateService.getTodayRateViewDtos();
        model.addAttribute("rates", todayRates);
        return "forexView"; // 👉 templates/forexView.html (비회원도 접근 가능)
    }
    @GetMapping("/account/foreign1")
    public String showForeignAccountJoin() {
        return "account/foreign1"; // templates/account/foreign0.html
    }

    // ✅ [상세] 환율 상세 페이지
    // - 로그인 X: 리다이렉트하지 않고 loginRequired=true 플래그만 내려서 모달로 안내
    // - 로그인 O & 상품 미가입: needSubscription=true 플래그 내려서 모달로 안내
    @GetMapping("/forex/detail")
    public String getForexDetail(@RequestParam("currency") String currencyCode,
                                 Model model,
                                 HttpSession session) throws JsonProcessingException {
        System.out.println("📄 [상세 환율] /forex/detail?currency=" + currencyCode);

        // 1) 로그인/가입 상태 플래그 계산 (리다이렉트 없이 화면에서 모달 처리)
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        boolean loginRequired    = (loginUser == null);
        boolean needSubscription = (!loginRequired) && !"Y".equalsIgnoreCase(loginUser.getUcheck());

        model.addAttribute("loginRequired", loginRequired);
        model.addAttribute("needSubscription", needSubscription);

        // 2) 상세 데이터 구성
        Rate today = rateService.getTodayRate(currencyCode);
        Rate yesterday = rateService.getYesterdayRate(currencyCode);

        if (today == null) { // DB에 데이터 자체가 없는 경우
            model.addAttribute("error", "환율 정보를 불러올 수 없습니다.");
            return "error";
        }

        BigDecimal base = (yesterday != null) ? yesterday.getRvalue() : today.getRvalue();
        BigDecimal diff = today.getRvalue().subtract(base);
        BigDecimal diffPercent = (base.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ZERO
                : diff.divide(base, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        List<ForexChartDto> weekDto = rateService.getPastWeekRates(currencyCode).stream()
                .map(r -> new ForexChartDto(r.getRdate().toString(), r.getRvalue()))
                .collect(Collectors.toList());

        List<ForexChartDto> monthDto = rateService.getPastMonthRates(currencyCode).stream()
                .map(r -> new ForexChartDto(r.getRdate().toString(), r.getRvalue()))
                .collect(Collectors.toList());

        model.addAttribute("currencyCode", currencyCode);
        model.addAttribute("currencyName", getCurrencyName(currencyCode));
        model.addAttribute("todayRate", today.getRvalue());
        model.addAttribute("diff", diff.abs());
        model.addAttribute("diffPercent", diffPercent.abs().setScale(2, RoundingMode.HALF_UP));
        model.addAttribute("weekRatesJson", objectMapper.writeValueAsString(weekDto));
        model.addAttribute("monthRatesJson", objectMapper.writeValueAsString(monthDto));
        model.addAttribute("displayDate", today.getRdate());
        model.addAttribute("prevDisplayDate", (yesterday != null) ? yesterday.getRdate() : null);

        return "ForexDetailView"; // 👉 templates/ForexDetailView.html
    }

    // ✅ 수동 수집 (재시도 포함) — 권한 가드 필요 시 별도 처리
    @GetMapping("/forex/collect")
    public String collectRatesManually(Model model) {
        System.out.println("⚡ [관리자 수동 수집] /forex/collect 호출");

        LocalDate bizDate = rateService.collectTodayWithRetry();
        if (bizDate != null) {
            eximRateService.fetchAndSaveRatesForDate(bizDate);
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
