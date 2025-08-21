package com.example.bnk_project_02s.controller;

import com.example.bnk_project_02s.dto.ForeignMainSummary;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.ForeignMainService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/foreign")
public class ForeignMainController {

    private final ForeignMainService service;

    // ★ 로그인 컨트롤러에서 쓰는 세션 키와 동일해야 함: "LOGIN_USER"
    private static final String LOGIN_USER = "LOGIN_USER";

    @GetMapping("/summary")
    public ResponseEntity<ForeignMainSummary> summary(HttpSession session) {
        // ★ 디버그 로그: 현재 세션과 로그인 객체 여부 확인
        System.out.println("[/api/foreign/summary] sid=" + session.getId()
                + ", has LOGIN_USER=" + (session.getAttribute(LOGIN_USER) != null));

        User me = (User) session.getAttribute(LOGIN_USER);
        if (me == null) {
            return ResponseEntity.ok(
                ForeignMainSummary.builder()
                    .isLoggedIn(false).hasProducts(false).accountData(null)
                    .build()
            );
        }

        var res = service.getSummary(me.getUid());
        System.out.println("[/api/foreign/summary] uid=" + me.getUid() + ", hasProducts=" + res.isHasProducts());
        return ResponseEntity.ok(res);
    }
}
