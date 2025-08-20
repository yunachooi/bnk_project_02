package com.example.bnk_project_02s.service;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.ForeignMainSummary;
import com.example.bnk_project_02s.entity.ChildAccount;
import com.example.bnk_project_02s.entity.ParentAccount;
import com.example.bnk_project_02s.repository.ChildAccountRepository;
import com.example.bnk_project_02s.repository.ParentAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ForeignMainService {
    private final ParentAccountRepository parentRepo;
    private final ChildAccountRepository childRepo;
    
 // 필요시 통화 한글명 매핑(엔티티에 한글명이 없을 때만 사용)
    private static final Map<String,String> KR_NAMES = Map.of(
        "USD","미국 달러","JPY","일본 엔","EUR","유럽 유로","CNY","중국 위안",
        "GBP","영국 파운드","AUD","호주 달러","CAD","캐나다 달러","CHF","스위스 프랑","HKD","홍콩 달러"
    );
    
    public ForeignMainSummary getSummary(String uid) {
        // 로그인은 컨트롤러에서 확인했다고 가정
        Optional<ParentAccount> parentOpt = parentRepo.findByUser_Uid(uid);
        if (parentOpt.isEmpty()) {
            return ForeignMainSummary.builder()
                    .isLoggedIn(true)
                    .hasProducts(false)
                    .accountData(null)
                    .build();
        }
        ParentAccount parent = parentOpt.get();
        List<ChildAccount> children = childRepo.findByParentAccount_Pano(parent.getPano());

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        List<ForeignMainSummary.ChildAccountSummary> childSummaries = children.stream()
            .sorted(Comparator.comparing(c -> c.getCurrency().getCuname())) // 코드 정렬
            .map(c -> {
                String code = c.getCurrency().getCuname(); // 예: USD
                String name = null;
                try {
                    // 엔티티에 한글명이 있다면 사용 (예: getKname/getKoName 등 프로젝트에 맞게)
                    name = (String)c.getCurrency().getClass().getMethod("getKname").invoke(c.getCurrency());
                } catch (Exception ignore) { /* 필드 없으면 아래 KR_NAMES 사용 */ }
                if (name == null || name.isBlank()) name = KR_NAMES.getOrDefault(code, code);

                String balanceStr = nf.format(
                    Optional.ofNullable(c.getCabalance()).orElse(java.math.BigDecimal.ZERO)
                );
                return ForeignMainSummary.ChildAccountSummary.builder()
                        .currencyCode(code)
                        .currencyFullName(name)
                        .balance(balanceStr)
                        .build();
            })
            .collect(Collectors.toList());

        ForeignMainSummary.AccountData data = ForeignMainSummary.AccountData.builder()
                .parentAccountNo(parent.getPano())
                .childAccounts(childSummaries)
                .build();

        return ForeignMainSummary.builder()
                .isLoggedIn(true)
                .hasProducts(true)
                .accountData(data)
                .build();
    }
}