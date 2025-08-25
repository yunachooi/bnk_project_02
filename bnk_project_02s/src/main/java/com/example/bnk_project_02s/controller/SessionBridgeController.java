package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/session")
public class SessionBridgeController {

    @PostMapping("/account")
    public ResponseEntity<Void> saveAccount(@RequestBody Map<String,String> body, HttpSession session) {
        String bank = body.getOrDefault("bankName", "").trim();
        String acct = body.getOrDefault("accountNumber", "").replaceAll("\\D", "").trim(); // 숫자만
        if (bank.isEmpty() || acct.isEmpty()) return ResponseEntity.badRequest().build();
        session.setAttribute("BANK_NAME_TMP", bank);
        session.setAttribute("ACCOUNT_NUMBER_TMP", acct);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/account/clear")
    public ResponseEntity<Void> clearAccount(HttpSession session) {
        session.removeAttribute("BANK_NAME_TMP");
        session.removeAttribute("ACCOUNT_NUMBER_TMP");
        return ResponseEntity.ok().build();
    }
}