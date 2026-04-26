package com.example.lecturesystem.modules.agent.support.policy;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AiPolicyQuestionNormalizer {
    private static final Pattern NON_TEXT = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final List<String> FILLER_TERMS = List.of(
            "请问", "想问", "请帮我", "告诉我", "麻烦", "请告诉我", "我想了解", "我想咨询", "我要咨询"
    );

    public NormalizedQuestion normalize(String question) {
        String original = trimToNull(question);
        if (original == null) {
            return null;
        }
        String cleaned = original;
        for (String filler : FILLER_TERMS) {
            cleaned = cleaned.replace(filler, " ");
        }
        cleaned = MULTI_SPACE.matcher(NON_TEXT.matcher(cleaned).replaceAll(" ")).replaceAll(" ").trim();
        String normalized = trimToNull(cleaned);
        if (normalized == null) {
            normalized = original.trim();
        }
        String compact = normalized.replace(" ", "");
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String token : normalized.split(" ")) {
            String value = trimToNull(token);
            if (value != null && value.length() > 1) {
                terms.add(value);
            }
        }
        if (!compact.isBlank()) {
            terms.add(compact);
        }
        return new NormalizedQuestion(original, normalized, compact, new ArrayList<>(terms));
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    public record NormalizedQuestion(String original, String normalized, String compact, List<String> terms) {
    }
}
