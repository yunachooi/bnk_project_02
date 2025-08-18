package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.entity.CustomerRate;
import com.example.bnk_project_02s.service.EximRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/forex")
public class RateRestController {

    private final EximRateService eximRateService;

    @GetMapping("/today")
    public CustomerRate today(@RequestParam("code") String code){
        return eximRateService.getTodayRateByCode(code);
    }
}