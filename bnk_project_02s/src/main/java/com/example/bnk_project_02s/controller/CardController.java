package com.example.bnk_project_02s.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
    public CardDto getCardInfo(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        
        return cardService.getCardByUserId(loginUser.getUid());
    }
    
    @GetMapping("/toggle-status")
    @ResponseBody
    public String toggleCardStatus(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return "로그인이 필요합니다.";
        }
        
        try {
            boolean result = cardService.toggleCardStatus(loginUser.getUid());
            return result ? "카드 상태가 변경되었습니다." : "카드 상태 변경에 실패했습니다.";
        } catch (Exception e) {
            return "카드 상태 변경 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    
    @GetMapping("/full-number")
    @ResponseBody
    public String getFullCardNumber(HttpSession session) {
        User loginUser = (User) session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return "로그인이 필요합니다.";
        }
        
        try {
            return cardService.getFullCardNumber(loginUser.getUid());
        } catch (Exception e) {
            return "카드번호 조회 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}