package com.example.bnk_project_02s.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "bnk_push",
    indexes = {
        @Index(name = "idx_bnk_push_uid_created_at", columnList = "uid, created_at")
    }
)
@Getter @Setter
public class BnkPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="uid", nullable=false, length=50)
    private String uid;

    @Column(name="kind", nullable=false, length=50)   // 예: PRODUCT_RECO
    private String kind;

    @Column(name="title", nullable=false, length=200)
    private String title;

    @Column(name="body", nullable=false, length=1000)
    private String body;

    @Lob
    @Column(name="data_json")
    private String dataJson;                          // {"deeplink":"app://products/recommend"}

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name="consumed", nullable=false)
    private boolean consumed = false;

    @Column(name="consumed_at")
    private LocalDateTime consumedAt;
}
