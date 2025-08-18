package com.example.bnk_project_02s.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.entity.Card;
import com.example.bnk_project_02s.entity.ChildAccount;
import com.example.bnk_project_02s.entity.History;
import com.example.bnk_project_02s.entity.ShoppingLog;
import com.example.bnk_project_02s.entity.ShoppingProducts;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.CardRepository;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.HistoryRepository;
import com.example.bnk_project_02s.repository.ShoppingLogRepository;
import com.example.bnk_project_02s.repository.ShoppingProductsRepository;
import com.example.bnk_project_02s.repository.UserRepository;

@Service
@Transactional
public class ShoppingLogService {
    
    @Autowired
    private ShoppingLogRepository shoppingLogRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ShoppingProductsRepository shoppingProductsRepository;
    
    @Autowired
    private CardRepository cardRepository;
    
    @Autowired
    private ChildAccountRepository childAccountRepository;
    
    @Autowired
    private HistoryRepository historyRepository;
    
    public Map<String, Object> processPayment(String uid, String spno, String cardno, String slamount, String slcurrency) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findById(uid);
        if (!userOpt.isPresent()) {
            return createErrorResponse("USER_NOT_FOUND", "존재하지 않는 사용자입니다.");
        }
        
        Optional<ShoppingProducts> productOpt = shoppingProductsRepository.findById(spno);
        if (!productOpt.isPresent()) {
            return createErrorResponse("PRODUCT_NOT_FOUND", "존재하지 않는 상품입니다.");
        }
        
        Optional<Card> cardOpt = cardRepository.findById(cardno);
        if (!cardOpt.isPresent()) {
            return createErrorResponse("CARD_NOT_FOUND", "존재하지 않는 카드입니다.");
        }
        
        Card card = cardOpt.get();
        
        if (!"Y".equals(card.getCardstatus())) {
            ShoppingLog failedLog = saveFailedLog(userOpt.get(), productOpt.get(), card, slamount, slcurrency, "CC45");
            return createFailResponse("CC45", "카드가 정지 상태입니다.", failedLog, Map.of("cardstatus", card.getCardstatus()));
        }
        
     // --- 2) 자식계좌 조회 ---
        Optional<ChildAccount> childAccountOpt = childAccountRepository.findById(card.getCano());
        if (childAccountOpt.isEmpty()) return createErrorResponse("CHILD_ACCOUNT_NOT_FOUND", "연결된 계좌를 찾을 수 없습니다.");

        ChildAccount childAccount = childAccountOpt.get();

        // --- 3) 금액 파싱/검증 (BigDecimal) ---
        BigDecimal currentBalance = childAccount.getCabalance() == null ? BigDecimal.ZERO : childAccount.getCabalance();
        BigDecimal paymentAmount  = parseAmount(slamount); // "1,234.56" 등 방어

        if (currentBalance.compareTo(paymentAmount) < 0) {
            ShoppingLog failedLog = saveFailedLog(userOpt.get(), productOpt.get(), card, slamount, slcurrency, "CC10");
            return createFailResponse(
                    "CC10",
                    "잔액이 부족합니다.",
                    failedLog,
                    Map.of("currentBalance", currentBalance.toPlainString(), "requiredAmount", paymentAmount.toPlainString())
            );
        }

        // --- 4) 결제 성공 로그 저장 ---
        ShoppingLog successLog = saveSuccessLog(userOpt.get(), productOpt.get(), card, slamount, slcurrency);

        // --- 5) 잔액 차감/저장 (ChildAccount.cabalance = BigDecimal) ---
        BigDecimal newBalance = currentBalance.subtract(paymentAmount);
        childAccount.setCabalance(newBalance);
        childAccountRepository.save(childAccount); // @Transactional 이라 변경감지로도 저장되지만 명시 저장

        // --- 6) 거래내역 기록 (History) ---
        // History 매핑 가정:
        // - user, parentAccount, currency: @ManyToOne
        // - hwithdraw/hdeposit/hbalance: String (현 설계)
        // - hkrw: BigDecimal + KRW Converter(scale=0, DOWN)
        History history = History.builder()
                .user(userOpt.get())
                .parentAccount(childAccount.getParentAccount())
                .currency(childAccount.getCurrency())
                .hwithdraw(paymentAmount.toPlainString())         // 출금액(외화/원화 설계에 맞게)
                .hdeposit("0")                                     // 카드 결제 = 외화 입금 없음
                .hbalance(newBalance.toPlainString())              // 거래 후 외화 잔액(문자열 칼럼 가정)
                .hkrw(parseAmount(slamount))                       // 사용 원화 금액 (Converter가 정수화)
                .hdate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
        historyRepository.save(history);

        // --- 7) 응답 ---
        result.put("success", true);
        result.put("message", "결제가 성공적으로 완료되었습니다.");
        result.put("data", Map.of(
                "slno", successLog.getSlno(),
                "slstatus", successLog.getSlstatus(),
                "remainingBalance", newBalance.toPlainString()
        ));
        return result;
    }

    private ShoppingLog saveFailedLog(User user, ShoppingProducts product, Card card, String amount, String currency, String failReason) {
        ShoppingLog shoppingLog = ShoppingLog.builder()
                .user(user)
                .shoppingProducts(product)
                .card(card)
                .slamount(amount)
                .slcurrency(currency)
                .slstatus("N")
                .slreason(failReason)
                .build();
        return shoppingLogRepository.save(shoppingLog);
    }

    private ShoppingLog saveSuccessLog(User user, ShoppingProducts product, Card card, String amount, String currency) {
        ShoppingLog shoppingLog = ShoppingLog.builder()
                .user(user)
                .shoppingProducts(product)
                .card(card)
                .slamount(amount)
                .slcurrency(currency)
                .slstatus("Y")
                .slcomat(LocalDateTime.now())
                .build();
        return shoppingLogRepository.save(shoppingLog);
    }

    private Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> createFailResponse(String code, String message, ShoppingLog log, Map<String, String> additionalData) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);

        Map<String, Object> data = new HashMap<>();
        data.put("slno", log.getSlno());
        data.put("slstatus", log.getSlstatus());
        data.put("slreason", log.getSlreason());
        data.putAll(additionalData);

        result.put("data", data);
        return result;
    }

    /** "1,234.56" / null / "" 등 방어 파서 */
    private BigDecimal parseAmount(String s) {
        if (s == null) return BigDecimal.ZERO;
        String cleaned = s.trim().replace(",", "");
        if (cleaned.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(cleaned); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}