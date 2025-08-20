package com.example.bnk_project_02s.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.bnk_project_02s.service.FCMService;
import lombok.Data;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class TestNotificationController {
    
    @Autowired
    private FCMService fcmService;
    
    @PostMapping("/test-notification")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@RequestBody TestNotificationRequest request) {
        try {
            boolean success = fcmService.sendTestNotification(request.getUid(), request.getMessage());
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "테스트 알림이 발송되었습니다."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "활성 FCM 토큰을 찾을 수 없습니다."
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "테스트 알림 발송 중 오류가 발생했습니다.",
                "error", e.getMessage()
            ));
        }
    }
    
    @Data
    public static class TestNotificationRequest {
        private String uid;
        private String message;
    }
}