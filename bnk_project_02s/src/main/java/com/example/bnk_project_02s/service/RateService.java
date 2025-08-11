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
import org.springframework.transaction.annotation.Transactional;

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

    /* ==============================================================
     *  ✅ 재시도 래퍼
     *     - ECOS가 아직 미게시일 때 지수 백오프로 재시도
     *     - 최종적으로 사용된 '영업일(LocalDate)' 반환
     * ============================================================== */
    public LocalDate collectTodayWithRetry() {
        final int maxAttempts = 6;             // 최대 6회
        long delayMs = 5 * 60 * 1000L;         // 5분부터 시작 (최대 30분)
        LocalDate bizDate = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.println("🔁 [수집 재시도] attempt " + attempt + "/" + maxAttempts);
                bizDate = fetchTodayRatesDedup();  // ⬅️ 실제 응답/저장 rdate 반환
                System.out.println("✅ [수집 성공] bizDate=" + bizDate);
                return bizDate;
            } catch (NoDataYetException nde) {
                System.out.println("⚠️ [아직 미게시] " + nde.getMessage() + " 재시도 예정");
            } catch (Exception e) {
                System.out.println("❌ [수집 오류] " + e.getMessage());
                e.printStackTrace();
            }

            if (attempt == maxAttempts) break;

            try {
                System.out.println("⏳ " + delayMs + "ms 후 재시도(지수 백오프)");
                Thread.sleep(delayMs);
                delayMs = Math.min((long)(delayMs * 1.6), 30 * 60 * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("🚫 [수집 최종 실패] 오늘 데이터 수집 완료 못함");
        // 폴백: 그래도 최신 영업일 반환(없으면 null)
        return resolveLatestDate(LocalDate.now(), 7);
    }

    /* ==============================================================
     *  ✅ 중복 저장 방지 수집 메서드
     *     - (rdate, rcode) 존재 시 skip
     *     - 전체 통화가 0건이면 아직 공시 전 → NoDataYetException
     *     - 실제로 응답/저장에 사용된 rdate(영업일) 반환
     * ============================================================== */
    @Transactional
    public LocalDate fetchTodayRatesDedup() throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate bizDate = null;  // ← 실제 응답에서 확인된 rdate(영업일)
        int totalFetched = 0;      // API에서 받은 총 건수
        int totalNew = 0;          // 신규 insert 건수

        for (String itemCode : CODE_TO_RCODE.keySet()) {
            String apiUrl = String.format(
                "https://ecos.bok.or.kr/api/StatisticSearch/%s/json/kr/1/1000/731Y001/D/%s/%s/%s",
                apiKey, today, today, itemCode);

            System.out.println("🔄 [API 호출] URL: " + apiUrl);

            String json = httpGet(apiUrl);
            JsonNode root = objectMapper.readTree(json);
            JsonNode rows = root.at("/StatisticSearch/row");

            if (!rows.isArray() || rows.size() == 0) {
                System.out.println("⚠️ " + CODE_TO_RCODE.get(itemCode) + " 데이터 없음, 스킵");
                continue;
            }

            List<ForexRateDto> dtos = objectMapper.convertValue(rows, new TypeReference<List<ForexRateDto>>() {});
            totalFetched += dtos.size();

            List<Rate> toInsert = new ArrayList<>();
            for (ForexRateDto dto : dtos) {
                LocalDate rdate = LocalDate.parse(dto.getRdate(), DateTimeFormatter.BASIC_ISO_DATE);
                if (bizDate == null) bizDate = rdate;  // ← 처음 본 rdate를 영업일로 채택

                String rcode = CODE_TO_RCODE.get(dto.getRcode());
                String rcurrency = dto.getRcurrency();
                BigDecimal rvalue = new BigDecimal(dto.getRvalue().replace(",", ""));

                if (repository.existsByRdateAndRcode(rdate, rcode)) {
                    // 이미 있으면 스킵 (업데이트 정책이 필요하면 여기서 비교/갱신)
                    continue;
                }

                toInsert.add(
                    Rate.builder()
                        .rdate(rdate)
                        .rcode(rcode)
                        .rcurrency(rcurrency)
                        .rvalue(rvalue)
                        .build()
                );
            }

            if (!toInsert.isEmpty()) {
                repository.saveAll(toInsert);
                totalNew += toInsert.size();
                System.out.println("💾 [DB 저장] " + CODE_TO_RCODE.get(itemCode) + " 신규 " + toInsert.size() + "건");
            }
        }

        // 모든 통화가 0건이면 아직 공시 전으로 판단 → 재시도 유도
        if (totalFetched == 0 && totalNew == 0) {
            throw new NoDataYetException("ECOS 금일 데이터가 아직 게시되지 않았을 수 있습니다.");
        }

        // 혹시 bizDate를 못 잡았다면(드묾) 최신 영업일로 폴백
        if (bizDate == null) {
            bizDate = resolveLatestDate(LocalDate.now(), 7);
        }
        return bizDate;
    }

    /* ---------------------- 기존: 수동 수집(원본 유지) ---------------------- */
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

    /* ---------------------- 간단 GET 유틸 ---------------------- */
    private String httpGet(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);
        conn.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            return sb.toString();
        }
    }

    /* ---------------------- “아직 미게시” 구분용 예외 ---------------------- */
    static class NoDataYetException extends RuntimeException {
        NoDataYetException(String msg) { super(msg); }
    }

    /* ---------------------- 영업일 폴백 헬퍼 (기존 유지) ---------------------- */
    private LocalDate resolveLatestDate(LocalDate base, int lookbackDays) {
        LocalDate d = base;
        for (int i = 0; i < lookbackDays; i++) {
            if (!repository.findByRdate(d).isEmpty()) return d;
            d = d.minusDays(1);
        }
        return null;
    }

    private LocalDate previousDate(LocalDate latest, int lookbackDays) {
        if (latest == null) return null;
        LocalDate d = latest.minusDays(1);
        for (int i = 0; i < lookbackDays; i++) {
            if (!repository.findByRdate(d).isEmpty()) return d;
            d = d.minusDays(1);
        }
        return null;
    }

    /* ---------------------- View 리스트용 (기존 유지) ---------------------- */
    public List<ForexRateDiffDto> getTodayRateViewDtos() {
        LocalDate latest = resolveLatestDate(LocalDate.now(), 60);
        if (latest == null) return List.of();

        LocalDate prev = previousDate(latest, 60);
        List<Rate> latestRates = repository.findByRdate(latest);

        return latestRates.stream()
                .map(rate -> {
                    BigDecimal prevValue = rate.getRvalue();
                    if (prev != null) {
                        Rate y = repository.findByRdateAndRcode(prev, rate.getRcode());
                        if (y != null) prevValue = y.getRvalue();
                    }

                    int diffFlag = rate.getRvalue().compareTo(prevValue);
                    diffFlag = diffFlag > 0 ? 1 : (diffFlag < 0 ? -1 : 0);

                    return new ForexRateDiffDto(
                            rate.getRcode(),
                            rate.getRcurrency(),
                            rate.getRvalue(),
                            prevValue,
                            diffFlag
                    );
                })
                .sorted(Comparator.comparingInt(r -> RCODE_ORDER.indexOf(r.getRcode())))
                .collect(Collectors.toList());
    }

    /* ---------------------- 상세/차트용 (기존 유지) ---------------------- */
    public Rate getTodayRate(String currencyCode) {
        LocalDate latest = resolveLatestDate(LocalDate.now(), 60);
        return (latest == null) ? null : repository.findByRdateAndRcode(latest, currencyCode);
    }

    public Rate getYesterdayRate(String currencyCode) {
        LocalDate latest = resolveLatestDate(LocalDate.now(), 60);
        LocalDate prev = previousDate(latest, 60);
        return (prev == null) ? null : repository.findByRdateAndRcode(prev, currencyCode);
    }

    public List<Rate> getPastWeekRates(String currencyCode) {
        LocalDate latest = resolveLatestDate(LocalDate.now(), 60);
        if (latest == null) return List.of();
        LocalDate fromDate = latest.minusDays(6); // latest 포함 7일
        return repository.findByRcodeAndRdateBetweenOrderByRdateAsc(currencyCode, fromDate, latest);
    }

    public List<Rate> getPastMonthRates(String currencyCode) {
        LocalDate latest = resolveLatestDate(LocalDate.now(), 90);
        if (latest == null) return List.of();
        LocalDate fromDate = latest.minusDays(29); // latest 포함 30일
        return repository.findByRcodeAndRdateBetweenOrderByRdateAsc(currencyCode, fromDate, latest);
    }
}
