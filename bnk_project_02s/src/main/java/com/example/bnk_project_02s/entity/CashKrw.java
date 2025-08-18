package com.example.bnk_project_02s.entity;

import java.math.BigDecimal;

import com.example.bnk_project_02s.util.KrwConverter;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_cash")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashKrw {

    /** 환전코드 (PK, 자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cno")
    private Long cno;

    /** 유저아이디 (FK → bnk_user.uid) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", referencedColumnName = "uid", nullable = false, unique = true)
    private User user;

    /** 환전 횟수 */
    @Column(name = "ccount")
    private Integer ccount;

    /** 누적 환전 금액 (KRW, 정수) */
    @Convert(converter = KrwConverter.class)
    @Column(name = "cfxamount", length = 50, nullable = false)
    private BigDecimal cfxamount = BigDecimal.ZERO;
}
