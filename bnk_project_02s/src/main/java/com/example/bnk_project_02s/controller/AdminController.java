package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    /** 외환 최상위 → 대시보드 리다이렉트 (선택) */
    @GetMapping("/fx")
    public String fxRoot() {
        return "redirect:/admin/dashboard";
    }

    /** 관리자 대시보드 */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
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