package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bnk_project_02s.dto.CardDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.CardService;

import jakarta.servlet.http.HttpSession;

@Controller
@CrossOrigin(origins = "*")
public class CardApiController {
    
    private static final String LOGIN_USER = "LOGIN_USER";
    
    @Autowired
    private CardService cardService;
    
    @GetMapping("/api/session-check")
    @ResponseBody
    public ResponseEntity<?> checkSession(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        return ResponseEntity.ok(Map.of(
            "uid", loginUser.getUid(),
            "uname", loginUser.getUname(),
            "urole", loginUser.getUrole(),
            "sessionId", session.getId()
        ));
    }
    
    @GetMapping("/api/auth/token")
    @ResponseBody
    public ResponseEntity<?> getAuthToken(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        String token = session.getId();
        return ResponseEntity.ok(Map.of(
            "token", token,
            "uid", loginUser.getUid(),
            "uname", loginUser.getUname()
        ));
    }
    
    @GetMapping("/api/card/info")
    @ResponseBody
    public ResponseEntity<?> getApiCardInfo(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        try {
            CardDto cardInfo = cardService.getCardByUserId(loginUser.getUid());
            return ResponseEntity.ok(cardInfo);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "카드 정보를 찾을 수 없습니다."));
        }
    }
    
    @GetMapping("/api/card/toggle-status")
    @ResponseBody
    public ResponseEntity<?> toggleApiCardStatus(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        try {
            boolean result = cardService.toggleCardStatus(loginUser.getUid());
            if (result) {
                return ResponseEntity.ok(Map.of("message", "카드 상태가 변경되었습니다."));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "카드 상태 변경에 실패했습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "카드 상태 변경 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/api/card/full-number")
    @ResponseBody
    public ResponseEntity<?> getApiFullCardNumber(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        try {
            String fullNumber = cardService.getFullCardNumber(loginUser.getUid());
            return ResponseEntity.ok(Map.of(
                "cardno", fullNumber,
                "message", "전체 카드번호 조회 완료"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "카드번호 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}