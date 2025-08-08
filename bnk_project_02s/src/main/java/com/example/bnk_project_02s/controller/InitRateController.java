package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.service.InitRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/init")
public class InitRateController {

    private final InitRateService initRateService;

    @PostMapping("/rates")
    public String initializeRates() {
        initRateService.fetchInitialRates();  // 5월 8일부터 8월 8일까지 수집
        return "✅ 초기 환율 데이터 저장 완료 (USD~CHF)";
    }
}

