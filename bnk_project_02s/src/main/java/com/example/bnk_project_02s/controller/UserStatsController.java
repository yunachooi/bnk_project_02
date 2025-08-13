package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.service.UserStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;
    //유저 남녀 성별 비율 계산
    @GetMapping("/genderStats")
    public Map<String, Long> getGenderStats() {
        return userStatsService.genderStats();
    }
}
