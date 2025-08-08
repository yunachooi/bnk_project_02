package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.dto.ForexRateDiffDto;
import com.example.bnk_project_02s.service.RateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class RateController {

    @Autowired
    private RateService rateService;

    @GetMapping("/forex")
    public String showRates(Model model) {
        System.out.println("📄 [환율 조회] /forex 진입");

        // ✅ 오늘 + 전일 비교된 DTO 리스트 조회
        List<ForexRateDiffDto> todayRates = rateService.getTodayRateViewDtos();

        model.addAttribute("rates", todayRates);
        return "forexView"; // 👉 templates/forexView.html
    }

}
