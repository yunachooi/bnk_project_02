package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

	@GetMapping("/foreign1")
	public String foreignDetail() {
	    return "account/foreign1";
	}
}
