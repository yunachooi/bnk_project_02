package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.entity.CustomerRate;
import com.example.bnk_project_02s.service.EximRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fx")
public class FxLiveApiController {

    private final EximRateService eximRateService;

    @GetMapping("/ticker")
    public Map<String, Object> ticker(
            @RequestParam(defaultValue = "USD,JPY(100),EUR,CNH,GBP,CHF") String codes) {

        Map<String, Object> out = new LinkedHashMap<>();
        for (String raw : codes.split(",")) {
            String code = raw.trim();
            if (code.isEmpty()) continue;
            CustomerRate cr = eximRateService.getTodayRateByCode(code);
            if (cr != null) {
                out.put(code, Map.of(
                        "date", cr.getCdate().toString(),
                        "code", cr.getCcode(),
                        "name", cr.getCname(),
                        "dealBasR", cr.getCvalue(),
                        "tts", cr.getCtts(),
                        "pref", cr.getCpref(),
                        "final", cr.getCfinal(),
                        "fee", cr.getCfee()
                ));
            }
        }
        return out;
    }
}