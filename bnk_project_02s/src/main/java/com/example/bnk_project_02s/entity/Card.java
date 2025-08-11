package com.example.bnk_project_02s.entity;

import java.time.LocalDateTime;
import java.util.Random;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
    @Column(name = "cano", length = 16)
    private String cano;

    @Column(name = "cacvc")
    private Integer cacvc;

    @Column(name = "caname")
    @Builder.Default
    private String caname = "BNK 쇼핑환전체크카드";

    @Column(name = "pano")
    private String pano;

    @Column(name = "cuno")
    private String cuno;

    @Column(name = "castatus", length = 1)
    @Builder.Default
    private String castatus = "Y";

    @CreationTimestamp
    @Column(name = "cadate")
    private LocalDateTime cadate;

    @PrePersist
    public void prePersist() {
        if (this.cano == null) {
            this.cano = generateCardNumber();
        }
        if (this.cacvc == null) {
            this.cacvc = generateCvc();
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