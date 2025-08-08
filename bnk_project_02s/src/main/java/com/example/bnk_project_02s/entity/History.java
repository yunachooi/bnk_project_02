package com.example.bnk_project_02s.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class History {
	
	@Id
    @Column(name = "hno", length = 50)
    private Long hno; // 거래내역 고유번호

    @Column(name = "uid", length = 50)
    private String uid; // 사용자 ID (FK to bnk_user.uid)

    @Column(name = "paaccount", length = 50)
    private String paaccount; // 계좌번호 (부모 계좌)

    @Column(name = "caserial", length = 10)
    private String caserial; // 통화 코드 (예: USD)

    @Column(name = "hwithdraw", length = 20)
    private String hwithdraw; // 출금액

    @Column(name = "hdeposit", length = 20)
    private String hdeposit; // 입금액

    @Column(name = "hbalance", length = 20)
    private String hbalance; // 거래 후 잔액

    @Column(name = "hkrw", length = 20)
    private String hkrw; // 사용한 원화 금액 (ex. 환전 기준 원화)

    @Column(name = "hdate", length = 30)
    private String hdate; // 거래일시 (yyyy-MM-dd HH:mm:ss 형식)

}
