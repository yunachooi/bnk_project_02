package com.example.bnk_project_02s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

	@GetMapping("/foreign0") // 메인
	public String foregin0() {
		return "account/foreign0";
	}
	
	@GetMapping("/foreign1") // 상품 안내
	public String foreign1() {
	    return "account/foreign1";
	}
	
	@GetMapping("/foreign2") // 가입 1/3단계
	public String foregin2() {
		return "account/foreign2";
	}
	
	@GetMapping("/foreign3") // 가입 1/3단계 - 사진
	public String foregin3() {
		return "account/foreign3";
	}
	
	@GetMapping("/foreign4") // 가입 1/3단계 - 본인 정보 확인
	public String foregin4() {
		return "account/foreign4";
	}
	
	@GetMapping("/foreign5") // 가입 2/3단계
	public String foregin5() {
		return "account/foreign5";
	}
	
	@GetMapping("/foreign6") // 가입 3단계
	public String foregin6() {
		return "account/foreign6";
	}
	
	@GetMapping("/foreign7") // 카드 가입 1단계
	public String foregin7() {
		return "account/foreign7";
	}
	
	@GetMapping("/foreign8") // 카드 가입 2단계
	public String foregin8() {
		return "account/foreign8";
	}
	
}
