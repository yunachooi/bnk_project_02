package com.example.bnk_project_02s.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bnk_user2")
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id 
    @Column(length = 50)
    private String uid;                    // 사용자아이디 (PK)

    @Column(nullable = false, length = 60) // BCrypt hash 길이
    private String upw;                    // 비밀번호(Hash)

    @Column(nullable = false, length = 30)
    private String uname;                  // 이름

    @Column(nullable = false, length = 1)  // 'M' 또는 'F'
    private String ugender;                // 성별 (주민번호로 유도)

    // 생년월일은 사용하지 않으면 nullable 허용(또는 필드 제거)
    @Column(nullable = true, length = 10)   // YYYYMMDD (옵션)
    private String ubirth;

    @Column(nullable = false, length = 13) // 010-1234-5678
    private String uphone;                  // 휴대번호(하이픈 포함)

    @Column(nullable = false, length = 20)
    private String urole = "ROLE_USER";     // 디폴트

    @Column(length = 200)
    private String ucurrency;               // 관심통화 CSV (예: USD,JPY,CNY)

    @Column(length = 200)
    private String uinterest;               // 관심분야 CSV (예: 여행,유학)

    @Column(length = 2)
    private String ucheck = "N";            // 상품가입여부 (디폴트 N)

    private Long ushare = 0L;               // 공유횟수

    @Column(length = 256)                   // AES 등으로 암호화한 주민번호 전체
    private String rrnEnc;                  // 주민등록번호 암호문
}