package com.example.bnk_project_02s.dto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.ForeignIssueService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/foreign")
public class ForeignIssueController {

    private final ForeignIssueService service;
    public ForeignIssueController(ForeignIssueService service) {
        this.service = service;
    }

    @PostMapping("/final-issue")
    public ResponseEntity<IssuanceResult> finalIssue(
            @RequestBody FinalIssueRequest req,
            HttpSession session
    ) {
        User login = (User) session.getAttribute("LOGIN_USER");
        if (login == null) return ResponseEntity.status(401).build();

        IssuanceResult result = service.finalIssue(login.getUid(), req);
        return ResponseEntity.ok(result);
    }
}
