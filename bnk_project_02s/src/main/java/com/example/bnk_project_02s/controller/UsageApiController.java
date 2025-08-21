package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.dto.MultiSeriesDto;
import com.example.bnk_project_02s.dto.SumKrwDto;
import com.example.bnk_project_02s.dto.TimeSeriesDto;
import com.example.bnk_project_02s.service.HistoryStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usage")
public class UsageApiController {

  private final HistoryStatsService svc;

  /** 텍스트로 표시할 총 원화 사용액 */
  @GetMapping("/krw/total")
  public SumKrwDto totalKrw() { return svc.totalKrw(); }

  /** 일자별 KRW 합계 (기본 14일) */
  @GetMapping("/krw/daily")
  public TimeSeriesDto krwDaily(@RequestParam(name = "days",defaultValue = "14") int days) {
    return svc.krwDaily(days);
  }

  /** 일자별 통화별 외화 환전금액 합계 (기본 14일)
   *  통화코드 매핑이 필요하면 cuno→코드 맵을 주입해서 사용하세요.
   *  지금은 서버에서 기본 빈 맵으로 사용, 프런트에서 치환 가능.
   */
  @GetMapping("/fx/daily")
  public MultiSeriesDto fxDaily(@RequestParam(name = "days", defaultValue = "14") int days) {
    return svc.fxDailyByCurrency(days, /*cunoToCode*/ Map.of());
  }
}