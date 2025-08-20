package com.example.bnk_project_02s.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ForeignMainSummary {
    private boolean isLoggedIn;
    private boolean hasProducts;
    private AccountData accountData; // null 가능

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AccountData {
        private String parentAccountNo; // pano
        private List<ChildAccountSummary> childAccounts; // 통화별 요약
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChildAccountSummary {
        private String currencyCode;       // 예: USD
        private String currencyFullName;   // 예: 미국 달러 (없으면 코드로 대체)
        private String balance;            // 포맷팅된 문자열 "1,234.56"
    }
}