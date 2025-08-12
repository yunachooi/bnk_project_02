package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.entity.CustomerRate;
import com.example.bnk_project_02s.service.EximRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/exchange")
public class ExchangeController {

    private final EximRateService eximRateService;

    /** 1단계: 구매 실행 화면 */
    @GetMapping("/buy/{code}")
    public String buyCurrency(@PathVariable("code") String code, Model model) {
        CustomerRate rate = eximRateService.fetchAndSaveSingleRate(code);
        if (rate == null) {
            model.addAttribute("error", "환율 정보를 불러올 수 없습니다.");
            return "error";
        }
        model.addAttribute("currencyCode", code);
        model.addAttribute("rate", rate);
        model.addAttribute("today", rate.getCdate());
        model.addAttribute("prefRate", rate.getCpref());
        model.addAttribute("fee", rate.getCfee());
        model.addAttribute("finalRate", rate.getCfinal());
        model.addAttribute("rateDate", rate.getCdate());
        model.addAttribute("rateRound", 1); // 필요시 실제 회차로 교체
        return "forexExchange"; // 1단계
    }

    /** 2단계: 구매 확인 화면으로 이동 (DB 저장 없음) */
    @PostMapping("/confirm")
    public String confirm(
            @RequestParam("code") String code,
            @RequestParam("fxAmount") String fxAmount,     // 외화 금액(문자열 유지)
            @RequestParam("krwAmount") String krwAmount,   // 원화 금액(정수 문자열)
            @RequestParam("finalRate") String finalRate,   // 고객 적용 환율
            @RequestParam(value = "rateDate", required = false) String rateDate,
            @RequestParam(value = "rateRound", required = false) String rateRound,
            @RequestParam(value = "savedFee", required = false) String savedFee,
            @RequestParam(value = "account", required = false) String accountMasked,
            Model model
    ) {
        // 그대로 뷰로 전달 (서버 계산/저장은 안 함)
        model.addAttribute("rateCode", code);
        model.addAttribute("fxAmount", fxAmount);
        model.addAttribute("krwAmount", krwAmount);
        model.addAttribute("finalRate", finalRate);
        model.addAttribute("rateDate", rateDate);
        model.addAttribute("rateRound", rateRound);
        model.addAttribute("savedFee", savedFee);
        model.addAttribute("accountMasked", accountMasked);
        return "forexExchange2"; // 확인 페이지
    }
    
    /* 3단계: 구매 완료 화면 (임시 버전 - DB 저장 없음) */
    @PostMapping("/submit")
    public String doneTemp(
            @RequestParam("code") String code,
            @RequestParam("fxAmount") String fxAmount,
            @RequestParam("krwAmount") String krwAmount,
            @RequestParam("finalRate") String finalRate,
            @RequestParam(value = "rateDate", required = false) String rateDate,
            @RequestParam(value = "rateRound", required = false) String rateRound,
            @RequestParam(value = "account", required = false) String accountMasked,
            @RequestParam(value = "savedFee", required = false) String savedFee,
            Model model
    ) {
        // 임시: 저장 없이 모델만 전달하여 완료 페이지 렌더링
        model.addAttribute("rateCode", code);
        model.addAttribute("fxAmount", fxAmount);
        model.addAttribute("krwAmount", krwAmount);
        model.addAttribute("finalRate", finalRate);
        model.addAttribute("rateDate", rateDate);
        model.addAttribute("rateRound", rateRound);
        model.addAttribute("accountMasked", accountMasked);
        model.addAttribute("savedFee", savedFee);
        return "forexExchange3"; // 임시 완료 페이지
    }
    


}
