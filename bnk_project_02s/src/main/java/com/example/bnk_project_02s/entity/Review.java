package com.example.bnk_project_02s.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bnk_review")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @Column(name = "rvno", length = 50, nullable = false)
    private String rvno;                 // PK: "1","2","3"...

    @Column(name = "uid", length = 50, nullable = false)
    private String uid;                  // 작성자 아이디

    @Column(name = "rvcontent", columnDefinition = "TEXT", nullable = false)
    private String rvcontent;            // 리뷰 내용

    @Column(name = "rvrating", length = 10, nullable = false)
    private String rvrating;             // "4" 또는 "4.5"

    @Column(name = "rvdate", length = 30)
    private String rvdate;               // "YYYY-MM-DD"
}
