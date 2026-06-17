package org.example.milvuschinabank.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签提取器配置类
 * 用于管理业务标签、实体标签、主题标签的提取规则
 */
@Configuration
@ConfigurationProperties(prefix = "tag-extractor")
public class TagExtractorConfig {

    /**
     * 业务标签关键词列表
     */
    private List<String> businessKeywords = new ArrayList<>();

    /**
     * 实体标签正则配置
     */
    private EntityPatterns entityPatterns = new EntityPatterns();

    /**
     * 主题标签配置
     */
    private TopicPatterns topicPatterns = new TopicPatterns();

    // Getters and Setters

    public List<String> getBusinessKeywords() {
        return businessKeywords;
    }

    public void setBusinessKeywords(List<String> businessKeywords) {
        this.businessKeywords = businessKeywords;
    }

    public EntityPatterns getEntityPatterns() {
        return entityPatterns;
    }

    public void setEntityPatterns(EntityPatterns entityPatterns) {
        this.entityPatterns = entityPatterns;
    }

    public TopicPatterns getTopicPatterns() {
        return topicPatterns;
    }

    public void setTopicPatterns(TopicPatterns topicPatterns) {
        this.topicPatterns = topicPatterns;
    }

    /**
     * 实体标签正则配置
     */
    public static class EntityPatterns {
        /**
         * 货币代码正则（如 USD, EUR）
         */
        private String currency = "[A-Z]{3}";

        /**
         * 利率正则（如 5.2%, 3.15%）
         */
        private String rate = "\\d+\\.?\\d*%";

        /**
         * 日期正则（如 2024-01-15, 2024年1月15日）
         */
        private String date = "\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}";

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getRate() {
            return rate;
        }

        public void setRate(String rate) {
            this.rate = rate;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    /**
     * 主题标签配置
     */
    public static class TopicPatterns {
        /**
         * 章节标题前缀正则列表
         */
        private List<String> chapterPrefixes = new ArrayList<>();

        /**
         * 长文本判定阈值
         */
        private int minLengthForLongText = 500;

        public List<String> getChapterPrefixes() {
            return chapterPrefixes;
        }

        public void setChapterPrefixes(List<String> chapterPrefixes) {
            this.chapterPrefixes = chapterPrefixes;
        }

        public int getMinLengthForLongText() {
            return minLengthForLongText;
        }

        public void setMinLengthForLongText(int minLengthForLongText) {
            this.minLengthForLongText = minLengthForLongText;
        }
    }
}
