package com.example.bnk_project_02s.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.bnk_project_02s.dto.ForexRateDto;
import com.example.bnk_project_02s.entity.Rate;
import com.example.bnk_project_02s.repository.ForexFirstRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateService {

    private final ForexFirstRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${exim.api-key}")
    private String apiKey;

    public RateService(ForexFirstRepository repository) {
        this.repository = repository;
    }

    public List<Rate> getTodayRates() {
        return repository.findByRdate(LocalDate.now());
    }

    public void fetchTodayRates() {
        try {
            String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String apiUrl = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON?authkey="
                    + apiKey + "&searchdate=" + dateStr + "&data=AP01";

            System.out.println("🔄 [API 호출] URL: " + apiUrl);

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                responseBuilder.append(line);
            }
            br.close();

            String json = responseBuilder.toString();
            System.out.println("📦 [API 응답 데이터] 일부 출력: " + json.substring(0, Math.min(100, json.length())) + "...");

            List<ForexRateDto> allRates = objectMapper.readValue(json, new TypeReference<>() {});
            Set<String> choice = Set.of("USD", "JPY(100)", "EUR", "CNH", "GBP", "CHF");

            LocalDate today = LocalDate.now();

            List<Rate> filtered = allRates.stream()
                    .filter(dto -> choice.contains(dto.getRcode()))
                    .map(dto -> Rate.builder()
                            .rdate(today)
                            .rcurrencyno(dto.getRcode())
                            .rtoday(new BigDecimal(dto.getRvalue().replace(",", "")))
                            .build())
                    .collect(Collectors.toList());

            repository.saveAll(filtered);
            System.out.println("✅ [DB 저장 완료] 저장된 환율 수: " + filtered.size());

        } catch (Exception e) {
            System.out.println("❌ [에러 발생] " + e.getMessage());
        }
    }
}
