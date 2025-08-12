package com.example.bnk_project_02s.entity;

import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_child_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildAccount {

    @Id
    @Column(name = "cano")
    private String cano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pano", referencedColumnName = "pano")
    private ParentAccount parentAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuno", referencedColumnName = "cuno")
    private Currency currency;

    @CreationTimestamp
    @Column(name = "cajoin")
    private LocalDate cajoin;

    @Column(name = "cabalance")
    private String cabalance;

    @Column(name = "pabank")
    private String pabank;

    @PrePersist
    public void prePersist() {
        if (this.cano == null && this.parentAccount != null && this.currency != null) {
            this.cano = generateChildAccountNumber();
        }
    }

    private String generateChildAccountNumber() {
        return parentAccount.getPano() + currency.getCuno();
    }
}