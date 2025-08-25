package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.bnk_project_02s.service.SubscriberStatsService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
	
	 private final SubscriberStatsService stats; // 추가

    /** 외환 최상위 → 대시보드 리다이렉트 (선택) */
    @GetMapping("/fx")
    public String fxRoot() {
        return "redirect:/admin/dashboard";
    }

    /** 관리자 대시보드 */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 서버 렌더링으로도 즉시 표시하고 싶을 때 주입(옵션)
        model.addAttribute("dailySeries",     stats.daily(7));
        model.addAttribute("monthlySeries",   stats.monthly(6));
        model.addAttribute("quarterlySeries", stats.quarterly(6));

        model.addAttribute("content", "admin/dashboard");
        return "layout/adminLayout";
    }
    
    /** 고객정보 화면 */
    @GetMapping("/customer-info")
    public String customerInfo(Model model) {
        model.addAttribute("content", "admin/customerInfo");
        return "layout/adminLayout";
    }

    /** (선택) 과거 /admin/home 북마크 호환 */
    @GetMapping("/home")
    public String homeCompat() {
        return "redirect:/admin/dashboard";
    }
}