package org.example.milvuschinabank.rag.strategy;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.repository.MilvusChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下文感知召回策略
 * 结合向量检索、关键词检索和上下文邻接拉取
 */
@Component
public class ContextAwareRetrievalStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ContextAwareRetrievalStrategy.class);

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private MilvusChunkRepository chunkRepository;

    /**
     * 混合检索：向量 + 关键词
     * @param query 查询文本
     * @param queryVector 查询向量
     * @param tags 业务标签过滤
     * @return 召回结果列表
     */
    public List<DocumentChunk> hybridSearch(String query, List<Float> queryVector, List<String> tags) {
        logger.info("执行混合检索，查询: {}", query);

        // 1. 向量检索
        List<DocumentChunk> vectorResults = vectorSearch(queryVector);

        // 2. 关键词检索（基于标签过滤）
        List<DocumentChunk> keywordResults = new ArrayList<>();
        if (tags != null && !tags.isEmpty()) {
            keywordResults = keywordSearch(tags);
        }

        // 3. 合并结果（去重）
        Map<String, DocumentChunk> mergedMap = new LinkedHashMap<>();

        // 先加入向量结果
        for (DocumentChunk chunk : vectorResults) {
            mergedMap.put(chunk.getSegId(), chunk);
        }

        // 再加入关键词结果（不覆盖已有的）
        for (DocumentChunk chunk : keywordResults) {
            mergedMap.putIfAbsent(chunk.getSegId(), chunk);
        }

        List<DocumentChunk> mergedResults = new ArrayList<>(mergedMap.values());

        // 4. 按 pos 排序
        mergedResults.sort(Comparator.comparingInt(DocumentChunk::getPos));

        logger.info("混合检索完成，共召回 {} 个切片", mergedResults.size());

        return mergedResults;
    }

    /**
     * 向量检索
     */
    private List<DocumentChunk> vectorSearch(List<Float> queryVector) {
        // TODO: 实现 Milvus 向量检索
        // 根据实际的 Milvus SDK 版本实现
        logger.info("执行向量检索，TopK: {}", ragConfig.getVectorTopK());
        return new ArrayList<>();
    }

    /**
     * 关键词检索（基于标签过滤）
     */
    private List<DocumentChunk> keywordSearch(List<String> tags) {
        logger.info("执行关键词检索，标签: {}", tags);
        return chunkRepository.queryByTags(tags);
    }

    /**
     * 动态阈值调整
     * 根据召回结果数量和质量动态调整相似度阈值
     * @param resultCount 当前召回数量
     * @param avgScore 平均相似度分数
     * @return 调整后的阈值
     */
    public double adjustThreshold(int resultCount, double avgScore) {
        double baseThreshold = ragConfig.getRerankThreshold();

        // 如果召回数量太少，降低阈值
        if (resultCount < 5) {
            return Math.max(0.1, baseThreshold - 0.1);
        }

        // 如果平均分数很高，提高阈值
        if (avgScore > 0.8) {
            return Math.min(0.5, baseThreshold + 0.1);
        }

        return baseThreshold;
    }

    /**
     * 重排序：结合向量分数和关键词匹配度
     * @param chunks 待重排序的切片列表
     * @param query 查询文本
     * @return 重排序后的列表
     */
    public List<DocumentChunk> rerank(List<DocumentChunk> chunks, String query) {
        if (chunks.isEmpty()) {
            return chunks;
        }

        // 计算每个切片的综合分数
        Map<String, Double> scores = new HashMap<>();

        for (DocumentChunk chunk : chunks) {
            double vectorScore = chunk.getSemanticCohesion() != null ?
                    chunk.getSemanticCohesion() : 0.5;

            double keywordScore = calculateKeywordMatchScore(chunk.getContent(), query);

            // 加权融合
            double vectorWeight = ragConfig.getVectorWeight();
            double keywordWeight = 1.0 - vectorWeight;

            double finalScore = vectorWeight * vectorScore + keywordWeight * keywordScore;

            scores.put(chunk.getSegId(), finalScore);
        }

        // 按综合分数排序
        return chunks.stream()
                .sorted((a, b) -> Double.compare(
                        scores.getOrDefault(b.getSegId(), 0.0),
                        scores.getOrDefault(a.getSegId(), 0.0)))
                .collect(Collectors.toList());
    }

    /**
     * 计算关键词匹配分数
     */
    private double calculateKeywordMatchScore(String content, String query) {
        if (content == null || query == null) {
            return 0.0;
        }

        String[] queryTerms = query.split("[\\s\\p{Punct}]+");
        int matchCount = 0;

        for (String term : queryTerms) {
            if (term.length() >= 2 && content.contains(term)) {
                matchCount++;
            }
        }

        return queryTerms.length > 0 ? (double) matchCount / queryTerms.length : 0.0;
    }

    /**
     * 业务规则过滤
     * 根据业务规则过滤无关内容
     * @param chunks 切片列表
     * @param requiredTags 必须包含的标签
     * @return 过滤后的列表
     */
    public List<DocumentChunk> filterByBusinessRules(
            List<DocumentChunk> chunks, List<String> requiredTags) {

        if (requiredTags == null || requiredTags.isEmpty()) {
            return chunks;
        }

        return chunks.stream()
                .filter(chunk -> {
                    if (chunk.getTags() == null || chunk.getTags().isEmpty()) {
                        return false;
                    }

                    // 检查是否包含至少一个必需标签
                    return chunk.getTags().stream()
                            .anyMatch(tag -> requiredTags.contains(tag));
                })
                .collect(Collectors.toList());
    }
}