package com.example.bnk_project_02s.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bnk_project_02s.service.ShoppingLogService;
import lombok.Data;
import java.util.Map;

@Controller
@RequestMapping("/user/shopping/log")
@CrossOrigin(origins = "*")
public class ShoppingLogController {
    
    @Autowired
    private ShoppingLogService shoppingLogService;
    
    @PostMapping("/saveLog")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveLog(@RequestBody SaveLogRequest request) {
        try {
            Map<String, Object> result = shoppingLogService.processPayment(
                request.getUid(), 
                request.getSpno(), 
                request.getCardno(), 
                request.getSlamount(), 
                request.getSlcurrency()
            );
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "message", "결제 처리 중 오류가 발생했습니다.",
                "error", e.getMessage(),
                "code", "PAYMENT_ERROR"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @Data
    public static class SaveLogRequest {
        private String uid;
        private String spno;
        private String cardno;
        private String slamount;
        private String slcurrency;
    }
}