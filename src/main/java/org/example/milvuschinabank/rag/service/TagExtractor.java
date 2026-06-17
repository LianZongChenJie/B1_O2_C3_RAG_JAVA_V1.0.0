package org.example.milvuschinabank.rag.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标签提取服务
 * 使用规则和小模型提取业务/主题/实体标签
 */
@Service
public class TagExtractor {

    // 银行业务关键词
    private static final List<String> BUSINESS_KEYWORDS = Arrays.asList(
            "外汇", "汇率", "结汇", "购汇", "跨境", "结算", "信用证",
            "贸易融资", "国际结算", "外币", "USD", "EUR", "GBP", "JPY",
            "存款", "贷款", "利率", "理财", "投资", "基金", "保险",
            "信用卡", "借记卡", "转账", "汇款", "SWIFT", "清算"
    );

    // 实体提取正则
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");
    private static final Pattern RATE_PATTERN = Pattern.compile("\\d+\\.?\\d*%");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}");

    /**
     * 提取切片标签
     * @param content 切片内容
     * @param docId 文档ID
     * @param pos 位置
     * @return 标签列表
     */
    public List<String> extractTags(String content, String docId, int pos) {
        List<String> tags = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return tags;
        }

        // 1. 提取业务标签
        tags.addAll(extractBusinessTags(content));

        // 2. 提取实体标签
        tags.addAll(extractEntityTags(content));

        // 3. 提取主题标签
        tags.addAll(extractTopicTags(content));

        return tags;
    }

    /**
     * 提取业务标签
     */
    private List<String> extractBusinessTags(String content) {
        List<String> businessTags = new ArrayList<>();

        for (String keyword : BUSINESS_KEYWORDS) {
            if (content.contains(keyword)) {
                businessTags.add("biz:" + keyword);
            }
        }

        return businessTags;
    }

    /**
     * 提取实体标签
     */
    private List<String> extractEntityTags(String content) {
        List<String> entityTags = new ArrayList<>();

        // 提取货币代码
        Matcher currencyMatcher = CURRENCY_PATTERN.matcher(content);
        while (currencyMatcher.find()) {
            entityTags.add("entity:currency:" + currencyMatcher.group());
        }

        // 提取利率
        Matcher rateMatcher = RATE_PATTERN.matcher(content);
        while (rateMatcher.find()) {
            entityTags.add("entity:rate:" + rateMatcher.group());
        }

        // 提取日期
        Matcher dateMatcher = DATE_PATTERN.matcher(content);
        while (dateMatcher.find()) {
            entityTags.add("entity:date:" + dateMatcher.group());
        }

        return entityTags;
    }

    /**
     * 提取主题标签
     */
    private List<String> extractTopicTags(String content) {
        List<String> topicTags = new ArrayList<>();

        // 基于内容长度判断（长文本可能包含多个主题）
        if (content.length() > 500) {
            topicTags.add("topic:long_text");
        }

        // 检测是否包含数字（可能是数据/报告类）
        if (content.matches(".*\\d+.*")) {
            topicTags.add("topic:data");
        }

        // 检测是否包含问句（可能是FAQ类）
        if (content.contains("？") || content.contains("?")) {
            topicTags.add("topic:faq");
        }

        // 检测是否包含标题特征
        if (content.matches("^[第\\d]+[章节].*") || content.matches("^\\d+[、.].*")) {
            topicTags.add("topic:chapter");
        }

        return topicTags;
    }
}