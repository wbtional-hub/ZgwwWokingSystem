package com.example.lecturesystem.modules.agent.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleChatClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleChatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public String chat(String apiBaseUrl, String apiToken, String modelCode, String systemPrompt, String userPrompt) {
        return chatForUsage(apiBaseUrl, apiToken, modelCode, systemPrompt, userPrompt).getContent();
    }

    public ChatResult chatForUsage(String apiBaseUrl,
                                   String apiToken,
                                   String modelCode,
                                   String systemPrompt,
                                   String userPrompt) {
        validateArgs(apiBaseUrl, apiToken, modelCode);
        try {
            String endpoint = buildChatEndpoint(apiBaseUrl);
            String requestBody = buildRequestBody(modelCode, systemPrompt, userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            long durationMs = System.currentTimeMillis() - start;

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("AI调用失败，HTTP状态码=" + response.statusCode());
            }

            return parseChatResult(response.body(), durationMs);
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI调用失败：" + ex.getMessage(), ex);
        }
    }

    public boolean supportsNativeStream(String apiBaseUrl) {
        return apiBaseUrl != null && !apiBaseUrl.trim().isEmpty();
    }

    public ChatResult streamChat(String apiBaseUrl,
                                 String apiToken,
                                 String modelCode,
                                 String systemPrompt,
                                 String userPrompt,
                                 StreamDeltaHandler deltaHandler) {
        validateArgs(apiBaseUrl, apiToken, modelCode);
        try {
            String endpoint = buildChatEndpoint(apiBaseUrl);
            String requestBody = buildRequestBody(modelCode, systemPrompt, userPrompt, true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("AI流式调用失败，HTTP状态码=" + response.statusCode());
            }

            return parseStreamChatResult(response.body(), start, deltaHandler);
        } catch (Exception ex) {
            throw new IllegalArgumentException("AI流式调用失败：" + ex.getMessage(), ex);
        }
    }

    public long getConnectTimeoutMillis() {
        return CONNECT_TIMEOUT.toMillis();
    }

    public long getRequestTimeoutMillis() {
        return REQUEST_TIMEOUT.toMillis();
    }

    private void validateArgs(String apiBaseUrl, String apiToken, String modelCode) {
        if (apiBaseUrl == null || apiBaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("AI接入地址不能为空");
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            throw new IllegalArgumentException("AI Token不能为空");
        }
        if (modelCode == null || modelCode.trim().isEmpty()) {
            throw new IllegalArgumentException("模型不能为空");
        }
    }

    private String buildRequestBody(String modelCode, String systemPrompt, String userPrompt) throws Exception {
        return buildRequestBody(modelCode, systemPrompt, userPrompt, false);
    }

    private String buildRequestBody(String modelCode,
                                    String systemPrompt,
                                    String userPrompt,
                                    boolean stream) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelCode);
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
                Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)
        ));
        if (stream) {
            payload.put("stream", true);
            payload.put("stream_options", Map.of("include_usage", true));
        }
        return objectMapper.writeValueAsString(payload);
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

    private ChatResult parseChatResult(String body, long durationMs) throws Exception {
        JsonNode root = objectMapper.readTree(body);

        String content = extractContent(root, body);

        JsonNode usageNode = root.get("usage");
        boolean hasUsage = usageNode != null && !usageNode.isNull();

        int promptTokens = hasUsage && usageNode.get("prompt_tokens") != null
                ? usageNode.get("prompt_tokens").asInt(0)
                : 0;
        int completionTokens = hasUsage && usageNode.get("completion_tokens") != null
                ? usageNode.get("completion_tokens").asInt(0)
                : 0;
        int totalTokens = hasUsage && usageNode.get("total_tokens") != null
                ? usageNode.get("total_tokens").asInt(0)
                : 0;

        String usageSource = hasUsage ? "PROVIDER_USAGE" : "PROVIDER_NO_USAGE";

        return new ChatResult(
                content,
                promptTokens,
                completionTokens,
                totalTokens,
                durationMs,
                usageSource
        );
    }

    private ChatResult parseStreamChatResult(InputStream body,
                                             long startTimeMillis,
                                             StreamDeltaHandler deltaHandler) throws Exception {
        StringBuilder content = new StringBuilder();
        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;
        boolean hasUsage = false;
        boolean sawStreamFrame = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(":")) {
                    continue;
                }
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                sawStreamFrame = true;
                String data = trimmed.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(data);
                String deltaContent = extractStreamDeltaContent(root);
                if (deltaContent != null && !deltaContent.isEmpty()) {
                    content.append(deltaContent);
                    if (deltaHandler != null) {
                        deltaHandler.onDelta(deltaContent);
                    }
                }
                JsonNode usageNode = root.get("usage");
                if (usageNode != null && !usageNode.isNull()) {
                    hasUsage = true;
                    promptTokens = usageNode.get("prompt_tokens") == null ? promptTokens : usageNode.get("prompt_tokens").asInt(promptTokens);
                    completionTokens = usageNode.get("completion_tokens") == null ? completionTokens : usageNode.get("completion_tokens").asInt(completionTokens);
                    totalTokens = usageNode.get("total_tokens") == null ? totalTokens : usageNode.get("total_tokens").asInt(totalTokens);
                }
            }
        }

        if (!sawStreamFrame) {
            throw new IllegalArgumentException("provider 流式响应格式不支持");
        }

        long durationMs = System.currentTimeMillis() - startTimeMillis;
        return new ChatResult(
                content.toString(),
                promptTokens,
                completionTokens,
                totalTokens,
                durationMs,
                hasUsage ? "PROVIDER_STREAM_USAGE" : "PROVIDER_STREAM_NO_USAGE"
        );
    }

    private String extractContent(JsonNode root, String rawBody) {
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            JsonNode first = choices.get(0);
            if (first != null) {
                JsonNode message = first.get("message");
                if (message != null && message.get("content") != null) {
                    return message.get("content").asText();
                }
            }
        }
        return rawBody;
    }

    private String extractStreamDeltaContent(JsonNode root) {
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode first = choices.get(0);
        if (first == null) {
            return null;
        }
        JsonNode delta = first.get("delta");
        if (delta != null && delta.get("content") != null && !delta.get("content").isNull()) {
            return delta.get("content").asText();
        }
        JsonNode message = first.get("message");
        if (message != null && message.get("content") != null && !message.get("content").isNull()) {
            return message.get("content").asText();
        }
        return null;
    }

    @FunctionalInterface
    public interface StreamDeltaHandler {
        void onDelta(String delta) throws Exception;
    }

    public static class ChatResult {
        private final String content;
        private final Integer promptTokens;
        private final Integer completionTokens;
        private final Integer totalTokens;
        private final Long durationMs;
        private final String usageSource;

        public ChatResult(String content,
                          Integer promptTokens,
                          Integer completionTokens,
                          Integer totalTokens,
                          Long durationMs,
                          String usageSource) {
            this.content = content;
            this.promptTokens = promptTokens == null ? 0 : promptTokens;
            this.completionTokens = completionTokens == null ? 0 : completionTokens;
            this.totalTokens = totalTokens == null ? 0 : totalTokens;
            this.durationMs = durationMs == null ? 0L : durationMs;
            this.usageSource = usageSource;
        }

        public String getContent() {
            return content;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public Long getDurationMs() {
            return durationMs;
        }

        public String getUsageSource() {
            return usageSource;
        }
    }
}
