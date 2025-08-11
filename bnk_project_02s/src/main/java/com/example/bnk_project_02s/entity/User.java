package com.example.bnk_project_02s.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "bnk_user2",
    indexes = {
        @Index(name = "ux_rrn_hmac",   columnList = "rrn_hmac",   unique = true),
        @Index(name = "ux_phone_hmac", columnList = "phone_hmac", unique = true) // 휴대폰 중복 차단
    }
)
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(length = 50)
    private String uid;                      // 사용자아이디 (PK)

    @Column(nullable = false, length = 60)   // BCrypt hash
    private String upw;                      // 비밀번호(Hash)

    @Column(nullable = false, length = 30)
    private String uname;                    // 이름

    @Column(nullable = false, length = 1)    // 'M'/'F'
    private String ugender;                  // 성별

    @Column(nullable = true, length = 10)    // YYYY-MM-DD
    private String ubirth;

    // ⚠️ 호환용: 평문 휴대폰. 더 이상 저장하지 않음(서비스에서 uphoneEnc/Hmac만 사용).
    //@Column(nullable = true, length = 13)    // 예: 010-1234-5678
    //private String uphone;

    @Column(nullable = false, length = 20)
    private String urole = "ROLE_USER";

    @Column(length = 200)
    private String ucurrency;                // 관심통화 CSV

    @Column(length = 200)
    private String uinterest;                // 관심분야 CSV

    @Column(length = 2)
    private String ucheck = "N";             // 상품가입여부

    @Column
    private Long ushare = 0L;                // 공유횟수
    
    @Column
    private String upush;
    
    @Column
    private String upushdate;
    
    @Column
    private String ulocation;

    /* ===== 주민등록번호 보호 ===== */
    @Column(name = "rrn_enc",  nullable = false, length = 512)
    private String urrnEnc;                  // AES-GCM "iv:ct" (Base64)

    @Column(name = "rrn_hmac", nullable = false, unique = true, length = 128)
    private String urrnHmac;                 // HMAC-SHA256 (hex)

    /* ===== 휴대번호 보호 ===== */
    @Column(name = "phone_enc",  nullable = true, length = 512)
    private String uphoneEnc;                // AES-GCM "iv:ct" (Base64)

    @Column(name = "phone_hmac", nullable = true, unique = true, length = 64)
    private String uphoneHmac;               // HMAC-SHA256 (hex, 64자)
}