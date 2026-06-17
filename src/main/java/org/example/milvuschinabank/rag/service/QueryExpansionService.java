package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 查询扩写服务
 * 将用户自然语言问题扩写为多个不同角度的查询语句
 */
@Service
public class QueryExpansionService {

    private static final Logger logger = LoggerFactory.getLogger(QueryExpansionService.class);

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private PromptOptimizer promptOptimizer;

    /**
     * 扩写用户查询
     * @param originalQuery 原始查询
     * @return 扩写后的查询列表
     */
    public List<String> expandQuery(String originalQuery) {
        logger.info("开始扩写查询: {}", originalQuery);

        String prompt = promptOptimizer.buildQueryExpansionPrompt(originalQuery);

        try {
            String response = llmClient.generateCompletion(prompt);

            if (response != null && !response.isEmpty()) {
                List<String> expandedQueries = parseExpandedQueries(response);
                logger.info("扩写完成，生成 {} 个查询", expandedQueries.size());
                return expandedQueries;
            }
        } catch (Exception e) {
            logger.error("查询扩写失败，使用原始查询", e);
        }

        // 失败时返回原始查询
        return Arrays.asList(originalQuery);
    }

    /**
     * 解析扩写结果
     */
    private List<String> parseExpandedQueries(String response) {
        // 按 | 分隔
        String[] queries = response.split("\\|");

        return Arrays.stream(queries)
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 基于规则的简单扩写（不依赖 LLM）
     * @param originalQuery 原始查询
     * @return 扩写后的查询列表
     */
    public List<String> expandQueryByRules(String originalQuery) {
        List<String> expandedQueries = new java.util.ArrayList<>();

        // 1. 原始查询
        expandedQueries.add(originalQuery);

        // 2. 添加同义词/近义词
        String synonymQuery = addSynonyms(originalQuery);
        if (!synonymQuery.equals(originalQuery)) {
            expandedQueries.add(synonymQuery);
        }

        // 3. 添加业务术语
        String businessQuery = addBusinessTerms(originalQuery);
        if (!businessQuery.equals(originalQuery)) {
            expandedQueries.add(businessQuery);
        }

        // 4. 简化查询
        String simplifiedQuery = simplifyQuery(originalQuery);
        if (!simplifiedQuery.equals(originalQuery)) {
            expandedQueries.add(simplifiedQuery);
        }

        return expandedQueries;
    }

    /**
     * 添加同义词
     */
    private String addSynonyms(String query) {
        return query.replace("外汇", "外币")
                .replace("汇率", "汇价")
                .replace("结汇", "外汇结算")
                .replace("购汇", "购买外汇");
    }

    /**
     * 添加业务术语
     */
    private String addBusinessTerms(String query) {
        if (query.contains("外汇")) {
            return query + " 国际结算 贸易融资";
        }
        if (query.contains("汇率")) {
            return query + " 中间价 买入价 卖出价";
        }
        return query;
    }

    /**
     * 简化查询
     */
    private String simplifyQuery(String query) {
        // 去除修饰词，保留核心关键词
        return query.replaceAll("(请问|我想了解|帮我查一下|能不能告诉我)", "")
                .trim();
    }
}