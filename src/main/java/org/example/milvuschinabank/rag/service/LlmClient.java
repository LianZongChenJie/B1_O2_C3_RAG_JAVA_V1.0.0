package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 客户端（兼容 OpenAI 协议）
 * 用于调用中国银行内部大模型
 */
@Service
public class LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

    @Autowired
    private RagConfig ragConfig;

    /**
     * 生成补全响应
     * @param prompt 提示词
     * @return LLM 响应文本
     */
    public String generateCompletion(String prompt) {
        try {
            URL url = new URL(ragConfig.getLlmApiUrl() + "/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + ragConfig.getLlmApiKey());
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ragConfig.getLlmModelName());
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            requestBody.put("messages", messages.toArray(new Object[0]));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            String jsonBody = toJson(requestBody);

            // 发送请求
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    // 打印完整的 LLM 响应
                    logger.info("LLM API 完整响应: {}", response.toString());

                    // 解析响应
                    String parsedContent = parseCompletionResponse(response.toString());
                    logger.info("LLM 解析后的内容: {}", parsedContent);
                    return parsedContent;
                }
            } else {
                // 打印错误响应
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    logger.error("LLM API 调用失败，响应码: {}, 错误响应: {}", responseCode, errorResponse.toString());
                } catch (Exception e) {
                    logger.error("LLM API 调用失败，响应码: {}, 无法读取错误响应", responseCode);
                }
                return null;
            }
        } catch (Exception e) {
            logger.error("调用 LLM API 异常", e);
            return null;
        }
    }

    /**
     * 解析 LLM 响应
     */
    private String parseCompletionResponse(String jsonResponse) {
        try {
            // 简单 JSON 解析（实际应使用 Jackson/Gson）
            int contentStart = jsonResponse.indexOf("\"content\":\"");
            if (contentStart == -1) {
                return null;
            }

            contentStart += "\"content\":\"".length();
            int contentEnd = jsonResponse.indexOf("\"", contentStart);

            if (contentEnd == -1) {
                return null;
            }

            String content = jsonResponse.substring(contentStart, contentEnd);

            // 处理转义字符
            return content.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        } catch (Exception e) {
            logger.error("解析 LLM 响应失败", e);
            return null;
        }
    }

    /**
     * 简单 JSON 序列化（实际应使用 Jackson/Gson）
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }

            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Object[]) {
                json.append(serializeArray((Object[]) value));
            } else if (value instanceof Map) {
                json.append(serializeMap((Map<String, Object>) value));
            }

            first = false;
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 序列化数组
     */
    private String serializeArray(Object[] array) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) json.append(",");
            Object item = array[i];
            if (item instanceof Map) {
                json.append(serializeMap((Map<String, Object>) item));
            } else if (item instanceof String) {
                json.append("\"").append(escapeJson((String) item)).append("\"");
            } else {
                json.append(item);
            }
        }
        json.append("]");
        return json.toString();
    }

    /**
     * 序列化 Map
     */
    private String serializeMap(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }

            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Object[]) {
                json.append(serializeArray((Object[]) value));
            } else if (value instanceof Map) {
                json.append(serializeMap((Map<String, Object>) value));
            }

            first = false;
        }

        json.append("}");
        return json.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}