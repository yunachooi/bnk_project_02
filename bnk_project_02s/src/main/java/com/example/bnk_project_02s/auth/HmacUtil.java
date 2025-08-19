package com.example.bnk_project_02s.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class HmacUtil {
    private final SecretKeySpec keySpec;

    /** ✅ 신규 권장: raw 바이트 키 */
    public HmacUtil(byte[] rawKeyBytes) {
        if (rawKeyBytes == null || rawKeyBytes.length == 0) {
            throw new IllegalArgumentException("HMAC key must not be empty");
        }
        this.keySpec = new SecretKeySpec(rawKeyBytes, "HmacSHA256");
    }

    /** ✅ 구버전 호환 유지: 문자열 키 생성자 (있던 곳 그대로 동작) */
    public HmacUtil(String keyText) {
        if (keyText == null || keyText.isEmpty()) {
            throw new IllegalArgumentException("HMAC key must not be empty");
        }
        this.keySpec = new SecretKeySpec(keyText.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** ✅ 신규 내부 표준: 소문자 hex */
    public String hmacHexLower(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHexLower(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC compute failed", e);
        }
    }

    /** ✅ 구버전 호환 alias: 예전에 쓰던 이름 유지 */
    public String hmacHex(String data) {
        return hmacHexLower(data);
    }

    // (구버전에서 쓰던 경우 대비) hextobytes 같은 유틸이 필요하면 아래 유지
    public static byte[] hexToBytes(String hex) {
        if (hex == null) return new byte[0];
        String s = hex.trim();
        if ((s.length() & 1) == 1) throw new IllegalArgumentException("odd length");
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return out;
    }

    private static String toHexLower(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        final char[] d = "0123456789abcdef".toCharArray();
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            hex[j++] = d[b >>> 4];
            hex[j++] = d[b & 0x0f];
        }
        return new String(hex);
    }
}