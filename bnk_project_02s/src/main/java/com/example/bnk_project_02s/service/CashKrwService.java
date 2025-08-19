package com.example.bnk_project_02s.service;

import com.example.bnk_project_02s.entity.CashKrw;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.CashKrwRepository;
import com.example.bnk_project_02s.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashKrwService {

    private final CashKrwRepository cashRepo;
    private final UserRepository userRepo;

    /**
     * 이번 거래의 원화금액(정수)을 누적.
     * - 행이 없으면 새로 만들고 ccount=1, cfxamount=이번금액
     * - 있으면 ccount+=1, cfxamount+=이번금액
     */
    @Transactional
    public CashKrw accumulate(String uid, BigDecimal krwAmount) {
        if (krwAmount == null) krwAmount = BigDecimal.ZERO;

        var opt = cashRepo.findByUser_Uid(uid);
        if (opt.isEmpty()) {
            // 유저 로드
            User user = userRepo.findByUid(uid)
                    .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다: " + uid));

            CashKrw created = CashKrw.builder()
                    .user(user)
                    .ccount(1)
                    .cfxamount(krwAmount) // 누적 시작
                    .build();
            return cashRepo.save(created);
        } else {
            CashKrw row = opt.get();
            int prevCount = row.getCcount() == null ? 0 : row.getCcount();
            BigDecimal prevAmt = row.getCfxamount() == null ? BigDecimal.ZERO : row.getCfxamount();

            row.setCcount(prevCount + 1);
            row.setCfxamount(prevAmt.add(krwAmount));
            return cashRepo.save(row);
        }
    }
}
