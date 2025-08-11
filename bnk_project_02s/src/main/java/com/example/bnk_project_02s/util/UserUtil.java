package com.example.bnk_project_02s.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

@Component
public class UserUtil {

    /* ===================== Keys ===================== */

    @Value("${aes.key.256.base64}")
    private String aesKeyBase64; // AES 256 Base64

    @Value("${hmac.secret.base64}")
    private String hmacKeyBase64; // HMAC-SHA256 Base64

    private SecretKey aesKey;
    private SecretKey hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        byte[] aes = Base64.getDecoder().decode(aesKeyBase64);
        if (aes.length != 32) throw new IllegalStateException("AES 키 길이가 32바이트가 아닙니다.");
        this.aesKey = new SecretKeySpec(aes, "AES");

        byte[] hmac = Base64.getDecoder().decode(hmacKeyBase64);
        if (hmac.length < 32) throw new IllegalStateException("HMAC 키는 최소 32바이트를 권장합니다.");
        this.hmacKey = new SecretKeySpec(hmac, "HmacSHA256");
    }

    /* ===================== RRN Normalization ===================== */

    /** 입력 두 칸(앞6/뒤7) → 숫자만 13자리로 병합 */
    public String normalizeRrnParts(String front, String back) {
        String f = front == null ? "" : front.replaceAll("\\D", "");
        String b = back  == null ? "" : back.replaceAll("\\D", "");
        return f + b;
    }

    /** 문자열 하나 → 숫자만 남김 */
    public String normalizeRrn(String rrn) {
        return rrn == null ? "" : rrn.replaceAll("\\D", "");
    }

    /** 13자리 형식 체크 */
    public boolean isValidRrn13(String normalized) {
        return normalized != null && normalized.matches("^\\d{13}$");
    }

    /* ===================== AES-GCM ===================== */

    /**
     * 주민번호 암호화 (입력은 평문, 내부에서 정규화)
     * 저장형식: base64(iv) : base64(ciphertext)
     */
    public String encryptRrn(String rrnPlain) {
        if (rrnPlain == null || rrnPlain.isBlank()) return null;
        String normalized = normalizeRrn(rrnPlain);
        if (!isValidRrn13(normalized))
            throw new IllegalArgumentException("주민등록번호 형식(13자리)이 올바르지 않습니다.");

        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + ":" +
                   Base64.getEncoder().encodeToString(ct);
        } catch (Exception e) {
            throw new IllegalStateException("주민번호 암호화 실패", e);
        }
    }

    /**
     * 주민번호 복호화 → 13자리 숫자 문자열 반환
     * 입력형식: base64(iv) : base64(ciphertext)
     */
    public String decryptRrn(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            String[] parts = encrypted.split(":");
            if (parts.length != 2) throw new IllegalArgumentException("암호문 형식 오류(IV:CT)");

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ct = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            byte[] pt = cipher.doFinal(ct);

            String rrn = new String(pt, StandardCharsets.UTF_8);
            if (!isValidRrn13(rrn)) throw new IllegalStateException("복호화 결과 형식 오류");
            return rrn;
        } catch (Exception e) {
            throw new IllegalStateException("주민번호 복호화 실패", e);
        }
    }

    /* ===================== HMAC (중복/인덱스용) ===================== */

    /** 조회/중복검사용 HMAC(HEX 소문자) — 입력은 13자리 숫자 */
    public String hmacRrnHex(String normalizedRrn13) {
        if (!isValidRrn13(normalizedRrn13))
            throw new IllegalArgumentException("HMAC 계산 전 형식 오류(13자리 필요)");

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] out = mac.doFinal(normalizedRrn13.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("RRN HMAC 계산 실패", e);
        }
    }

    /* ===================== Masking/Helpers ===================== */

    /** 마스킹: 앞 6자리 + '-' + ******* */
    public String maskRrn(String normalizedRrn13) {
        if (!isValidRrn13(normalizedRrn13)) return "******-*******";
        return normalizedRrn13.substring(0, 6) + "-" + "*******";
    }
}