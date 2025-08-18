package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AccountController {

	@GetMapping("/foreign0") public String foreign0() { return "account/foreign0"; }
    @GetMapping("/foreign1") public String foreign1() { return "account/foreign1"; }
    @GetMapping("/foreign2") public String foreign2() { return "account/foreign2"; }
    @GetMapping("/foreign3") public String foreign3() { return "account/foreign3"; }
    @GetMapping("/foreign4") public String foreign4() { return "account/foreign4"; }
    @GetMapping("/foreign5") public String foreign5() { return "account/foreign5"; }
    @GetMapping("/foreign6") public String foreign6() { return "account/foreign6"; }
    @GetMapping("/foreign7") public String foreign7() { return "account/foreign7"; }
    @GetMapping("/foreign8") public String foreign8() { return "account/foreign8"; }
}