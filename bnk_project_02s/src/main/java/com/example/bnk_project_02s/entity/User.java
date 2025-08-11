package com.example.bnk_project_02s.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "bnk_user2",
    indexes = {
        @Index(name = "ux_rrn_hmac", columnList = "rrn_hmac", unique = true) // 중복 차단
    }
)
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

    @Column(nullable = true, length = 10)  // YYYYMMDD (옵션)
    private String ubirth;

    @Column(nullable = false, length = 13) // 예: 010-1234-5678
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

    /* ===== 주민등록번호 보호 ===== */
    @Column(name = "rrn_enc", nullable = false, length = 512)
    private String urrnEnc;                  // AES-GCM 암호문(Base64, IV 포함)

    @Column(name = "rrn_hmac", nullable = false, unique = true, length = 128)
    private String urrnHmac;                 // HMAC-SHA256(조회/중복 인덱스용)

}