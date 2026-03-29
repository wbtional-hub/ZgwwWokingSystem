package com.example.lecturesystem.modules.aiconfig.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class AiTokenCipherSupport {
    private final String secret;

    public AiTokenCipherSupport(@Value("${app.ai.token-secret:codex-ai-access-secret}") String secret) {
        this.secret = secret;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, buildKey());
            byte[] encrypted = cipher.doFinal(plainText.trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI Token加密失败", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.trim().isEmpty()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, buildKey());
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI Token解密失败", ex);
        }
    }

    public String mask(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return "";
        }
        String trimmed = plainText.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    private SecretKeySpec buildKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        byte[] aesKey = new byte[16];
        System.arraycopy(keyBytes, 0, aesKey, 0, aesKey.length);
        return new SecretKeySpec(aesKey, "AES");
    }
}