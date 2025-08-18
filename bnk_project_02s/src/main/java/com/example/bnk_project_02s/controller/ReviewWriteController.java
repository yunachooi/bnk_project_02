package com.example.bnk_project_02s.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.example.bnk_project_02s.entity.Review;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.ReviewRepository;
import com.example.bnk_project_02s.service.ReviewStatsService; // 있으면 사용

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewWriteController {

    private final ReviewRepository reviewRepo;

    private static final String LOGIN_USER = "LOGIN_USER";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object RVNO_LOCK = new Object();

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public Map<String, Object> create(@RequestParam("content") String content,
                                      @RequestParam(value = "rating", required = false) String rating,
                                      HttpSession session) {
        Object u = session.getAttribute(LOGIN_USER);
        if (!(u instanceof User login)) {
            return Map.of("ok", false, "error", "LOGIN_REQUIRED");
        }

        String body = content == null ? "" : content.trim();
        if (body.isEmpty()) return Map.of("ok", false, "error", "EMPTY_CONTENT");
        if (body.length() > 1000) body = body.substring(0, 1000);

        int rate = 5;
        if (StringUtils.hasText(rating)) {
            try { rate = Integer.parseInt(rating.trim()); } catch (Exception ignore) {}
        }
        if (rate < 1) rate = 1;
        if (rate > 5) rate = 5;

        String next;
        synchronized (RVNO_LOCK) {
            int max = reviewRepo.findMaxRvno();
            next = String.valueOf(max + 1);
            Review r = Review.builder()
                    .rvno(next)
                    .uid(login.getUid())
                    .rvcontent(body)
                    .rvrating(String.valueOf(rate))
                    .rvdate(LocalDateTime.now().format(FMT))
                    .build();
            reviewRepo.save(r);
        }
        return Map.of("ok", true, "rvno", next);
    }
}