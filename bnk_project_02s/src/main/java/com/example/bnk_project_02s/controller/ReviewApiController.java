package com.example.bnk_project_02s.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.dto.KeywordResult;
import com.example.bnk_project_02s.dto.MonthlyStatsDto;
import com.example.bnk_project_02s.dto.ReviewRowDto;
import com.example.bnk_project_02s.entity.Review;
import com.example.bnk_project_02s.repository.ReviewRepository;
import com.example.bnk_project_02s.service.OpenAiKeywordService;
import com.example.bnk_project_02s.service.ReviewStatsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewApiController {

    private final ReviewStatsService statsService;
    private final ReviewRepository reviewRepository;
    private final OpenAiKeywordService keywordService;

    /** 최근 리뷰 n건 (프런트에서 기대하는 형태: { items: [...] }) */
    @GetMapping("/recent")
    public Map<String, Object> recent(@RequestParam(name = "limit", defaultValue = "20") int limit) {
        int safe = Math.max(1, Math.min(limit, 100));
        List<ReviewRowDto> rows = statsService.recent(safe);
        return Map.of("items", rows);
    }

    /** 월별 리뷰수/평점 (최근 N개월) */
    @GetMapping("/statsMonthly")
    public MonthlyStatsDto statsMonthly(@RequestParam(name = "months",defaultValue = "6") int months) {
        int safe = Math.max(1, Math.min(months, 12));
        return statsService.monthly(safe); // <- 메서드명 소문자 확인!
    }

    /** 키워드(긍정/부정) - 최근 N개월 리뷰 텍스트 기반 OpenAI 분석 */
    @GetMapping("/keywords")
    public KeywordResult keywords(@RequestParam(name = "months", defaultValue = "3") int months) {
        int safe = Math.max(1, Math.min(months, 12));

        // 최근 N개월의 시작일 00:00 ~ '내일 00:00' 미만까지 포함
        LocalDate today = LocalDate.now();
        var startDt = today.minusMonths(safe - 1).withDayOfMonth(1).atStartOfDay();
        var endDtEx = today.plusDays(1).atStartOfDay();

        List<Review> rows = reviewRepository.findByDateRange(startDt, endDtEx);
        List<String> texts = rows.stream()
                .map(Review::getRvcontent)
                .filter(Objects::nonNull)
                .toList();

        try {
            return keywordService.extract(texts);
        } catch (Exception e) {
            // 원인 파악을 위해 최소한 로그는 남겨두는 걸 추천
            e.printStackTrace();
            return new KeywordResult(List.of(), List.of());
        }
    }
}
