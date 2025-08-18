package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bnk_project_02s.dto.CardDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.CardService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/card")
@CrossOrigin(origins = "*")
public class CardController {
    
    private static final String LOGIN_USER = "LOGIN_USER";
    
    @Autowired
    private CardService cardService;
    
    @GetMapping("")
    public String showCardManagement(Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return "redirect:/user/login";
        }
        
        try {
            CardDto cardInfo = cardService.getCardByUserId(loginUser.getUid());
            model.addAttribute("cardInfo", cardInfo);
        } catch (Exception e) {
            model.addAttribute("cardInfo", null);
            model.addAttribute("error", "카드 정보를 찾을 수 없습니다.");
        }
        
        return "user/card/cardManagement";
    }
    
    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity<?> getCardInfo(HttpSession session) {
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
    
    @GetMapping("/toggle-status")
    @ResponseBody
    public ResponseEntity<?> toggleCardStatus(HttpSession session) {
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
    
    @GetMapping("/full-number")
    @ResponseBody
    public ResponseEntity<?> getFullCardNumber(HttpSession session) {
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