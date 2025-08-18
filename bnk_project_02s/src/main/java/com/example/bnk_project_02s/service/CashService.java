package com.example.bnk_project_02s.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.entity.CashKrw;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.CashKrwRepository;
import com.example.bnk_project_02s.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashService {

    private final CashKrwRepository cashRepository;
    private final UserRepository userRepository;

    /** uid로 통계 조회 */
    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String uid) {
        var opt = cashRepository.findByUser_Uid(uid);
        if (opt.isEmpty()) {
            return Map.of("uid", uid, "count", 0, "totalKrw", "0");
        }
        CashKrw cash = opt.get();
        // cfxamount가 KRW Converter(BigDecimal)라면 바로 숫자 사용 가능
        BigDecimal total = cash.getCfxamount() == null ? BigDecimal.ZERO : cash.getCfxamount();
        return Map.of(
            "uid", uid,
            "count", cash.getCcount() == null ? 0 : cash.getCcount(),
            "totalKrw", total.setScale(0, RoundingMode.DOWN).toPlainString()
        );
    }

    /** 환전 성공 시 반영 (업서트) */
    @Transactional
    public void recordExchange(String uid, BigDecimal usedKrw) {
        // uid 검증/참조
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자: " + uid));

        CashKrw cash = cashRepository.findByUser_Uid(uid)
                .orElseGet(() -> CashKrw.builder()
                        .user(user)
                        .ccount(0)
                        .cfxamount(BigDecimal.ZERO) // KRW Converter 사용 시 BigDecimal로
                        .build()
                );

        int nextCount = (cash.getCcount() == null ? 0 : cash.getCcount()) + 1;
        BigDecimal cur = cash.getCfxamount() == null ? BigDecimal.ZERO : cash.getCfxamount();
        BigDecimal nextTotal = cur.add(usedKrw == null ? BigDecimal.ZERO : usedKrw)
                                  .setScale(0, RoundingMode.DOWN); // KRW 정수 보장

        cash.setCcount(nextCount);
        cash.setCfxamount(nextTotal);
        cashRepository.save(cash);
    }
}
