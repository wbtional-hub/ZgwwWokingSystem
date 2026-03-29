package com.example.lecturesystem.modules.agent.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleChatClient {
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleChatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String chat(String apiBaseUrl, String apiToken, String modelCode, String systemPrompt, String userPrompt) {
        if (apiBaseUrl == null || apiBaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("AI接入地址不能为空");
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            throw new IllegalArgumentException("AI Token不能为空");
        }
        if (modelCode == null || modelCode.trim().isEmpty()) {
            throw new IllegalArgumentException("模型不能为空");
        }
        try {
            String endpoint = buildChatEndpoint(apiBaseUrl);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", modelCode,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                            Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)
                    )
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("AI调用失败，HTTP状态码=" + response.statusCode());
            }
            return parseContent(response.body());
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI调用失败：" + ex.getMessage(), ex);
        }
    }

    private String buildChatEndpoint(String apiBaseUrl) {
        String trimmed = apiBaseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        return trimmed + "/chat/completions";
    }

    private String parseContent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).get("message");
            if (message != null && message.get("content") != null) {
                return message.get("content").asText();
            }
        }
        return body;
    }
}