package com.example.bnk_project_02s.service;

import com.example.bnk_project_02s.entity.Rate;
import com.example.bnk_project_02s.repository.ForexFirstRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InitRateService {

    private final ForexFirstRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ecos.api-key}")
    private String apiKey;

    // 📌 ECOS 통화 코드 → 우리 시스템 통화코드
    private static final Map<String, String> CODE_TO_RCODE = Map.of(
        "0000001", "USD",
        "0000002", "JPY(100)",
        "0000003", "EUR",
        "0000053", "CNH",
        "0000012", "GBP",
        "0000014", "CHF"
    );

    public void fetchInitialRates() {
        LocalDate start = LocalDate.of(2025, 5, 8);
        LocalDate end = LocalDate.of(2025, 8, 8);
        DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;

        CODE_TO_RCODE.forEach((itemCode, rcode) -> {
            try {
                String apiUrl = String.format(
                    "https://ecos.bok.or.kr/api/StatisticSearch/%s/json/kr/1/1000/731Y001/D/%s/%s/%s",
                    apiKey,
                    start.format(formatter),
                    end.format(formatter),
                    itemCode
                );

                System.out.println("📡 API 호출: " + apiUrl);

                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                JsonNode root = objectMapper.readTree(sb.toString());
                JsonNode rows = root.at("/StatisticSearch/row");

                if (!rows.isArray() || rows.size() == 0) {
                    System.out.println("⚠️ " + rcode + " 데이터 없음");
                    return;
                }

                List<Rate> rates = new ArrayList<>();
                for (JsonNode node : rows) {
                    String time = node.get("TIME").asText();
                    String value = node.get("DATA_VALUE").asText();
                    String currencyName = node.get("ITEM_NAME1").asText();

                    LocalDate rdate = LocalDate.parse(time, formatter);
                    BigDecimal rvalue = new BigDecimal(value.replace(",", ""));

                    // ✅ 중복 방지
                    if (repository.existsByRdateAndRcode(rdate, rcode)) {
                        continue;
                    }

                    Rate rate = Rate.builder()
                            .rdate(rdate)
                            .rcode(rcode)
                            .rcurrency(currencyName)
                            .rvalue(rvalue)
                            .build();

                    rates.add(rate);
                }

                repository.saveAll(rates);
                System.out.println("✅ 저장 완료: " + rcode + " → " + rates.size() + "건");

            } catch (Exception e) {
                System.out.println("❌ 오류: " + rcode + " → " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
