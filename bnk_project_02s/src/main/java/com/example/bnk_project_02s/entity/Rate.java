package com.example.bnk_project_02s.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_rate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rate {

    @Id
    @Column(name = "rno")
    private Long rno; // 환율정보 고유번호 
    
    @Column(name = "rdate", nullable = false)
    private LocalDate rdate; // 기준일

    @Column(name = "rcurrencyno", length = 10)
    private String rcurrencyno; // 통화 코드 (예: USD)

    @Column(name = "rtoday", precision = 10, scale = 4)
    private BigDecimal rtoday; // 당일 환율

    @Column(name = "ryesterday", precision = 10, scale = 4)
    private BigDecimal ryesterday; // 전일 환율

    @Column(name = "r7ago", precision = 10, scale = 4)
    private BigDecimal r7ago; // 1주 전 환율

    @Column(name = "r1moago", precision = 10, scale = 4)
    private BigDecimal r1moago; // 1달 전 환율

    @Column(name = "rmax", precision = 10, scale = 4)
    private BigDecimal rmax; // 1개월 최고

    @Column(name = "rmin", precision = 10, scale = 4)
    private BigDecimal rmin; // 1개월 최저
}
