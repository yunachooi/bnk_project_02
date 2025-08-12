package com.example.bnk_project_02s.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserRepository userRepository;

    public Map<String, Long> genderStats() {
        long male = 0, female = 0, other = 0;

        for (Object[] row : userRepository.countByGenderRaw()) {
            String g = row[0] == null ? "" : String.valueOf(row[0]).toUpperCase();
            long cnt = ((Number) row[1]).longValue();

            if ("M".equals(g) || "MALE".equals(g) || "남".equals(g) || "남성".equals(g)) {
                male += cnt;
            } else if ("F".equals(g) || "FEMALE".equals(g) || "여".equals(g) || "여성".equals(g)) {
                female += cnt;
            } else {
                other += cnt; // null/공백/기타
            }
        }
        Map<String, Long> out = new LinkedHashMap<>();
        out.put("남성", male);
        out.put("여성", female);
        if (other > 0) out.put("기타", other);
        return out;
    }
}