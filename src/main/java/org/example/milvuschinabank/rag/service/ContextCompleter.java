package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 上下文补全器
 * 调用 LLM 生成补全查询，优化召回效果
 */
@Service
public class ContextCompleter {

    private static final Logger logger = LoggerFactory.getLogger(ContextCompleter.class);

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private LlmClient llmClient;

    /**
     * 生成补全查询
     * @param originalQuery 原始查询
     * @param currentContext 当前上下文
     * @param round 当前轮次
     * @return 补全后的查询
     */
    public String generateRefinedQuery(String originalQuery, String currentContext, int round) {
        logger.info("生成第 {} 轮补全查询", round);

        String prompt = buildRefinementPrompt(originalQuery, currentContext, round);

        try {
            String refinedQuery = llmClient.generateCompletion(prompt);
            logger.info("补全查询结果: {}", refinedQuery);
            return refinedQuery;
        } catch (Exception e) {
            logger.error("生成补全查询失败，使用原始查询", e);
            return originalQuery;
        }
    }

    /**
     * 构建补全提示词
     */
    private String buildRefinementPrompt(String originalQuery, String currentContext, int round) {
        return String.format(
                "你是一个专业的信息检索助手。用户提出了一个问题，你已经检索到了一些相关上下文，\n" +
                "但可能还不够完整。请根据已有上下文，生成一个更精确的补充查询，以获取缺失的关键信息。\n\n" +

                "## 原始问题\n" +
                "%s\n\n" +

                "## 已检索到的上下文\n" +
                "%s\n\n" +

                "## 当前轮次\n" +
                "第 %d 轮\n\n" +

                "## 任务\n" +
                "1. 分析已有上下文还缺少哪些关键信息\n" +
                "2. 生成一个简短的补充查询（不超过50字）\n" +
                "3. 只输出补充查询，不要输出其他内容\n\n" +

                "## 补充查询\n",
                originalQuery,
                currentContext != null && currentContext.length() > 1000 ?
                        currentContext.substring(0, 1000) + "..." : currentContext,
                round
        );
    }

    /**
     * 评估上下文是否足够回答问题
     * @param userQuery 用户查询
     * @param context 当前上下文
     * @return 是否足够
     */
    public boolean isContextSufficient(String userQuery, String context) {
        String prompt = buildSufficiencyPrompt(userQuery, context);

        try {
            String response = llmClient.generateCompletion(prompt);
            return response != null && response.trim().toLowerCase().contains("是");
        } catch (Exception e) {
            logger.error("评估上下文充分性失败", e);
            return false; // 失败时默认需要更多上下文
        }
    }

    /**
     * 构建充分性评估提示词
     */
    private String buildSufficiencyPrompt(String userQuery, String context) {
        return String.format(
                "请判断以下上下文是否足以回答用户的问题。\n\n" +

                "## 用户问题\n" +
                "%s\n\n" +

                "## 上下文\n" +
                "%s\n\n" +

                "## 判断标准\n" +
                "1. 上下文是否包含问题涉及的核心概念？\n" +
                "2. 上下文是否提供了足够的细节来回答问题？\n" +
                "3. 上下文是否存在明显的信息缺失？\n\n" +

                "## 输出\n" +
                "只输出'是'或'否'，不要输出其他内容。\n\n" +

                "## 判断结果\n",
                userQuery,
                context != null && context.length() > 2000 ?
                        context.substring(0, 2000) + "..." : context
        );
    }
}