package com.example.bnk_project_02s.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bnk_customer_rate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crno")
    private Long crno; // PK

    @Column(name = "cdate", nullable = false)
    private LocalDate cdate; // 기준일

    @Column(name = "ccode", length = 10, nullable = false)
    private String ccode; // 통화코드 (USD, JPY(100) 등)

    @Column(name = "cname", length = 30, nullable = false)
    private String cname; // 통화명 (미국 달러)

    @Column(name = "cvalue", precision = 10, scale = 4, nullable = false)
    private BigDecimal cvalue; // 기준 매매율 (deal_bas_r)

    @Column(name = "ctts", precision = 10, scale = 4, nullable = false)
    private BigDecimal ctts; // 현찰 살 때 환율 (tts)

    @Column(name = "cpref", precision = 5, scale = 2, nullable = false)
    private BigDecimal cpref; // 우대율 (%)

    @Column(name = "cfinal", precision = 10, scale = 4, nullable = false)
    private BigDecimal cfinal; // 고객적용환율

    @Column(name = "cfee", precision = 10, scale = 4, nullable = false)
    private BigDecimal cfee; // 수수료 (ctts - cvalue)
}
