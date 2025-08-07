package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String home(HttpSession session) {
        User loginUser = (User) session.getAttribute("LOGIN_USER");

        if (loginUser == null) {
            return "redirect:/user/login";
        }

        // 권한에 따라 페이지 분기
        String role = loginUser.getUrole();
        return switch (role) {
            case "ROLE_ADMIN" -> "admin/adminMain";
            case "ROLE_USER" -> "user/userMain";
            default -> "redirect:/user/login"; // 잘못된 권한인 경우 로그인 화면으로
        };
    }
}