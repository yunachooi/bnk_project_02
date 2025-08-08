package com.example.bnk_project_02s.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class UserUtil {

    @Value("${aes.key.256.base64}")
    private String aesKeyBase64; // properties에서 읽어온 Base64 키

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("AES 키 길이가 32바이트가 아닙니다.");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    // ✅ 주민번호 암호화
    public String encryptRrn(String rrnPlain) {
        if (rrnPlain == null || rrnPlain.isBlank()) return null;
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));

            byte[] cipherText = cipher.doFinal(rrnPlain.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + ":" +
                   Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("주민번호 암호화 실패", e);
        }
    }

    // ✅ 주민번호 복호화
    public String decryptRrn(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            String[] parts = encrypted.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("암호화된 주민번호 형식이 잘못되었습니다.");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));

            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("주민번호 복호화 실패", e);
        }
    }
}