package com.example.lecturesystem.modules.auth.support;

import java.util.Locale;
import java.util.Set;

public final class PasswordPolicyValidator {
    public static final String MESSAGE = "密码至少8位，且必须包含字母和数字，不能使用常见弱密码。";

    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "123456",
            "12345678",
            "abcdef",
            "password",
            "admin123",
            "111111",
            "000000"
    );

    private PasswordPolicyValidator() {
    }

    public static void validateOrThrow(String rawPassword) {
        String password = rawPassword == null ? null : rawPassword.trim();
        if (password == null || password.length() < 8 || !containsLetter(password) || !containsDigit(password)
                || WEAK_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(MESSAGE);
        }
    }

    private static boolean containsLetter(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (Character.isLetter(password.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDigit(String password) {
        for (int i = 0; i < password.length(); i++) {
            if (Character.isDigit(password.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
