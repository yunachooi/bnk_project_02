package com.example.bnk_project_02s.auth;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class AesGcmUtil {
    private static final String ALG = "AES";
    private static final String TRANS = "AES/GCM/NoPadding";
    private static final int NONCE_LEN = 12;   // 96-bit
    private static final int TAG_LEN_BIT = 128;

    private final SecretKeySpec key;
    private final SecureRandom rnd = new SecureRandom();

    public AesGcmUtil(byte[] keyBytes){
        if (keyBytes == null || !(keyBytes.length==16 || keyBytes.length==24 || keyBytes.length==32))
            throw new IllegalArgumentException("AES key must be 16/24/32 bytes");
        this.key = new SecretKeySpec(keyBytes, ALG);
    }

    /** 표준 Base64(패딩 포함) 문자열 반환 */
    public String encrypt(String plaintext){
        if (plaintext==null) return null;
        try{
            byte[] nonce = new byte[NONCE_LEN];
            rnd.nextBytes(nonce);

            Cipher c = Cipher.getInstance(TRANS);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BIT, nonce));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[NONCE_LEN + ct.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_LEN);
            System.arraycopy(ct,     0, out, NONCE_LEN, ct.length);

            return Base64.getEncoder().encodeToString(out);
        }catch(Exception e){
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    public String decrypt(String b64){
        if (b64==null) return null;
        try{
            byte[] in = Base64.getDecoder().decode(b64);
            if (in.length < NONCE_LEN + 16) throw new IllegalArgumentException("ciphertext too short");
            byte[] nonce = new byte[NONCE_LEN];
            byte[] ct    = new byte[in.length - NONCE_LEN];
            System.arraycopy(in, 0, nonce, 0, NONCE_LEN);
            System.arraycopy(in, NONCE_LEN, ct, 0, ct.length);

            Cipher c = Cipher.getInstance(TRANS);
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BIT, nonce));
            byte[] pt = c.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        }catch(Exception e){
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }
}