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
import com.example.bnk_project_02s.repository.ParentAccountRepository;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.CardRepository;

@Controller
public class RateController {

    @Autowired
    private RateService rateService;

    @Autowired
    private EximRateService eximRateService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LOGIN_USER = "LOGIN_USER"; // UserController와 동일 키 사용
    
    @Autowired
    private ParentAccountRepository parentAccountRepository;

    @Autowired
    private ChildAccountRepository childAccountRepository;

    @Autowired
    private CardRepository cardRepository;

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

     // --- 가입/로그인 상태 계산 (실데이터 기반) : 교체 시작 ---
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        boolean loginRequired = (loginUser == null);

        // 통화코드 정규화: "JPY(100)" -> "JPY"
        // ✅ final로 한 번만 할당 (람다에서 사용 가능)
        final String alpha = (
                currencyCode.contains("(")
                    ? currencyCode.substring(0, currencyCode.indexOf('('))
                    : currencyCode
            ).trim().toUpperCase();

        boolean hasParentAccount = false;
        boolean hasChildAccount  = false;
        boolean needSubscription = false;

        if (!loginRequired) {
            final String uid = loginUser.getUid();

            // 1) 부모계좌(외화통장) 보유 여부
            hasParentAccount = parentAccountRepository.existsByUser_Uid(uid);

            // 2) 자식계좌(해당 통화) 보유 여부 (✅ CUNAME 기준)
            hasChildAccount = childAccountRepository
                    .findByParentAccount_User_Uid(uid)  // 유저의 모든 자식계좌
                    .stream()
                    .anyMatch(ca ->
                            ca.getCurrency() != null
                         && alpha.equalsIgnoreCase(ca.getCurrency().getCuname())
                    );

            // 환전 진입 조건: Parent + 해당 통화 Child 보유 시 통과
            needSubscription = !(hasParentAccount && hasChildAccount);
        }

        // 뷰로 내려줌
        model.addAttribute("loginRequired", loginRequired);
        model.addAttribute("needSubscription", needSubscription);
        model.addAttribute("hasParentAccount", hasParentAccount);
        model.addAttribute("hasChildAccount", hasChildAccount);
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
