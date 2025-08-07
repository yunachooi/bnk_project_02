package com.example.bnk_project_02s.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_customer_rate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRate {

    @Id
    @Column(name = "crno")
    private Long crno; // 고객 적용 환율 고유번호 (예: 사용자ID+날짜+통화 등)

    @Column(name = "cbase", precision = 10, scale = 4)
    private BigDecimal cbase; // 기준 매매율

    @Column(name = "ctts", precision = 10, scale = 4)
    private BigDecimal ctts; // 현찰 살 때 환율

    @Column(name = "cfee", precision = 5, scale = 2)
    private BigDecimal cfee; // 수수료

    @Column(name = "cprefer", precision = 5, scale = 2)
    private BigDecimal cprefer; // 우대율

    @Column(name = "cfinal", precision = 10, scale = 4)
    private BigDecimal cfinal; // 최종 적용 환율
}
