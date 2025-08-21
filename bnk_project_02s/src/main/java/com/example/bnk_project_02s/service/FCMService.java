package com.example.bnk_project_02s.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.entity.FcmToken;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FCMService {
    
    @Autowired
    private FcmTokenService fcmTokenService;
    
    public void sendPaymentNotificationToUser(String uid, String status, String amount, String reason) {
        log.info("==== FCM 알림 발송 시작 ====");
        log.info("uid: {}, status: {}, amount: {}, reason: {}", uid, status, amount, reason);
        
        List<FcmToken> activeTokens = fcmTokenService.getActiveTokensByUserId(uid);
        log.info("활성 FCM 토큰 개수: {}", activeTokens.size());
        
        if (activeTokens.isEmpty()) {
            log.warn("사용자 {}의 활성 FCM 토큰이 없습니다.", uid);
            return;
        }
        
        for (FcmToken fcmToken : activeTokens) {
            try {
                log.info("FCM 토큰으로 알림 발송 시도: {}...", fcmToken.getFcmToken().substring(0, 20));
                sendPaymentNotification(fcmToken.getFcmToken(), status, amount, reason);
                log.info("사용자 {} (기기: {})에게 실제 푸시 알림 발송 완료!", uid, fcmToken.getDeviceType());
            } catch (Exception e) {
                log.error("사용자 {} 푸시 알림 발송 실패: {}", uid, e.getMessage());
                e.printStackTrace();
            }
        }
        log.info("==== FCM 알림 발송 종료 ====");
    }
    
    public void sendPaymentNotification(String token, String status, String amount, String reason) {
        try {
            log.info("==== 메시지 생성 시작 ====");
            log.info("토큰: {}..., 상태: {}, 금액: {}, 이유: {}", token.substring(0, 20), status, amount, reason);
            
            String title = "";
            String body = "";
            String icon = "";
            
            String merchant = "해외직구쇼핑몰";
            String cardName = "환전체크";
            String currency = "USD";
            
            if ("Y".equals(status)) {
                title = "[출금] " + cardName + " | " + merchant;
                body = currency + " " + formatAmount(amount, currency) + "\n";
                icon = "success";
            } else if ("N".equals(status)) {
                title = "[결제실패] " + cardName + " | " + merchant;
                if ("CC45".equals(reason)) {
                    body = currency + " " + formatAmount(amount, currency) + "\n정지된 카드입니다.";
                } else if ("CC10".equals(reason)) {
                    body = currency + " " + formatAmount(amount, currency) + "\n잔액이 부족합니다.";
                } else {
                    body = currency + " " + formatAmount(amount, currency) + "\n사용할 수 없는 카드입니다.";
                }
                icon = "error";
            } else {
                log.warn("알 수 없는 결제 상태: {}", status);
                return;
            }
            
            log.info("생성된 타이틀: {}", title);
            log.info("생성된 바디: {}", body);
            
            Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("type", "payment")
                .putData("status", status)
                .putData("reason", reason != null ? reason : "")
                .putData("amount", amount)
                .putData("currency", currency)
                .putData("merchant", merchant)
                .putData("cardName", cardName)
                .putData("icon", icon)
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .build();
            
            log.info("Firebase로 메시지 전송 시도...");
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("실제 푸시 알림 발송 성공! Response ID: {}", response);
            
        } catch (Exception e) {
            log.error("Firebase 메시징 오류: {}", e.getMessage());
            e.printStackTrace();
            handleTokenError(token, e);
        }
    }
    
    private void handleTokenError(String token, Exception e) {
        String errorMessage = e.getMessage();
        
        if (errorMessage != null) {
            if (errorMessage.contains("UNREGISTERED") || errorMessage.contains("INVALID_REGISTRATION")) {
                log.warn("유효하지 않은 FCM 토큰 비활성화: {}", token.substring(0, 20) + "...");
                fcmTokenService.deactivateToken(token);
            } else if (errorMessage.contains("MESSAGE_RATE_EXCEEDED")) {
                log.warn("메시지 전송 한도 초과. 잠시 후 재시도 필요");
            } else if (errorMessage.contains("DEVICE_MESSAGE_RATE_EXCEEDED")) {
                log.warn("기기별 메시지 한도 초과: {}", token.substring(0, 20) + "...");
            } else {
                log.error("FCM 오류: {}", errorMessage);
            }
        }
    }
    
    private String formatAmount(String amount, String currency) {
        try {
            double amt = Double.parseDouble(amount);
            if ("USD".equals(currency)) {
                return String.format("$%.2f", amt);
            } else if ("KRW".equals(currency)) {
                return String.format("￦%,d", (long)amt);
            } else {
                return currency + " " + String.format("%.2f", amt);
            }
        } catch (NumberFormatException e) {
            return currency + " " + amount;
        }
    }
    
    public boolean sendTestNotification(String uid, String message) {
        List<FcmToken> activeTokens = fcmTokenService.getActiveTokensByUserId(uid);
        
        if (activeTokens.isEmpty()) {
            log.warn("사용자 {}의 활성 FCM 토큰이 없습니다.", uid);
            return false;
        }
        
        for (FcmToken fcmToken : activeTokens) {
            try {
                Message testMessage = Message.builder()
                    .setToken(fcmToken.getFcmToken())
                    .setNotification(Notification.builder()
                        .setTitle("테스트 알림")
                        .setBody(message)
                        .build())
                    .putData("type", "test")
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .build();
                
                String response = FirebaseMessaging.getInstance().send(testMessage);
                log.info("실제 테스트 알림 발송 성공! Response: {} (기기: {})", 
                        response, fcmToken.getDeviceType());
                
            } catch (Exception e) {
                log.error("테스트 알림 발송 실패: {}", e.getMessage());
                return false;
            }
        }
        
        return true;
    }
}