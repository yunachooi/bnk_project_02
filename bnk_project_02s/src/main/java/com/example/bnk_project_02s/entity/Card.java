package com.example.bnk_project_02s.entity;

import java.time.LocalDateTime;
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
@Table(name = "bnk_card")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @Column(name = "cardno", length = 16)
    private String cardno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cano", referencedColumnName = "cano")
    private ChildAccount childAccount;

    @Column(name = "cardcvc")
    private Integer cardcvc;

    @Column(name = "cardname")
    @Builder.Default
    private String cardname = "BNK 쇼핑환전체크카드";

    @Column(name = "pano")
    private String pano;

    @Column(name = "cuno")
    private String cuno;

    @Column(name = "cardstatus", length = 1)
    @Builder.Default
    private String cardstatus = "Y";

    @CreationTimestamp
    @Column(name = "carddate")
    private LocalDateTime carddate;

    @PrePersist
    public void prePersist() {
        if (this.cardno == null) {
            this.cardno = generateCardNumber();
        }
        if (this.cardcvc == null) {
            this.cardcvc = generateCvc();
        }
    }

    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("4000");
        for (int i = 0; i < 12; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private Integer generateCvc() {
        Random random = new Random();
        return 100 + random.nextInt(900);
    }
}