package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.entity.Rate;
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
        List<Rate> todayRates = rateService.getTodayRates();
        model.addAttribute("rates", todayRates);
        return "forexView"; // 👉 templates/forexView.html
    }

    @GetMapping("/forex/fetch")
    public String fetchRates() {
        System.out.println("📥 [환율 데이터 수집] /forex/fetch 호출됨");
        rateService.fetchTodayRates();
        return "redirect:/forex";
    }
}
