package com.example.bnk_project_02s.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateChildAccountReq {
    private String cuno;
    private java.math.BigDecimal amount;
    private String uid;
}