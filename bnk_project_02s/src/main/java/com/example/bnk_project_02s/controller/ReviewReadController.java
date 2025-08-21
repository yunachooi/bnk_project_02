package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.service.ReviewStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewReadController {

    private final ReviewStatsService reviewStatsService;

    @GetMapping("/recent2")
    public Map<String, Object> recent(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        int n = clamp(limit);
        var items = reviewStatsService.recent(n);
        return Map.of("items", items);
    }

    private int clamp(int v) {
        if (v < 1)  return 1;
        if (v > 50) return 50;
        return v;
    }
}