package com.example.bnk_project_02s.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.service.OpenAiReportService;

@RestController
@RequestMapping("/api")
public class OpenAiReportController {

	@Autowired
	private OpenAiReportService reportService;
	
	public OpenAiReportController(OpenAiReportService reportSerive) {
		this.reportService = reportSerive;
	}
	
	@PostMapping("/generateReport")
	//프런트가 보낸 대시보드 원본 데이터를 본문으로 받고, 문자열(Markdown 리포트)을 그대로 응답. 컨트롤러는 단순 포워딩만 수행
	public String generateReport(@RequestBody String statsJson) {
		return reportService.generateDashBoardReport(statsJson);
	}
}
