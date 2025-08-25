// AutofillController.java
package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.profile.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AutofillController {

    private final ProfileService profileService;

    @GetMapping("/autofill")
    public ResponseEntity<?> autofill(HttpSession session) {
        // 로그인 사용자 확인 (UserController가 넣어둔 세션 키)
        Object obj = session != null ? session.getAttribute("LOGIN_USER") : null;
        if (!(obj instanceof User u) || u.getUid() == null || u.getUid().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHENTICATED"));
        }

        // 프로필(이름/폰/주민번호 마스킹)
        var v = profileService.get(u.getUid());

        // foreign4에서 세션에 넣어둔 값(있으면 함께 내려줌)
        String bank = session != null ? (String) session.getAttribute("BANK_NAME_TMP") : null;
        String acct = session != null ? (String) session.getAttribute("ACCOUNT_NUMBER_TMP") : null;

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("name",           v.getName());
        resp.put("phone",          v.getPhonePlain());
        resp.put("residentMasked", v.getResidentIdMasked());

        if (bank != null && !bank.isBlank() && acct != null && !acct.isBlank()) {
            resp.put("bankName", bank);
            resp.put("accountMasked", acct); // 마스킹해서만 내려감
        }

        return ResponseEntity.ok(resp);
    }

    // ****-****-1234 형태로 마스킹
    private String maskAccount(String acct){
        if (acct == null || acct.isBlank()) return "";
        acct = acct.replaceAll("\\D", "");
        if (acct.length() <= 4) return "****";
        return "****-****-" + acct.substring(acct.length() - 4);
    }
}
