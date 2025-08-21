// ExchangeHistoryService.java
package com.example.bnk_project_02s.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.entity.ChildAccount;
import com.example.bnk_project_02s.entity.Currency;
import com.example.bnk_project_02s.entity.History;
import com.example.bnk_project_02s.entity.ParentAccount;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.CurrencyRepository;
import com.example.bnk_project_02s.repository.HistoryRepository;
import com.example.bnk_project_02s.repository.ParentAccountRepository;
import com.example.bnk_project_02s.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExchangeHistoryService {

    private final HistoryRepository historyRepo;
    private final ParentAccountRepository parentRepo;
    private final ChildAccountRepository childRepo;   // (현재 미사용: 자식계좌 잔액 갱신 시 활용)
    private final CurrencyRepository currencyRepo;
    private final UserRepository userRepo;
    private final CashKrwService cashKrwService;     // ✅ 누적 KRW 반영

    /** 환전 완료 → History 저장 + 누적 KRW 업데이트 */
    @Transactional
    public History savePurchaseHistory(String pano,
                                       String rateCode,      // 화면에서 올 수 있는 값: "USD", "JPY(100)", "USD 현찰" 등
                                       String fxAmountStr,   // "600.00"
                                       String krwAmountStr,  // "835,335 원"
                                       String uid) {

        // --- 레퍼런스 로드
        ParentAccount parent = parentRepo.findByPano(nullToEmpty(pano))
                .orElseThrow(() -> new IllegalArgumentException("부모계좌가 존재하지 않습니다: " + pano));

        String normCode = normalizeCode(rateCode);   // "JPY(100)" → "JPY"
        Currency currency = currencyRepo.findByCunameIgnoreCase(normCode)
                .orElseThrow(() -> new IllegalArgumentException("통화코드가 존재하지 않습니다: " + normCode));

        User user = userRepo.findByUid(nullToEmpty(uid))
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다: " + uid));

        // --- 금액 파싱
        BigDecimal fxAmt  = parseDecimalStrict(fxAmountStr);  // 외화(소수 허용)
        BigDecimal krwAmt = parseKrwToInt(krwAmountStr);      // 원화(정수, 소수 버림)

        // --- 직전 잔액(같은 통화 기준) 조회
        ChildAccount child = childRepo
        	    .findByParentAccount_PanoAndCurrency_Cuname(parent.getPano(), currency.getCuname())
        	    .orElseThrow(() -> new IllegalStateException("해당 통화 자식계좌가 없습니다: " + currency.getCuname()));

        	java.math.BigDecimal oldChildBal =
        	        (child.getCabalance() == null) ? java.math.BigDecimal.ZERO : child.getCabalance();

        	java.math.BigDecimal newChildBal = oldChildBal
        	        .add(fxAmt) // 구매 → 외화 잔액 증가
        	        .setScale(2, java.math.RoundingMode.HALF_UP);

        	child.setCabalance(newChildBal);
        	childRepo.save(child);

        	// --- History 엔티티 저장 (거래 후 잔액 = 자식계좌 새 잔액으로 기록)
        	History hist = History.builder()
        	        .parentAccount(parent)
        	        .currency(currency)
        	        .user(user)
        	        .hwithdraw(null)
        	        .hdeposit(formatPlain2(fxAmt))           // "600.00"
        	        .hbalance(formatPlain2(newChildBal))     // ✅ 자식계좌 새 잔액으로 기록
        	        .hkrw(krwAmt)
        	        .hdate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        	        .build();
        History saved = historyRepo.save(hist);

        // --- 누적 KRW 테이블 업데이트 (유저별 1행 upsert)
        cashKrwService.accumulate(uid, krwAmt);

        return saved;
    }

    // ---------- helpers ----------

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** 소수 허용 파서: 숫자/점만 남김. 빈 값이면 0. */
    private static BigDecimal parseDecimalStrict(String s) {
        if (s == null) return BigDecimal.ZERO;
        String cleaned = s.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty() || ".".equals(cleaned)) return BigDecimal.ZERO;
        return new BigDecimal(cleaned);
    }

    /** KRW는 정수로 저장(소수점 이하 버림, 음수 방지) */
    private static BigDecimal parseKrwToInt(String s) {
        BigDecimal v = parseDecimalStrict(s);
        if (v.signum() < 0) v = v.abs();
        return v.setScale(0, RoundingMode.DOWN);
    }

    /** 외화 문자열화: 소수 2자리 고정, 콤마 없음 */
    private static String formatPlain2(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** "JPY(100)" → "JPY", "USD 현찰" → "USD" 등 코드 정규화 */
    private static String normalizeCode(String s) {
        if (s == null) return "";
        String t = s.split("\\(")[0];        // 괄호 이후 제거
        t = t.replaceAll("[^A-Za-z]", "");   // 영문자만 남김
        return t.trim();
    }
}
