package com.example.bnk_project_02s.entity;

import java.math.BigDecimal;

import com.example.bnk_project_02s.util.KrwConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hno")
    private Long hno; // 거래내역코드

    /** 부모 계좌 (FK → bnk_parent_account.pano) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pano", referencedColumnName = "pano")
    private ParentAccount parentAccount;

    /** 통화 (FK → bnk_currency.cuno) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuno", referencedColumnName = "cuno")
    private Currency currency;

    /** 사용자 (FK → bnk_user.uid) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", referencedColumnName = "uid")
    private User user;

    /** 출금액 */
    @Column(name = "hwithdraw", length = 20)
    private String hwithdraw;

    /** 입금액 */
    @Column(name = "hdeposit", length = 20)
    private String hdeposit;

    /** 거래 후 잔액 */
    @Column(name = "hbalance", length = 20)
    private String hbalance;

    /** 사용 원화 금액 (KRW, 정수) */
    @Convert(converter = KrwConverter.class)
    @Column(name = "hkrw", length = 20, nullable = false)
    private BigDecimal hkrw = BigDecimal.ZERO;

    /** 거래일시 */
    @Column(name = "hdate", length = 30)
    private String hdate;
}
