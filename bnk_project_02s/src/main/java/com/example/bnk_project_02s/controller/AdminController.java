package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/fx")            // 외환 최상위 → 대시보드 리다이렉트
    public String fxRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
    	model.addAttribute("content", "admin/dashboard");
        return "layout/adminLayout"; 
    }

    @GetMapping("/customer-info")
    public String customerInfo(Model model) {
    	model.addAttribute("content", "admin/customerInfo");
        return "layout/adminLayout"; 
    }
}
