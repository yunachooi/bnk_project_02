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
	public String generateReport(@RequestBody String statsJson) {
		return reportService.generateDashBoardReport(statsJson);
	}
}
