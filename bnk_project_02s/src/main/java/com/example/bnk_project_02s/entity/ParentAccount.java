package com.example.bnk_project_02s.entity;

import java.time.LocalDate;
import java.util.Random;

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
@Table(name = "bnk_parent_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentAccount {

    @Id
    @Column(name = "pano", length = 12)
    private String pano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", referencedColumnName = "uid")
    private User user;

    @Column(name = "fno")
    @Builder.Default
    private Integer fno = 1;

    @CreationTimestamp
    @Column(name = "pajoin")
    private LocalDate pajoin;

    @Column(name = "pabank")
    private String pabank;

    @PrePersist
    public void prePersist() {
        if (this.pano == null) {
            this.pano = generateAccountNumber();
        }
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}