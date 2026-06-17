package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.TagExtractorConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标签提取服务
 * 使用规则和小模型提取业务/主题/实体标签
 */
@Service
public class TagExtractor {

    private final TagExtractorConfig config;
    
    // 预编译的正则表达式（延迟初始化）
    private Pattern currencyPattern;
    private Pattern ratePattern;
    private Pattern datePattern;
    private Pattern containsDigitsPattern;
    private List<Pattern> chapterPatterns;

    public TagExtractor(TagExtractorConfig config) {
        this.config = config;
        initializePatterns();
    }

    /**
     * 初始化正则表达式（从配置读取）
     */
    private void initializePatterns() {
        TagExtractorConfig.EntityPatterns entity = config.getEntityPatterns();
        
        this.currencyPattern = Pattern.compile(entity.getCurrency());
        this.ratePattern = Pattern.compile(entity.getRate());
        this.datePattern = Pattern.compile(entity.getDate());
        this.containsDigitsPattern = Pattern.compile("\\d");
        
        // 初始化章节模式
        this.chapterPatterns = new ArrayList<>();
        TagExtractorConfig.TopicPatterns topic = config.getTopicPatterns();
        if (topic.getChapterPrefixes() != null) {
            for (String pattern : topic.getChapterPrefixes()) {
                try {
                    this.chapterPatterns.add(Pattern.compile(pattern));
                } catch (Exception e) {
                    System.err.println("[TagExtractor] 无效的章节正则: " + pattern);
                }
            }
        }
    }

    /**
     * 提取切片标签
     * @param content 切片内容
     * @param docId 文档ID
     * @param pos 位置
     * @return 标签列表
     */
    public List<String> extractTags(String content, String docId, int pos) {
        long startTime = System.currentTimeMillis();
        List<String> tags = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return tags;
        }

        // 1. 提取业务标签
        long t1 = System.currentTimeMillis();
        tags.addAll(extractBusinessTags(content));
        long t2 = System.currentTimeMillis();

        // 2. 提取实体标签
        tags.addAll(extractEntityTags(content));
        long t3 = System.currentTimeMillis();

        // 3. 提取主题标签
        tags.addAll(extractTopicTags(content));
        long t4 = System.currentTimeMillis();

        if (t4 - startTime > 10) { // 只记录耗时超过 10ms 的
            System.out.println(String.format("[TagExtractor] pos=%d, 业务=%dms, 实体=%dms, 主题=%dms, 总计=%dms, 内容长度=%d",
                    pos, t2-t1, t3-t2, t4-t3, t4-startTime, content.length()));
        }

        return tags;
    }

    /**
     * 提取业务标签（使用配置的关键词）
     */
    private List<String> extractBusinessTags(String content) {
        List<String> businessTags = new ArrayList<>();
        List<String> keywords = config.getBusinessKeywords();

        if (keywords == null || keywords.isEmpty()) {
            return businessTags;
        }

        for (String keyword : keywords) {
            if (content.indexOf(keyword) >= 0) {
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
        Matcher currencyMatcher = currencyPattern.matcher(content);
        while (currencyMatcher.find()) {
            entityTags.add("entity:currency:" + currencyMatcher.group());
        }

        // 提取利率
        Matcher rateMatcher = ratePattern.matcher(content);
        while (rateMatcher.find()) {
            entityTags.add("entity:rate:" + rateMatcher.group());
        }

        // 提取日期
        Matcher dateMatcher = datePattern.matcher(content);
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
        TagExtractorConfig.TopicPatterns topicConfig = config.getTopicPatterns();

        // 基于内容长度判断（长文本可能包含多个主题）
        if (content.length() > topicConfig.getMinLengthForLongText()) {
            topicTags.add("topic:long_text");
        }

        // 检测是否包含数字（可能是数据/报告类）
        if (containsDigitsPattern.matcher(content).find()) {
            topicTags.add("topic:data");
        }

        // 检测是否包含问句（可能是FAQ类）
        if (content.contains("？") || content.contains("?")) {
            topicTags.add("topic:faq");
        }

        // 检测是否包含标题特征
        for (Pattern pattern : chapterPatterns) {
            if (pattern.matcher(content).matches()) {
                topicTags.add("topic:chapter");
                break;
            }
        }

        return topicTags;
    }
}
