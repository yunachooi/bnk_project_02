package com.example.bnk_project_02s.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HMAC-SHA256 유틸.
 * - CryptoConfig에서 생성한 byte[] 키로 초기화합니다.
 * - hmacHex(String) → hex(소문자) 문자열 반환.
 */
public class HmacUtil {

    private final SecretKeySpec keySpec;

    /** ✅ 권장 생성자: raw 바이트 키 */
    public HmacUtil(byte[] rawKeyBytes) {
        if (rawKeyBytes == null || rawKeyBytes.length == 0) {
            throw new IllegalArgumentException("HMAC key must not be empty");
        }
        this.keySpec = new SecretKeySpec(rawKeyBytes, "HmacSHA256");
    }

    /** (옵션) 문자열 키도 지원하고 싶다면 유지/사용 */
    public HmacUtil(String hexOrTextKey) {
        if (hexOrTextKey == null || hexOrTextKey.isEmpty()) {
            throw new IllegalArgumentException("HMAC key must not be empty");
        }
        byte[] keyBytes;
        if (hexOrTextKey.matches("^[0-9a-fA-F]+$") && hexOrTextKey.length() % 2 == 0) {
            keyBytes = hexStringToBytes(hexOrTextKey);
        } else {
            keyBytes = hexOrTextKey.getBytes(StandardCharsets.UTF_8);
        }
        this.keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /** 입력 문자열에 대해 HMAC-SHA256 → hex(소문자) */
    public String hmacHex(String src) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] out = mac.doFinal(src.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 계산 실패", e);
        }
    }

    /* ===== helpers ===== */

    private static String bytesToHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static byte[] hexStringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}