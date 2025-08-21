package com.example.bnk_project_02s.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.MultiSeriesDto;
import com.example.bnk_project_02s.dto.NamedSeriesDto;
import com.example.bnk_project_02s.dto.SumKrwDto;
import com.example.bnk_project_02s.dto.TimeSeriesDto;
import com.example.bnk_project_02s.repository.HistoryStatsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoryStatsService {
  private final HistoryStatsRepository repo;

  public SumKrwDto totalKrw() {
    return new SumKrwDto(repo.sumTotalKrw());
  }

  public TimeSeriesDto krwDaily(int days) {
    int n = Math.max(1, Math.min(days, 90));
    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusDays(n - 1);
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime endEx = today.plusDays(1).atStartOfDay();

    // 라벨(연속 날짜)
    List<String> labels = new ArrayList<>();
    Map<String, BigDecimal> map = new LinkedHashMap<>();
    for (int i=0;i<n;i++) {
      LocalDate d = startDate.plusDays(i);
      String key = d.toString(); // YYYY-MM-DD
      labels.add(key);
      map.put(key, BigDecimal.ZERO);
    }

    // 쿼리/적재
    for (Object[] row : repo.sumKrwByDay(start, endEx)) {
      String d = String.valueOf(row[0]); // yyyy-MM-dd
      BigDecimal v = (row[1] instanceof BigDecimal) ? (BigDecimal) row[1]
                       : new BigDecimal(String.valueOf(row[1]));
      if (map.containsKey(d)) map.put(d, v);
    }

    List<BigDecimal> data = labels.stream().map(map::get).toList();
    return new TimeSeriesDto(labels, data);
  }

  public MultiSeriesDto fxDailyByCurrency(int days, Map<String,String> cunoToCode) {
    int n = Math.max(1, Math.min(days, 90));
    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusDays(n - 1);
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime endEx = today.plusDays(1).atStartOfDay();

    List<String> labels = new ArrayList<>();
    for (int i=0;i<n;i++) labels.add(startDate.plusDays(i).toString());

    // cuno → 통화코드(없으면 cuno 그대로)
    Map<String,String> toName = (cunoToCode!=null)? cunoToCode : Map.of();

    // 날짜축 초기화
    Map<String, Map<String, BigDecimal>> table = new LinkedHashMap<>();
    for (String d: labels) table.put(d, new HashMap<>());

    for (Object[] row : repo.sumFxByDayCurrency(start, endEx)) {
      String d = String.valueOf(row[0]);                // yyyy-MM-dd
      String cuno = String.valueOf(row[1]);             // 통화 FK
      BigDecimal amt = (row[2] instanceof BigDecimal) ? (BigDecimal) row[2]
                          : new BigDecimal(String.valueOf(row[2]));
      table.get(d).merge(cuno, amt, BigDecimal::add);
    }

    // 통화 목록 수집(라벨 순서 보장)
    LinkedHashSet<String> cunoSet = new LinkedHashSet<>();
    for (String d : labels) cunoSet.addAll(table.get(d).keySet());

    List<NamedSeriesDto> series = new ArrayList<>();
    for (String cuno : cunoSet) {
      String name = toName.getOrDefault(cuno, cuno);  // 'USD' 같은 코드 매핑
      List<BigDecimal> data = new ArrayList<>();
      for (String d : labels) data.add(table.get(d).getOrDefault(cuno, BigDecimal.ZERO));
      series.add(new NamedSeriesDto(name, data));
    }

    return new MultiSeriesDto(labels, series);
  }
}
