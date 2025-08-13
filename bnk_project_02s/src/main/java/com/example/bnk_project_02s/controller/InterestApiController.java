package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.service.InterestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class InterestApiController {
	
	private final InterestService interestService;
	
    /** 관심 통화 분포 */
    @GetMapping("/currencies")
    public Map<String, Integer> currencies() {
        return interestService.countByCurrency();
    }

    /** 관심 분야 분포 */
    @GetMapping("/topics")
    public Map<String, Integer> topics() {
        return interestService.countByTopic();
    }

}
