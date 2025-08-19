package com.example.bnk_project_02s.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FinalIssueRequest {

    /** 상품 코드 (없으면 서버에서 1로 처리) */
    private Integer fno;

    /** 관리지점 */
    @NotBlank(message = "관리지점(pabank)은 필수입니다.")
    private String pabank;

    /** 통화코드 목록, 예: ["USD","JPY","EUR"] */
    @NotEmpty(message = "통화코드(currencies)는 1개 이상이어야 합니다.")
    private List<String> currencies;

    /** 카드명(선택) */
    private String cardName;
}
