package com.example.bnk_project_02s.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bnk_rate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rno")
    private Long rno;

    @Column(name = "rdate", nullable = false)
    private LocalDate rdate; // 기준일

    @Column(name = "rcode", length = 10, nullable = false)
    private String rcode; // 통화코드 (예: USD)

    @Column(name = "rcurrency", length = 30, nullable = false)
    private String rcurrency; // 통화명 (예: 미국 달러)

    @Column(name = "rvalue", precision = 10, scale = 4, nullable = false)
    private BigDecimal rvalue; // 매매기준율
}