package com.example.bnk_project_02s.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.example.bnk_project_02s.dto.ForexRateDiffDto;
import com.example.bnk_project_02s.dto.ForexRateDto;
import com.example.bnk_project_02s.entity.Rate;
import com.example.bnk_project_02s.repository.ForexFirstRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RateService {

    @Autowired
    private ForexFirstRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ecos.api-key}")
    private String apiKey;

    private static final Map<String, String> CODE_TO_RCODE = Map.of(
        "0000001", "USD",
        "0000002", "JPY(100)",
        "0000003", "EUR",
        "0000053", "CNH",
        "0000012", "GBP",
        "0000014", "CHF"
    );

    private static final List<String> RCODE_ORDER = List.of("USD", "JPY(100)", "EUR", "CNH", "GBP", "CHF");

    public void fetchTodayRates() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            for (String itemCode : CODE_TO_RCODE.keySet()) {
                String apiUrl = String.format(
                    "https://ecos.bok.or.kr/api/StatisticSearch/%s/json/kr/1/1000/731Y001/D/%s/%s/%s",
                    apiKey, today, today, itemCode);

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
                JsonNode root = objectMapper.readTree(json);
                JsonNode rows = root.at("/StatisticSearch/row");

                if (!rows.isArray() || rows.size() == 0) {
                    System.out.println("⚠️ " + CODE_TO_RCODE.get(itemCode) + " 데이터 없음, 스킵");
                    continue;
                }

                List<ForexRateDto> dtos = objectMapper.convertValue(rows, new TypeReference<>() {});
                List<Rate> rates = dtos.stream()
                        .map(dto -> Rate.builder()
                                .rdate(LocalDate.parse(dto.getRdate(), DateTimeFormatter.BASIC_ISO_DATE))
                                .rcode(CODE_TO_RCODE.get(dto.getRcode()))
                                .rcurrency(dto.getRcurrency())
                                .rvalue(new BigDecimal(dto.getRvalue().replace(",", "")))
                                .build())
                        .collect(Collectors.toList());

                repository.saveAll(rates);
                System.out.println("✅ [DB 저장 완료] " + CODE_TO_RCODE.get(itemCode) + " 저장된 건수: " + rates.size());
            }

        } catch (Exception e) {
            System.out.println("❌ [에러 발생] " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<ForexRateDiffDto> getTodayRateViewDtos() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<Rate> todayRates = repository.findByRdate(today);

        return todayRates.stream()
                .map(rate -> {
                    Rate yesterdayRate = repository.findByRdateAndRcode(yesterday, rate.getRcode());
                    BigDecimal yesterdayValue = (yesterdayRate != null) ? yesterdayRate.getRvalue() : rate.getRvalue();

                    int diffFlag = rate.getRvalue().compareTo(yesterdayValue);
                    if (diffFlag > 0) diffFlag = 1;
                    else if (diffFlag < 0) diffFlag = -1;

                    return new ForexRateDiffDto(
                            rate.getRcode(),
                            rate.getRcurrency(),
                            rate.getRvalue(),
                            yesterdayValue,
                            diffFlag
                    );
                })
                .sorted(Comparator.comparingInt(r -> RCODE_ORDER.indexOf(r.getRcode())))
                .collect(Collectors.toList());
    }
}
