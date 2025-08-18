package com.example.bnk_project_02s.auth;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    private static final int AES_KEY_LEN = 32;   // AES-256 = 32 bytes
    private static final int HMAC_MIN_LEN = 32;  // HMAC은 256bit 이상 권장

    @Bean
    public AesGcmUtil aes(@Value("${aes.key.256.base64}") String aesKey) {
        byte[] key = decodeKeyFlexible(aesKey);
        requireLen(key, AES_KEY_LEN, "AES");
        return new AesGcmUtil(key);
    }

    @Bean
    public HmacUtil hmac(@Value("${hmac.secret.base64}") String hmacKey) {
        byte[] key = decodeKeyFlexible(hmacKey);
        requireAtLeast(key, HMAC_MIN_LEN, "HMAC");
        return new HmacUtil(key); // byte[] 생성자 사용
    }
    
    @Bean
    @Qualifier("urlHmac")
    public HmacUtil urlHmac(@Value("${hmac.url.secret.base64:${hmac.secret.base64}}") String keyB64) {
        byte[] key = decodeKeyFlexible(keyB64); // 기존 CryptoConfig의 유틸 메서드 재사용
        requireAtLeast(key, 32, "HMAC");
        return new HmacUtil(key);
    }

    /** Base64 → 실패 시 HEX(공백/0x 허용) 파싱 */
    private static byte[] decodeKeyFlexible(String in) {
        String s = (in == null) ? "" : in.trim();
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException ignore) {
            String hex = s.replaceAll("\\s+", "")
                          .replaceFirst("^0x", "")
                          .toLowerCase();
            if (hex.isEmpty() || hex.length() % 2 != 0) {
                throw new IllegalArgumentException("Invalid key: not Base64 and odd-length hex");
            }
            byte[] out = new byte[hex.length() / 2];
            for (int i = 0; i < hex.length(); i += 2) {
                out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }
            return out;
        }
    }

    private static void requireLen(byte[] key, int expected, String label) {
        if (key == null || key.length != expected) {
            throw new IllegalArgumentException(label + " key must be " + expected
                    + " bytes, but was " + (key == null ? 0 : key.length));
        }
    }

    private static void requireAtLeast(byte[] key, int min, String label) {
        if (key == null || key.length < min) {
            throw new IllegalArgumentException(label + " key must be >= " + min
                    + " bytes, but was " + (key == null ? 0 : key.length));
        }
    }
}