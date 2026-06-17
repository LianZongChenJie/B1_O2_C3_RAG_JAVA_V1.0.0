package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * LLM API 调试测试
 */
@SpringBootTest
public class LlmApiDebugTest {

    private static final Logger logger = LoggerFactory.getLogger(LlmApiDebugTest.class);

    @Autowired
    private RagConfig ragConfig;

    @Test
    @DisplayName("调试 LLM API 调用")
    void debugLlmApiCall() throws Exception {
        logger.info("\n========== LLM API 调试 ==========");
        logger.info("API URL: {}", ragConfig.getLlmApiUrl());
        logger.info("API Key: {}...", ragConfig.getLlmApiKey().substring(0, Math.min(10, ragConfig.getLlmApiKey().length())));
        logger.info("Model: {}", ragConfig.getLlmModelName());

        URL url = new URL(ragConfig.getLlmApiUrl() + "/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + ragConfig.getLlmApiKey());
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        // 使用正确的 JSON 格式
        String jsonBody = "{"
                + "\"model\": \"" + ragConfig.getLlmModelName() + "\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"请用一句话回答：1+1等于几？\"}],"
                + "\"temperature\": 0.7,"
                + "\"max_tokens\": 500"
                + "}";

        logger.info("请求体: {}", jsonBody);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        logger.info("响应码: {}", responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                logger.info("成功响应: {}", response.toString());
            }
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                logger.error("错误响应: {}", errorResponse.toString());
            }
        }
    }
}