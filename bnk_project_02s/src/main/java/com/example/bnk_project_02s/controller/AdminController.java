package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
	@GetMapping("/dashboard")
	public String dashboard(Model model) {
	    model.addAttribute("content", "admin/dashboard :: content"); // ❌ templates. 빼야 함
	    return "layout/admin-layout"; // ✅ layout/admin-layout.html 파일이 templates 폴더 아래에 있어야 함
	}
	
}
