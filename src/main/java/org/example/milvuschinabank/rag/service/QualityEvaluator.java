package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 召回质量评估器
 * 评估当前召回结果的质量，决定是否需要继续召回
 */
@Service
public class QualityEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(QualityEvaluator.class);

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private LlmClient llmClient;

    /**
     * 评估召回质量
     * @param userQuery 用户查询
     * @param context 当前上下文
     * @param currentRound 当前轮次
     * @return 是否需要更多上下文
     */
    public boolean evaluateQuality(String userQuery, String context, int currentRound) {
        // 第一轮默认需要更多上下文
        if (currentRound == 1) {
            return true;
        }

        // 检查上下文长度
        if (context == null || context.isEmpty()) {
            return true;
        }

        // 使用 LLM 评估质量
        try {
            String prompt = buildQualityEvaluationPrompt(userQuery, context);
            String response = llmClient.generateCompletion(prompt);

            // 解析 LLM 响应
            boolean needMore = parseQualityResponse(response);

            logger.info("第 {} 轮质量评估结果: {}", currentRound,
                    needMore ? "需要更多上下文" : "质量已达标");

            return needMore;
        } catch (Exception e) {
            logger.error("质量评估失败，继续召回", e);
            return true; // 失败时默认继续召回
        }
    }

    /**
     * 构建质量评估提示词
     */
    private String buildQualityEvaluationPrompt(String userQuery, String context) {
        return String.format(
                "你是一个专业的信息检索质量评估助手。请评估当前检索到的上下文是否足以回答用户的问题。\n\n" +

                "## 用户问题\n" +
                "%s\n\n" +

                "## 当前检索到的上下文\n" +
                "%s\n\n" +

                "## 评估维度\n" +
                "1. 相关性：上下文是否与问题高度相关？\n" +
                "2. 完整性：上下文是否覆盖了问题的各个方面？\n" +
                "3. 准确性：上下文中的信息是否准确可靠？\n\n" +

                "## 输出格式\n" +
                "请输出 JSON 格式：\n" +
                "{\n" +
                "  \"relevance_score\": 0-100,\n" +
                "  \"completeness_score\": 0-100,\n" +
                "  \"accuracy_score\": 0-100,\n" +
                "  \"need_more_context\": true/false,\n" +
                "  \"reason\": \"简要说明原因\"\n" +
                "}\n\n" +

                "## 评估结果\n",
                userQuery,
                context.length() > 3000 ? context.substring(0, 3000) + "..." : context
        );
    }

    /**
     * 解析质量评估响应
     */
    private boolean parseQualityResponse(String response) {
        if (response == null || response.isEmpty()) {
            return true;
        }

        // 简单解析：检查是否包含 "need_more_context\": true"
        return response.contains("\"need_more_context\": true") ||
               response.contains("\"need_more_context\":true");
    }
}