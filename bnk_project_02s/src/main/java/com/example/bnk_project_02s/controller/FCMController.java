package com.example.bnk_project_02s.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.bnk_project_02s.service.FcmTokenService;
import lombok.Data;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class FCMController {
    
    @Autowired
    private FcmTokenService fcmTokenService;
    
    @PostMapping("/fcm-token")
    public ResponseEntity<Map<String, Object>> saveFCMToken(@RequestBody FCMTokenRequest request) {
        try {
            boolean saved = fcmTokenService.saveOrUpdateToken(
                request.getUid(), 
                request.getFcmToken(), 
                request.getDeviceType(), 
                request.getDeviceId()
            );
            
            if (saved) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "FCM 토큰이 성공적으로 저장되었습니다."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "사용자를 찾을 수 없습니다."
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "FCM 토큰 저장 중 오류가 발생했습니다.",
                "error", e.getMessage()
            ));
        }
    }
    
    @Data
    public static class FCMTokenRequest {
        private String uid;
        private String fcmToken;
        private String deviceType;
        private String deviceId;
    }
}