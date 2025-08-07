package com.example.bnk_project_02s.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_shopping_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShoppingLog {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spno")
    private ShoppingProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uno")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pano")
    private User parentAccount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuno")
    private User currency;

    private Double slamount;
    private String slcurrency;
    private String slstatus;
    private String slmsg;

    @Column(name = "slreqat", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime slreqat;

    private LocalDateTime slcomat;
}
