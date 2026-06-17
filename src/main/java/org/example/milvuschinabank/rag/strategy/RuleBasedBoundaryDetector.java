package org.example.milvuschinabank.rag.strategy;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于规则的语义割裂检测器
 * 使用文本特征和关键词重叠计算语义连贯度
 */
@Component
public class RuleBasedBoundaryDetector implements SemanticBoundaryDetector {

    /**
     * 计算两个文本片段之间的语义连贯度
     * 综合使用多种规则：
     * 1. 关键词重叠度
     * 2. 句子结构相似度
     * 3. 段落连续性
     */
    @Override
    public double calculateCohesion(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        // 规则1：关键词重叠度（权重 0.4）
        double keywordOverlap = calculateKeywordOverlap(text1, text2);

        // 规则2：字符级 n-gram 相似度（权重 0.3）
        double ngramSimilarity = calculateNgramSimilarity(text1, text2, 3);

        // 规则3：句子结构相似度（权重 0.3）
        double structureSimilarity = calculateStructureSimilarity(text1, text2);

        // 加权平均
        return 0.4 * keywordOverlap + 0.3 * ngramSimilarity + 0.3 * structureSimilarity;
    }

    /**
     * 计算关键词重叠度
     */
    private double calculateKeywordOverlap(String text1, String text2) {
        Set<String> keywords1 = extractKeywords(text1);
        Set<String> keywords2 = extractKeywords(text2);

        if (keywords1.isEmpty() && keywords2.isEmpty()) {
            return 1.0;
        }

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        // Jaccard 相似度
        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 提取关键词（简单实现：分词后过滤停用词）
     */
    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();

        // 简单按字符分词（实际应使用专业分词工具）
        // 提取长度 >= 2 的连续中文字符或英文单词
        String[] words = text.split("[\\s\\p{Punct}]+");

        for (String word : words) {
            if (word.length() >= 2) {
                keywords.add(word.toLowerCase());
            }
        }

        return keywords;
    }

    /**
     * 计算字符级 n-gram 相似度
     */
    private double calculateNgramSimilarity(String text1, String text2, int n) {
        Set<String> ngrams1 = generateNgrams(text1, n);
        Set<String> ngrams2 = generateNgrams(text2, n);

        if (ngrams1.isEmpty() && ngrams2.isEmpty()) {
            return 1.0;
        }

        if (ngrams1.isEmpty() || ngrams2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 生成 n-gram（优化版：使用采样减少计算量）
     */
    private Set<String> generateNgrams(String text, int n) {
        Set<String> ngrams = new HashSet<>();
        
        // 对于长文本，使用采样策略避免生成过多 n-gram
        int step = Math.max(1, text.length() / 100); // 最多生成约 100 个 n-gram
        
        for (int i = 0; i <= text.length() - n; i += step) {
            ngrams.add(text.substring(i, i + n));
        }

        return ngrams;
    }

    /**
     * 计算句子结构相似度
     */
    private double calculateStructureSimilarity(String text1, String text2) {
        // 简单实现：比较句子数量差异
        int sentences1 = countSentences(text1);
        int sentences2 = countSentences(text2);

        if (sentences1 == 0 && sentences2 == 0) {
            return 1.0;
        }

        int maxSentences = Math.max(sentences1, sentences2);
        int diff = Math.abs(sentences1 - sentences2);

        return 1.0 - (double) diff / maxSentences;
    }

    /**
     * 计算句子数量
     */
    private int countSentences(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 中文和英文句子结束符
        String[] sentences = text.split("[。！？.!?]+");
        int count = 0;

        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                count++;
            }
        }

        return Math.max(count, 1);
    }
}