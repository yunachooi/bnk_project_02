package com.example.bnk_project_02s.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.example.bnk_project_02s.entity.FcmToken;
import com.example.bnk_project_02s.service.FcmTokenService;
import java.util.List;

@Service
@Slf4j
public class FCMService {
    
    @Autowired
    private FcmTokenService fcmTokenService;
    
    public void sendPaymentNotificationToUser(String uid, String status, String amount, String productName) {
        List<FcmToken> activeTokens = fcmTokenService.getActiveTokensByUserId(uid);
        
        if (activeTokens.isEmpty()) {
            log.warn("사용자 {}의 활성 FCM 토큰이 없습니다.", uid);
            return;
        }
        
        for (FcmToken fcmToken : activeTokens) {
            try {
                sendPaymentNotification(fcmToken.getFcmToken(), status, amount, productName);
                log.info("사용자 {} (기기: {})에게 실제 푸시 알림 발송 완료!", uid, fcmToken.getDeviceType());
            } catch (Exception e) {
                log.error("사용자 {} 푸시 알림 발송 실패: {}", uid, e.getMessage());
            }
        }
    }
    
    public void sendPaymentNotification(String token, String status, String amount, String productName) {
        try {
            String title = "";
            String body = "";
            String icon = "";
            
            if ("Y".equals(status)) {
                title = "[승인] 결제 완료";
                body = productName + " 결제가 승인되었습니다. (￦" + formatAmount(amount) + ")";
                icon = "success";
            } else if ("N".equals(status)) {
                title = "[실패] 결제 실패";
                body = productName + " 결제가 실패했습니다. (￦" + formatAmount(amount) + ")";
                icon = "error";
            } else {
                log.warn("알 수 없는 결제 상태: {}", status);
                return;
            }
            
            Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("type", "payment")
                .putData("status", status)
                .putData("amount", amount)
                .putData("productName", productName)
                .putData("icon", icon)
                .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                .build();
            
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("실제 푸시 알림 발송 성공! Response ID: {}", response);
            
        } catch (Exception e) {
            log.error("Firebase 메시징 오류: {}", e.getMessage());
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
    
    private String formatAmount(String amount) {
        try {
            long amt = Long.parseLong(amount);
            return String.format("%,d", amt);
        } catch (NumberFormatException e) {
            return amount;
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