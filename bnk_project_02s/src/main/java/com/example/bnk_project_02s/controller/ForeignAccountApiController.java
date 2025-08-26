package com.example.bnk_project_02s.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.dto.ChildAccountRes;
import com.example.bnk_project_02s.dto.CreateChildAccountReq;
import com.example.bnk_project_02s.service.ChildAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/foreign")
public class ForeignAccountApiController {

    private final ChildAccountService childAccountService;

    @PostMapping("/child-accounts")
    public ResponseEntity<?> createChildAccount(
            HttpServletRequest request,
            @RequestHeader(name = "X-UID", required = false) String headerUid,
            @RequestParam(name = "uid", required = false) String paramUid,
            @RequestBody CreateChildAccountReq req) {

        // 1) 세션
        HttpSession session = request.getSession(false);
        String uid = (session != null) ? (String) session.getAttribute("uid") : null;

        // 2) 헤더 → 3) 파라미터 → 4) 바디(uid)
        if (uid == null || uid.isBlank()) uid = headerUid;
        if (uid == null || uid.isBlank()) uid = paramUid;
        if (uid == null || uid.isBlank()) uid = req.getUid();

        if (uid == null || uid.isBlank())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("uid가 없습니다.");

        if (req.getCuno() == null || req.getCuno().isBlank())
            return ResponseEntity.badRequest().body("cuno 필수");

        try {
            ChildAccountRes created =
                childAccountService.createChildAccount(uid, req.getCuno(), req.getAmount());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 가입된 통화입니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(e.getMessage());
        }
    }
}
