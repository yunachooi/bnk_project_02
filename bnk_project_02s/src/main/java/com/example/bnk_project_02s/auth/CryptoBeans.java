package com.example.bnk_project_02s.auth;

import org.springframework.beans.factory.annotation.Qualifier; // ← 추가
import org.springframework.stereotype.Component;

@Component
public class CryptoBeans {

    public static AesGcmUtil AES;
    public static HmacUtil HMAC;       // 일반 용도
    public static HmacUtil URL_HMAC;   // URL 서명 용도 ← 추가

    public CryptoBeans(
            AesGcmUtil aes,
            @Qualifier("hmac") HmacUtil hmac,
            @Qualifier("urlHmac") HmacUtil urlHmac
    ) {
        AES       = aes;
        HMAC      = hmac;
        URL_HMAC  = urlHmac;
    }
}