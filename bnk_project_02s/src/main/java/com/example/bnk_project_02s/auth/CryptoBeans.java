package com.example.bnk_project_02s.auth;

import org.springframework.stereotype.Component;

/**
 * 스프링이 만든 암/복호화 유틸 빈을 정적으로 보관해서
 * JPA EntityListener(스프링 DI가 안됨)에서 접근 가능하게 한다.
 */
@Component
public class CryptoBeans {

    public static AesGcmUtil AES;
    public static HmacUtil HMAC;

    public CryptoBeans(AesGcmUtil aes, HmacUtil hmac) {
        // 애플리케이션 부팅 시 한 번 주입됨
        CryptoBeans.AES  = aes;
        CryptoBeans.HMAC = hmac;
    }
}