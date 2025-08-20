package com.example.bnk_project_02s.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.entity.ShoppingLog;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.entity.ShoppingProducts;
import com.example.bnk_project_02s.entity.Card;
import com.example.bnk_project_02s.entity.ChildAccount;
import com.example.bnk_project_02s.entity.History;
import com.example.bnk_project_02s.repository.ShoppingLogRepository;
import com.example.bnk_project_02s.repository.UserRepository;
import com.example.bnk_project_02s.repository.ShoppingProductsRepository;
import com.example.bnk_project_02s.repository.CardRepository;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.HistoryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    
    @Autowired
    private FCMService fcmService;
    
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
        
        User user = userOpt.get();
        ShoppingProducts product = productOpt.get();
        Card card = cardOpt.get();
        
        if (!"Y".equals(card.getCardstatus())) {
            ShoppingLog failedLog = saveFailedLog(user, product, card, slamount, slcurrency, "CC45");
            
            fcmService.sendPaymentNotificationToUser(user.getUid(), "N", slamount, product.getSpname());
            
            return createFailResponse("CC45", "카드가 정지 상태입니다.", failedLog, Map.of("cardstatus", card.getCardstatus()));
        }
        
        Optional<ChildAccount> childAccountOpt = childAccountRepository.findByCano(card.getCano());
        if (!childAccountOpt.isPresent()) {
            return createErrorResponse("CHILD_ACCOUNT_NOT_FOUND", "연결된 계좌를 찾을 수 없습니다.");
        }
        
        ChildAccount childAccount = childAccountOpt.get();
        BigDecimal currentBalance = childAccount.getCabalance() != null ? 
            childAccount.getCabalance() : BigDecimal.ZERO;
        BigDecimal paymentAmount = new BigDecimal(slamount);
        
        if (currentBalance.compareTo(paymentAmount) < 0) {
            ShoppingLog failedLog = saveFailedLog(user, product, card, slamount, slcurrency, "CC10");
            
            fcmService.sendPaymentNotificationToUser(user.getUid(), "N", slamount, product.getSpname());
            
            return createFailResponse("CC10", "잔액이 부족합니다.", failedLog, 
                Map.of("currentBalance", currentBalance.toString(), "requiredAmount", paymentAmount.toString()));
        }
        
        ShoppingLog successLog = saveSuccessLog(user, product, card, slamount, slcurrency);
        
        BigDecimal newBalance = currentBalance.subtract(paymentAmount);
        childAccount.setCabalance(newBalance);
        childAccountRepository.save(childAccount);
        
        History history = History.builder()
            .user(user)
            .parentAccount(childAccount.getParentAccount()) 
            .currency(childAccount.getCurrency())
            .hwithdraw(slamount)
            .hdeposit("0")
            .hbalance(newBalance.toString())
            .hkrw(paymentAmount)
            .hdate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .build();
        
        historyRepository.save(history);
        
        fcmService.sendPaymentNotificationToUser(user.getUid(), "Y", slamount, product.getSpname());
        
        result.put("success", true);
        result.put("message", "결제가 성공적으로 완료되었습니다.");
        result.put("data", Map.of(
            "slno", successLog.getSlno(),
            "slstatus", successLog.getSlstatus(),
            "remainingBalance", newBalance.toString()
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
}