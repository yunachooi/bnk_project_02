package com.example.bnk_project_02s.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.dto.StatsSeries;
import com.example.bnk_project_02s.service.SubscriberStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subscribers")
@RequiredArgsConstructor
public class SubscriberStatsRestController {

    private final SubscriberStatsService stats;

    @GetMapping("/daily")
    public StatsSeries daily(@RequestParam(name = "days", defaultValue = "7") int days) {
        return stats.daily(days);
    }

    @GetMapping("/monthly")
    public StatsSeries monthly(@RequestParam(name = "months", defaultValue = "6") int months) {
        return stats.monthly(months);
    }

    @GetMapping("/quarterly")
    public StatsSeries quarterly(@RequestParam(name = "quarters", defaultValue = "6") int quarters) {
        return stats.quarterly(quarters);
    }
}