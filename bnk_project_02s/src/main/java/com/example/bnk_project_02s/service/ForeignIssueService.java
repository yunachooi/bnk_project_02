// src/main/java/.../service/ForeignIssueService.java
package com.example.bnk_project_02s.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.FinalIssueRequest;          // ↔ 클래스(getter) 사용
import com.example.bnk_project_02s.dto.IssuanceResult;
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

import jakarta.transaction.Transactional;

@Service
public class ForeignIssueService {

    private final ParentAccountRepository parentRepo;
    private final ChildAccountRepository childRepo;
    private final CardRepository cardRepo;
    private final UserRepository userRepo;
    private final CurrencyRepository currencyRepo;

    public ForeignIssueService(
            ParentAccountRepository parentRepo,
            ChildAccountRepository childRepo,
            CardRepository cardRepo,
            UserRepository userRepo,
            CurrencyRepository currencyRepo
    ) {
        this.parentRepo = parentRepo;
        this.childRepo = childRepo;
        this.cardRepo = cardRepo;
        this.userRepo = userRepo;
        this.currencyRepo = currencyRepo;
    }

    @Transactional
    public IssuanceResult finalIssue(String uid, FinalIssueRequest req) {
        // 0) 유저 조회
        User user = userRepo.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + uid));

        // 1) 부모계좌: 있으면 재사용, 없으면 생성/저장
        ParentAccount parent = parentRepo.findFirstByUser_Uid(uid)
                .orElseGet(() -> parentRepo.save(
                        ParentAccount.builder()
                                .user(user)
                                .fno(Optional.ofNullable(req.getFno()).orElse(1)) // getter 사용
                                .pabank(req.getPabank())                           // getter 사용
                                .build()
                ));

        // 2) 통화별 자식계좌: 중복은 건너뛰고 없으면 생성
        List<String> wantCurrencies = Optional.ofNullable(req.getCurrencies())      // getter 사용
                .map(list -> list.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList())
                .filter(list -> !list.isEmpty())
                .orElse(List.of("USD")); // 기본 USD 하나

        List<ChildAccount> createdOrExisting = new ArrayList<>();

        for (String alpha : wantCurrencies) {           // alpha 코드: "USD", "JPY"...
            // 통화 엔티티 조회 (예: findByCuname("USD"))
            Currency cur = currencyRepo.findByCuname(alpha)
                    .orElseThrow(() -> new IllegalArgumentException("통화가 없습니다: " + alpha));

            // 부모 PANO + 통화(alpha) 조합으로 자식계좌 존재 확인
            ChildAccount child = childRepo
                    .findByParentAccount_PanoAndCurrency_Cuname(parent.getPano(), alpha)
                    .orElseGet(() -> childRepo.save(
                            ChildAccount.builder()
                                    .parentAccount(parent)
                                    .currency(cur)
                                    .cabalance(BigDecimal.ZERO)
                                    .pabank(req.getPabank())   // getter 사용
                                    .build()
                    ));

            createdOrExisting.add(child);
        }

        // 대표 cano (첫 번째)
        String primaryCano = createdOrExisting.get(0).getCano();

        // 3) 카드: uid 당 1장. 있으면 재사용, 없으면 생성
        Card card = cardRepo.findByUser_Uid(uid).orElseGet(() ->
                cardRepo.save(
                        Card.builder()
                                .user(user)
                                .cano(primaryCano)
                                .cardname(
                                        Optional.ofNullable(req.getCardName())     // getter 사용
                                                .orElse("BNK 쇼핑환전체크카드")
                                )
                                .build()
                )
        );

        // 4) 결과 리턴
        return new IssuanceResult(
                parent.getPano(),
                createdOrExisting.stream().map(ChildAccount::getCano).toList(),
                mask(card.getCardno()),
                card.getCardstatus()
        );
    }

    private String mask(String cardNo) {
        if (cardNo == null || cardNo.length() < 4) return "****";
        return "**** **** **** " + cardNo.substring(cardNo.length() - 4);
    }
}
