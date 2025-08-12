package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class BankController {

    /** /branches -> bankSearch.html 렌더, 기본 탭은 전체(all) */
    @GetMapping("/branches")
    public String bankSearch(@RequestParam(value = "tab", required = false) String tab, Model model) {
        String initial = "digital".equalsIgnoreCase(tab) ? "digital" : "all";
        model.addAttribute("initialTab", initial);
        return "bankSearch"; // src/main/resources/templates/bankSearch.html
    }
}