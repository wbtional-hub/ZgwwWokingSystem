package com.example.lecturesystem.modules.auth.support;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class Sm3PasswordCodec {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String encode(String rawPassword, String salt) {
        byte[] input = buildInput(rawPassword, salt);
        SM3Digest digest = new SM3Digest();
        digest.update(input, 0, input.length);
        byte[] output = new byte[digest.getDigestSize()];
        digest.doFinal(output, 0);
        return Hex.toHexString(output);
    }

    public boolean matches(String rawPassword, String salt, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return encode(rawPassword, salt).equalsIgnoreCase(storedHash.trim());
    }

    public String generateSalt() {
        byte[] salt = new byte[32];
        SECURE_RANDOM.nextBytes(salt);
        return Hex.toHexString(salt);
    }

    private byte[] buildInput(String rawPassword, String salt) {
        String passwordPart = rawPassword == null ? "" : rawPassword;
        String saltPart = salt == null ? "" : salt;
        return (passwordPart + saltPart).getBytes(StandardCharsets.UTF_8);
    }
}
