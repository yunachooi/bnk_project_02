package com.example.bnk_project_02s.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bnk_user2")
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id 
    @Column(length = 50)
    private String uid;             // 사용자아이디 (PK)

    @Column(nullable = false, length = 60) // BCrypt hash 길이
    private String upw;             // 비밀번호(Hash)

    @Column(nullable = false, length = 30)
    private String uname;           // 이름

    @Column(nullable = false, length = 10)
    private String ugender;         // 성별 (M/F)

    @Column(nullable = false, length = 30)
    private String ubirth;          // 생년월일(YYYYMMDD)

    @Column(nullable = false, length = 30)
    private String uphone;          // 휴대번호

    @Column(nullable = false, length = 20)
    private String urole = "ROLE_USER";   // 디폴트

    @Column(length = 200)
    private String ucurrency;       // 관심통화 (예: USD)

    @Column(length = 200)
    private String uinterest;       // 관심분야

    @Column(length = 2)
    private String ucheck = "N";    // 상품가입여부 (디폴트 N)

    private Long   ushare = 0L;     // 공유횟수
}