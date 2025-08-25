package com.example.bnk_project_02s.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.StatsSeries;
import com.example.bnk_project_02s.repo.DayCount;
import com.example.bnk_project_02s.repo.MonthCount;
import com.example.bnk_project_02s.repo.QuarterCount;
import com.example.bnk_project_02s.repo.SubscriberStatsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriberStatsService {

    private final SubscriberStatsRepository repo;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public StatsSeries daily(int days) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate start = today.minusDays(days - 1);
        LocalDate endExcl = today.plusDays(1);

        Map<LocalDate, Long> map = new HashMap<>();
        for (DayCount row : repo.countDaily(start, endExcl)) {
            map.put(row.getD(), row.getC());
        }

        List<String> labels = new ArrayList<>(days);
        List<Long> data = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            labels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            data.add(map.getOrDefault(d, 0L));
        }

        // Lombok O: builder, Lombok X: new StatsSeries(labels, data)
        return StatsSeries.builder().labels(labels).data(data).build();
    }

    public StatsSeries monthly(int months) {
        LocalDate firstOfThisMonth = LocalDate.now(ZONE).withDayOfMonth(1);
        LocalDate start = firstOfThisMonth.minusMonths(months - 1);
        LocalDate endExcl = firstOfThisMonth.plusMonths(1);

        Map<String, Long> map = new HashMap<>();
        for (MonthCount row : repo.countMonthly(start, endExcl)) {
            map.put(row.getYm(), row.getC()); // key: YYYY-MM
        }

        List<String> labels = new ArrayList<>(months);
        List<Long> data = new ArrayList<>(months);
        for (int i = 0; i < months; i++) {
            LocalDate m = start.plusMonths(i);
            String key = String.format("%04d-%02d", m.getYear(), m.getMonthValue());
            labels.add(m.getMonthValue() + "월");
            data.add(map.getOrDefault(key, 0L));
        }

        return StatsSeries.builder().labels(labels).data(data).build();
    }

    public StatsSeries quarterly(int quarters) {
        LocalDate today = LocalDate.now(ZONE);
        int q = ((today.getMonthValue() - 1) / 3) + 1;
        LocalDate qStart = LocalDate.of(today.getYear(), (q - 1) * 3 + 1, 1);
        LocalDate start = qStart.minusMonths((quarters - 1) * 3);
        LocalDate endExcl = qStart.plusMonths(3);

        Map<String, Long> map = new HashMap<>();
        for (QuarterCount row : repo.countQuarterly(start, endExcl)) {
            map.put(row.getYq(), row.getC()); // key: "YYYY Qn"
        }

        List<String> labels = new ArrayList<>(quarters);
        List<Long> data = new ArrayList<>(quarters);
        for (int i = 0; i < quarters; i++) {
            LocalDate p = start.plusMonths(i * 3L);
            String key = p.getYear() + " Q" + (((p.getMonthValue() - 1) / 3) + 1);
            labels.add(key);
            data.add(map.getOrDefault(key, 0L));
        }

        return StatsSeries.builder().labels(labels).data(data).build();
    }
}