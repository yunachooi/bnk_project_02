// com.example.bnk_project_02s.service.InterestService
package com.example.bnk_project_02s.service;

import com.example.bnk_project_02s.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InterestService {

    private final UserRepository userRepository;

    private static final List<String> CURRENCY_KEYS =
            List.of("USD","JPY","CNH","EUR","CHF","VND");
    private static final List<String> TOPIC_KEYS =
            List.of("TRAVEL","STUDY","SHOPPING","FINANCE","ETC");

    public Map<String, Integer> countByCurrency() {
        var rows = userRepository.countInterestCurrenciesCsv(); // List<Object[]> [k, cnt]
        return toFixedMap(rows, CURRENCY_KEYS);
    }

    public Map<String, Integer> countByTopic() {
        var rows = userRepository.countInterestTopicsCsv(); // List<Object[]> [k, cnt]
        return toFixedMap(rows, TOPIC_KEYS);
    }

    private Map<String,Integer> toFixedMap(List<Object[]> rows, List<String> keys){
        Map<String,Integer> out = new LinkedHashMap<>();
        keys.forEach(k -> out.put(k, 0));
        for (Object[] r : rows) {
            String k = (r[0] == null ? "" : r[0].toString().trim().toUpperCase());
            int cnt  = (r[1] == null ? 0 : Integer.parseInt(r[1].toString()));
            if (out.containsKey(k)) out.put(k, cnt);
        }
        return out;
    }
}
