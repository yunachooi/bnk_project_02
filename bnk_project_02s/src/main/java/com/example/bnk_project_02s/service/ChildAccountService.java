package com.example.bnk_project_02s.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.dto.ChildAccountRes;
import com.example.bnk_project_02s.entity.Card;
import com.example.bnk_project_02s.entity.ChildAccount;
import com.example.bnk_project_02s.entity.Currency;
import com.example.bnk_project_02s.entity.ParentAccount;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.repository.CardRepository;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.CurrencyRepository;
import com.example.bnk_project_02s.repository.ParentAccountRepository;
import com.example.bnk_project_02s.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChildAccountService {

    private final ParentAccountRepository parentRepo;
    private final ChildAccountRepository childRepo;
    private final CurrencyRepository currencyRepo;
    private final CardRepository cardRepo;
    private final UserRepository  userRepository;

    @Transactional
    public ChildAccountRes createChildAccount(String uid, String cuno, @Nullable BigDecimal initialAmt) {
        // 1) 부모계좌 조회(없으면 생성해도 되지만, 현재 정책대로면 없으면 에러)
        ParentAccount pa = parentRepo.findByUser_Uid(uid)
            .orElseThrow(() -> new IllegalStateException("부모계좌가 없습니다."));

        // 2) 통화 검증
        Currency cur = currencyRepo.findByCuno(cuno)
            .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 통화: " + cuno));

        // 3) 이미 해당 통화의 자식계좌가 있으면 409
        if (childRepo.findByParentAccount_PanoAndCurrency_Cuno(pa.getPano(), cuno).isPresent()) {
            throw new DuplicateKeyException("이미 가입된 통화입니다: " + cuno);
        }

        // 4) 자식계좌 생성
        ChildAccount ca = new ChildAccount();
        ca.setCano(generateCano());               // 16자리 등 원하는 규칙
        ca.setParentAccount(pa);
        ca.setCurrency(cur);
        ca.setCabalance(initialAmt != null ? initialAmt : BigDecimal.ZERO);
        ca.setCajoin(LocalDate.now());
        ca.setPabank(pa.getPabank());            // 스키마상 칼럼이 있다면
        childRepo.save(ca);

        // 5) 카드 보장(사용자 1장 정책이면 없을 때만 발급)
        ensureUserCard(uid);

        return new ChildAccountRes(ca.getCano(), pa.getPano(), cur.getCuno(), cur.getCuname(), ca.getCabalance());
    }

    private String generateCano() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void ensureUserCard(String uid) {
        if (cardRepo.existsByUser_Uid(uid)) return;
        Card card = new Card();
        card.setCardno(makeCardNo());
        card.setCardcvc(ThreadLocalRandom.current().nextInt(100, 999));
        card.setCardstatus("A");
        card.setCardname("BNK 쇼핑환전 카드");
        User userRef = userRepository.getReferenceById(uid); // 또는 findById(uid).orElseThrow(...)
        card.setUser(userRef);
        card.setCarddate(LocalDateTime.now());
        card.setCano(null); // 카드가 특정 자식계좌에 종속되지 않는 모델이라면 null
        cardRepo.save(card);
    }

    private String makeCardNo() {
        // 4-4-4-4 형식
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return String.format("%04d-%04d-%04d-%04d", r.nextInt(10000), r.nextInt(10000), r.nextInt(10000), r.nextInt(10000));
    }
}