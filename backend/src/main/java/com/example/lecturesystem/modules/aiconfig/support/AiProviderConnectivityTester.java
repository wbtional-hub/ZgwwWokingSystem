package com.example.lecturesystem.modules.aiconfig.support;

import com.example.lecturesystem.modules.aiconfig.vo.ProviderModelVO;
import com.example.lecturesystem.modules.aiconfig.vo.ProviderTestResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AiProviderConnectivityTester {
    private final ObjectMapper objectMapper;

    public AiProviderConnectivityTester(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProviderTestResultVO test(String apiBaseUrl, String apiToken) {
        ProviderTestResultVO result = new ProviderTestResultVO();
        if (apiBaseUrl == null || apiBaseUrl.trim().isEmpty()) {
            result.setConnectStatus("FAILED");
            result.setMessage("API Base URL不能为空");
            result.setModelCount(0);
            result.setModels(List.of());
            return result;
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            result.setConnectStatus("FAILED");
            result.setMessage("API Token不能为空");
            result.setModelCount(0);
            result.setModels(List.of());
            return result;
        }

        String endpoint = buildModelsEndpoint(apiBaseUrl);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                result.setConnectStatus("FAILED");
                result.setMessage("连接失败，HTTP状态码=" + response.statusCode());
                result.setModelCount(0);
                result.setModels(List.of());
                return result;
            }
            List<ProviderModelVO> models = parseModels(response.body());
            result.setConnectStatus("SUCCESS");
            result.setMessage(models.isEmpty() ? "连接成功，未返回模型列表" : "连接成功");
            result.setModelCount(models.size());
            result.setModels(models);
            return result;
        } catch (Exception ex) {
            result.setConnectStatus("FAILED");
            result.setMessage("连接失败：" + ex.getMessage());
            result.setModelCount(0);
            result.setModels(List.of());
            return result;
        }
    }

    private String buildModelsEndpoint(String apiBaseUrl) {
        String trimmed = apiBaseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/models")) {
            return trimmed;
        }
        return trimmed + "/models";
    }

    private List<ProviderModelVO> parseModels(String body) {
        List<ProviderModelVO> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    list.add(buildModel(item));
                }
                return list;
            }
            JsonNode modelsNode = root.get("models");
            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode item : modelsNode) {
                    list.add(buildModel(item));
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return list;
    }

    private ProviderModelVO buildModel(JsonNode item) {
        ProviderModelVO model = new ProviderModelVO();
        String modelCode = text(item, "id");
        if (modelCode == null) {
            modelCode = text(item, "model");
        }
        model.setModelCode(modelCode == null ? "unknown" : modelCode);
        model.setModelName(text(item, "name") != null ? text(item, "name") : model.getModelCode());
        model.setModelType(text(item, "object") != null ? text(item, "object") : "chat");
        model.setSupportKnowledge(Boolean.TRUE);
        model.setSupportSkillTrain(Boolean.TRUE);
        model.setSupportAgentChat(Boolean.TRUE);
        model.setSupportAnalysis(Boolean.TRUE);
        model.setStatus(1);
        return model;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}