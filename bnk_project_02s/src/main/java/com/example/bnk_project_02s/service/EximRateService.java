package com.example.bnk_project_02s.service;

import com.example.bnk_project_02s.dto.EximRateDto;
import com.example.bnk_project_02s.entity.CustomerRate;
import com.example.bnk_project_02s.repository.CustomerRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EximRateService {

    private final CustomerRateRepository customerRateRepository;

    @Value("${exim.api-key}")
    private String apiKey;

    private static final String API_URL =
            "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";

    private static final BigDecimal PREF_RATE = new BigDecimal("50"); // 고정 우대율 50%
    private static final DateTimeFormatter yyyymmdd = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate = new RestTemplate();

    /** ✅ 오늘 전체 통화 저장 */
    public void fetchAndSaveRatesForDate(LocalDate date) {
        String url = API_URL + "?authkey=" + apiKey
                + "&searchdate=" + yyyymmdd.format(date)
                + "&data=AP01";
        try {
            EximRateDto[] response = restTemplate.getForObject(url, EximRateDto[].class);
            if (response == null || response.length == 0) {
                log.warn("📌 EXIM API 응답 없음 (date={})", date);
                return;
            }
            Arrays.stream(response)
                    .filter(this::hasValidNumbers)
                    .forEach(dto -> {
                        CustomerRate saved = saveCustomerRateIfAbsent(date, dto); // ← date 사용!
                        if (saved != null) {
                            log.info("💾 저장 완료: {} / {}", saved.getCdate(), saved.getCcode());
                        }
                    });
        } catch (Exception e) {
            log.error("❌ EXIM 저장 중 오류 (date={}): {}", date, e.getMessage(), e);
        }
    }

    /** ✅ 오늘 특정 통화만 저장 (이미 있으면 재사용) */
    public CustomerRate fetchAndSaveSingleRate(String code) {
        LocalDate today = LocalDate.now();
        String normalized = normalizeCode(code);

        // 이미 있으면 반환
        Optional<CustomerRate> existing = customerRateRepository.findByCcodeAndCdate(normalized, today);
        if (existing.isPresent()) {
            log.info("📌 오늘 {} 데이터 이미 존재 → 재사용", normalized);
            return existing.get();
        }

        String url = API_URL + "?authkey=" + apiKey + "&searchdate=" + yyyymmdd.format(today) + "&data=AP01";

        try {
            EximRateDto[] response = restTemplate.getForObject(url, EximRateDto[].class);
            if (response == null || response.length == 0) {
                log.warn("📌 EXIM API 응답 없음");
                return null;
            }

            return Arrays.stream(response)
                    .filter(this::hasValidNumbers)
                    .filter(dto -> normalizeCode(dto.getCode()).equals(normalized))
                    .findFirst()
                    .map(dto -> saveCustomerRateIfAbsent(today, dto))
                    .orElseGet(() -> {
                        log.warn("📌 코드 {} 에 해당하는 환율을 API에서 찾지 못함", normalized);
                        return null;
                    });

        } catch (Exception e) {
            log.error("❌ EXIM 단일 저장 중 오류 ({}): {}", normalized, e.getMessage(), e);
            return null;
        }
    }

    /** ✅ 오늘 특정 통화 조회(컨트롤러에서 재사용 가능) */
    public CustomerRate getTodayRateByCode(String code) {
        return customerRateRepository
                .findByCcodeAndCdate(normalizeCode(code), LocalDate.now())
                .orElse(null);
    }

    /** ✅ DTO → 엔티티 변환 + (없을 때만) 저장 */
    private CustomerRate saveCustomerRateIfAbsent(LocalDate date, EximRateDto dto) {
        String code = normalizeCode(dto.getCode());

        // 중복 방지
        if (customerRateRepository.findByCcodeAndCdate(code, date).isPresent()) {
            return customerRateRepository.findByCcodeAndCdate(code, date).get();
        }

        BigDecimal dealBasR = parseBigDecimal(dto.getDealBasR());
        BigDecimal tts      = parseBigDecimal(dto.getTts());

        // 수수료 = (TTS - 기준매매율)
        BigDecimal fee = tts.subtract(dealBasR);

        // 우대 금액 = 수수료 × (우대율 / 100)
        BigDecimal discount = fee.multiply(PREF_RATE).divide(new BigDecimal("100"));

        // 최종 고객 적용 환율 = TTS - 우대금액
        BigDecimal finalRate = tts.subtract(discount);

        CustomerRate entity = CustomerRate.builder()
                .cdate(date)
                .ccode(code)
                .cname(safe(dto.getName()))
                .cvalue(dealBasR)
                .ctts(tts)
                .cpref(PREF_RATE)
                .cfee(fee)
                .cfinal(finalRate)
                .build();

        return customerRateRepository.save(entity);
    }

    /** 유효 숫자 필터(KRW/0 데이터 스킵) */
    private boolean hasValidNumbers(EximRateDto dto) {
        try {
            BigDecimal base = parseBigDecimal(dto.getDealBasR());
            BigDecimal tts  = parseBigDecimal(dto.getTts());
            // KRW는 tts=0, deal_bas_r=1 이라 보통 스킵
            return base.compareTo(BigDecimal.ZERO) > 0 && tts.compareTo(BigDecimal.ZERO) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 콤마 제거 + BigDecimal 변환 */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null) return BigDecimal.ZERO;
        String s = value.replace(",", "").trim();
        if (s.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(s);
    }

    /** 코드/문자열 정규화 */
    private String normalizeCode(String code) {
        return code == null ? "" : code.trim();
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    /** 목록 조회(관리 페이지 등에서 사용) */
    public List<CustomerRate> getAllRates() {
        return customerRateRepository.findAll();
    }
}
