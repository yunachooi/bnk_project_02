package com.example.bnk_project_02s.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class CryptoConfig {

    private static final int AES_KEY_LEN  = 32; // AES-256 = 32 bytes
    private static final int HMAC_MIN_LEN = 32; // 256-bit 이상 권장

    /* ========= AES: 예전처럼 그대로 'aes' 빈을 제공합니다 ========= */
    @Bean
    public AesGcmUtil aes(
            @Value("${aes.key.256.base64:}") String aesB64,
            @Value("${aes.key.256.hex:}")     String aesHex,
            @Value("${aes.key.256:}")         String aesRaw
    ) {
        byte[] key = firstNonEmptyBytes(aesB64, aesHex, aesRaw, "AES");
        requireLen(key, AES_KEY_LEN, "AES");
        return new AesGcmUtil(key); // 기존과 동일: new AesGcmUtil(byte[] key)
    }

    /* ========= HMAC: 기존과 동일하게 'hmac' 빈을 제공합니다 ========= */
    @Bean
    public HmacUtil hmac(
            @Value("${hmac.secret.base64:}") String hmacB64,
            @Value("${hmac.secret.hex:}")     String hmacHex,
            @Value("${hmac.secret:}")         String hmacRaw
    ) {
        byte[] key = firstNonEmptyBytes(hmacB64, hmacHex, hmacRaw, "HMAC");
        requireAtLeast(key, HMAC_MIN_LEN, "HMAC");
        return new HmacUtil(key); // 내부는 raw 바이트 키 사용
    }

    /* ======================= helpers ======================= */
    private static byte[] firstNonEmptyBytes(String b64, String hex, String raw, String who) {
        if (b64 != null && !b64.isBlank()) {
            return Base64.getDecoder().decode(b64.trim());
        }
        if (hex != null && !hex.isBlank()) {
            return hexToBytes(hex.trim());
        }
        if (raw != null && !raw.isBlank()) {
            return raw.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException(who + " key missing");
    }

    private static void requireLen(byte[] key, int len, String name) {
        if (key == null || key.length != len) {
            throw new IllegalArgumentException(name + " key must be exactly " + len + " bytes");
        }
    }

    private static void requireAtLeast(byte[] key, int min, String name) {
        if (key == null || key.length < min) {
            throw new IllegalArgumentException(name + " key must be >= " + min + " bytes");
        }
    }

    private static byte[] hexToBytes(String s) {
        String x = s;
        if ((x.length() & 1) == 1) throw new IllegalArgumentException("hex odd length");
        byte[] out = new byte[x.length() / 2];
        for (int i = 0; i < x.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(x.substring(i, i + 2), 16);
        }
        return out;
    }
}