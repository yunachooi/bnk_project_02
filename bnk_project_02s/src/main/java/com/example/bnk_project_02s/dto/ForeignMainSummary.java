package com.example.bnk_project_02s.dto;

import lombok.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ForeignMainSummary {

    @JsonProperty("isLoggedIn")
    private boolean isLoggedIn;

    @JsonProperty("hasProducts")
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
        private String balance;            // "1,234.56"
    }
}
