package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.strategy.ContextAwareRetrievalStrategy;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 4.3 召回准确度提升 - 单元测试
 * 
 * 测试混合检索、重排序、动态阈值和业务规则过滤机制，验证：
 * - 混合检索：关键词+向量检索融合
 * - 重排序模型：向量分数和关键词匹配度加权融合
 * - 动态阈值调整：根据召回结果数量和质量动态调整
 * - 业务规则过滤：基于标签过滤无关内容
 * - 关键词匹配分数计算
 * - 召回结果去重和排序
 * - 完整召回流程
 * - 边界条件处理
 * - 性能测试
 * 
 * @author RAG Team
 * @since 2024
 */
public class RecallAccuracyTest {

    private static final Logger logger = LoggerFactory.getLogger(RecallAccuracyTest.class);

    private ContextAwareRetrievalStrategy retrievalStrategy;

    @BeforeEach
    void setUp() {
        retrievalStrategy = new ContextAwareRetrievalStrategy();
    }

    @Test
    @DisplayName("测试1：混合检索 - 向量+关键词结果合并")
    void testHybridSearchMerge() {
        logger.info("\n========== 测试1：混合检索 - 向量+关键词结果合并 ==========");
        
        // 模拟向量检索结果
        List<DocumentChunk> vectorResults = Arrays.asList(
                createChunk("seg_1", 1, "个人购汇的具体额度是每年5万美元", 0.95),
                createChunk("seg_2", 2, "等值外币也可以购汇", 0.90),
                createChunk("seg_3", 3, "超出额度需要审批", 0.85)
        );
        
        // 模拟关键词检索结果
        List<DocumentChunk> keywordResults = Arrays.asList(
                createChunk("seg_2", 2, "等值外币也可以购汇", 0.0),
                createChunk("seg_4", 4, "购汇需要携带身份证", 0.0),
                createChunk("seg_5", 5, "外汇局审批流程", 0.0)
        );
        
        logger.info("向量检索结果: {} 个", vectorResults.size());
        for (DocumentChunk chunk : vectorResults) {
            logger.info("  segId={}, pos={}, score={}", chunk.getSegId(), chunk.getPos(), chunk.getSemanticCohesion());
        }
        
        logger.info("\n关键词检索结果: {} 个", keywordResults.size());
        for (DocumentChunk chunk : keywordResults) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        // 合并结果（去重）
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
        
        // 按 pos 排序
        mergedResults.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        logger.info("\n合并后结果: {} 个", mergedResults.size());
        for (DocumentChunk chunk : mergedResults) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        assertEquals(5, mergedResults.size(), "合并后应该有5个唯一切片");
        assertEquals("seg_1", mergedResults.get(0).getSegId());
        assertEquals("seg_2", mergedResults.get(1).getSegId());
        assertEquals("seg_3", mergedResults.get(2).getSegId());
        assertEquals("seg_4", mergedResults.get(3).getSegId());
        assertEquals("seg_5", mergedResults.get(4).getSegId());
        
        logger.info("✓ 混合检索合并测试通过");
    }

    @Test
    @DisplayName("测试2：重排序 - 向量分数和关键词匹配度加权融合")
    void testRerankWeightedFusion() {
        logger.info("\n========== 测试2：重排序 - 向量分数和关键词匹配度加权融合 ==========");
        
        String query = "个人购汇的具体额度是多少？";
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "个人购汇的具体额度是每年5万美元", 0.95),
                createChunk("seg_2", 2, "等值外币也可以购汇", 0.90),
                createChunk("seg_3", 3, "外汇局审批流程需要5个工作日", 0.85),
                createChunk("seg_4", 4, "购汇需要携带身份证和申请书", 0.80),
                createChunk("seg_5", 5, "银行存款利率为1.5%", 0.75)
        );
        
        logger.info("重排序前:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}, vectorScore={}", 
                    chunk.getSegId(), chunk.getPos(), chunk.getSemanticCohesion());
        }
        
        // 计算关键词匹配分数
        Map<String, Double> keywordScores = new HashMap<>();
        for (DocumentChunk chunk : chunks) {
            double score = calculateKeywordMatchScore(chunk.getContent(), query);
            keywordScores.put(chunk.getSegId(), score);
            logger.info("  segId={}, keywordScore={}", chunk.getSegId(), score);
        }
        
        // 加权融合
        double vectorWeight = 0.7;
        double keywordWeight = 0.3;
        
        Map<String, Double> finalScores = new HashMap<>();
        for (DocumentChunk chunk : chunks) {
            double vectorScore = chunk.getSemanticCohesion() != null ? chunk.getSemanticCohesion() : 0.5;
            double keywordScore = keywordScores.getOrDefault(chunk.getSegId(), 0.0);
            double finalScore = vectorWeight * vectorScore + keywordWeight * keywordScore;
            finalScores.put(chunk.getSegId(), finalScore);
        }
        
        logger.info("\n加权融合后:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, finalScore={}", chunk.getSegId(), finalScores.get(chunk.getSegId()));
        }
        
        // 按综合分数排序
        List<DocumentChunk> reranked = new ArrayList<>(chunks);
        reranked.sort((a, b) -> Double.compare(
                finalScores.getOrDefault(b.getSegId(), 0.0),
                finalScores.getOrDefault(a.getSegId(), 0.0)));
        
        logger.info("\n重排序后:");
        for (int i = 0; i < reranked.size(); i++) {
            DocumentChunk chunk = reranked.get(i);
            logger.info("  排名{}: segId={}, finalScore={}", 
                    i + 1, chunk.getSegId(), finalScores.get(chunk.getSegId()));
        }
        
        // 验证排序
        assertEquals("seg_1", reranked.get(0).getSegId(), "seg_1应该排第1（高向量分+高关键词匹配）");
        
        logger.info("✓ 重排序加权融合测试通过");
    }

    @Test
    @DisplayName("测试3：动态阈值调整 - 召回数量少时降低阈值")
    void testDynamicThresholdLowCount() {
        logger.info("\n========== 测试3：动态阈值调整 - 召回数量少时降低阈值 ==========");
        
        double baseThreshold = 0.3;
        int resultCount = 2;
        double avgScore = 0.6;
        
        double adjustedThreshold = adjustThreshold(baseThreshold, resultCount, avgScore);
        
        logger.info("基础阈值: {}", baseThreshold);
        logger.info("召回数量: {}", resultCount);
        logger.info("平均分数: {}", avgScore);
        logger.info("调整后阈值: {}", adjustedThreshold);
        
        assertEquals(0.2, adjustedThreshold, 0.01, "召回数量<5时应该降低阈值0.1");
        
        logger.info("✓ 动态阈值调整（低数量）测试通过");
    }

    @Test
    @DisplayName("测试4：动态阈值调整 - 平均分数高时提高阈值")
    void testDynamicThresholdHighScore() {
        logger.info("\n========== 测试4：动态阈值调整 - 平均分数高时提高阈值 ==========");
        
        double baseThreshold = 0.3;
        int resultCount = 10;
        double avgScore = 0.85;
        
        double adjustedThreshold = adjustThreshold(baseThreshold, resultCount, avgScore);
        
        logger.info("基础阈值: {}", baseThreshold);
        logger.info("召回数量: {}", resultCount);
        logger.info("平均分数: {}", avgScore);
        logger.info("调整后阈值: {}", adjustedThreshold);
        
        assertEquals(0.4, adjustedThreshold, 0.01, "平均分数>0.8时应该提高阈值0.1");
        
        logger.info("✓ 动态阈值调整（高分数）测试通过");
    }

    @Test
    @DisplayName("测试5：动态阈值调整 - 正常情况保持基础阈值")
    void testDynamicThresholdNormal() {
        logger.info("\n========== 测试5：动态阈值调整 - 正常情况保持基础阈值 ==========");
        
        double baseThreshold = 0.3;
        int resultCount = 8;
        double avgScore = 0.7;
        
        double adjustedThreshold = adjustThreshold(baseThreshold, resultCount, avgScore);
        
        logger.info("基础阈值: {}", baseThreshold);
        logger.info("召回数量: {}", resultCount);
        logger.info("平均分数: {}", avgScore);
        logger.info("调整后阈值: {}", adjustedThreshold);
        
        assertEquals(0.3, adjustedThreshold, 0.01, "正常情况下应该保持基础阈值");
        
        logger.info("✓ 动态阈值调整（正常）测试通过");
    }

    @Test
    @DisplayName("测试6：业务规则过滤 - 基于标签过滤")
    void testBusinessRuleFilterByTags() {
        logger.info("\n========== 测试6：业务规则过滤 - 基于标签过滤 ==========");
        
        List<String> requiredTags = Arrays.asList("外汇", "购汇");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunkWithTags("seg_1", 1, "个人购汇的具体额度", Arrays.asList("外汇", "购汇")),
                createChunkWithTags("seg_2", 2, "等值外币购汇", Arrays.asList("外汇")),
                createChunkWithTags("seg_3", 3, "银行存款利率", Arrays.asList("存款")),
                createChunkWithTags("seg_4", 4, "贷款审批流程", Arrays.asList("贷款")),
                createChunkWithTags("seg_5", 5, "外汇局审批", Arrays.asList("外汇", "审批"))
        );
        
        logger.info("过滤前: {} 个切片", chunks.size());
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, tags={}", chunk.getSegId(), chunk.getTags());
        }
        
        List<DocumentChunk> filtered = filterByBusinessRules(chunks, requiredTags);
        
        logger.info("\n过滤后: {} 个切片", filtered.size());
        for (DocumentChunk chunk : filtered) {
            logger.info("  segId={}, tags={}", chunk.getSegId(), chunk.getTags());
        }
        
        assertEquals(3, filtered.size(), "应该保留3个包含必需标签的切片");
        assertEquals("seg_1", filtered.get(0).getSegId());
        assertEquals("seg_2", filtered.get(1).getSegId());
        assertEquals("seg_5", filtered.get(2).getSegId());
        
        logger.info("✓ 业务规则过滤测试通过");
    }

    @Test
    @DisplayName("测试7：关键词匹配分数计算 - 完全匹配")
    void testKeywordMatchScoreExactMatch() {
        logger.info("\n========== 测试7：关键词匹配分数计算 - 完全匹配 ==========");
        
        String content = "个人购汇的具体额度是每年5万美元";
        String query = "个人购汇的具体额度";
        
        double score = calculateKeywordMatchScore(content, query);
        
        logger.info("内容: {}", content);
        logger.info("查询: {}", query);
        logger.info("匹配分数: {}", score);
        
        assertTrue(score > 0.5, "完全匹配应该获得高分数");
        
        logger.info("✓ 关键词匹配分数（完全匹配）测试通过");
    }

    @Test
    @DisplayName("测试8：关键词匹配分数计算 - 部分匹配")
    void testKeywordMatchScorePartialMatch() {
        logger.info("\n========== 测试8：关键词匹配分数计算 - 部分匹配 ==========");
        
        String content = "外汇局审批流程需要5个工作日";
        String query = "个人购汇的具体额度是多少？";
        
        double score = calculateKeywordMatchScore(content, query);
        
        logger.info("内容: {}", content);
        logger.info("查询: {}", query);
        logger.info("匹配分数: {}", score);
        
        assertTrue(score < 0.3, "部分匹配应该获得低分数");
        
        logger.info("✓ 关键词匹配分数（部分匹配）测试通过");
    }

    @Test
    @DisplayName("测试9：关键词匹配分数计算 - 无匹配")
    void testKeywordMatchScoreNoMatch() {
        logger.info("\n========== 测试9：关键词匹配分数计算 - 无匹配 ==========");
        
        String content = "银行存款利率为1.5%";
        String query = "个人购汇的具体额度是多少？";
        
        double score = calculateKeywordMatchScore(content, query);
        
        logger.info("内容: {}", content);
        logger.info("查询: {}", query);
        logger.info("匹配分数: {}", score);
        
        assertEquals(0.0, score, 0.01, "无匹配应该获得0分");
        
        logger.info("✓ 关键词匹配分数（无匹配）测试通过");
    }

    @Test
    @DisplayName("测试10：完整召回流程 - 混合检索+重排序+过滤")
    void testCompleteRecallFlow() {
        logger.info("\n========== 测试10：完整召回流程 - 混合检索+重排序+过滤 ==========");
        
        String query = "个人购汇的具体额度是多少？";
        List<String> requiredTags = Arrays.asList("外汇", "购汇");
        
        // 1. 模拟混合检索
        List<DocumentChunk> vectorResults = Arrays.asList(
                createChunk("seg_1", 1, "个人购汇的具体额度是每年5万美元", 0.95),
                createChunk("seg_2", 2, "等值外币也可以购汇", 0.90),
                createChunk("seg_3", 3, "超出额度需要审批", 0.85)
        );
        
        List<DocumentChunk> keywordResults = Arrays.asList(
                createChunkWithTags("seg_2", 2, "等值外币也可以购汇", Arrays.asList("外汇")),
                createChunkWithTags("seg_4", 4, "购汇需要携带身份证", Arrays.asList("外汇", "购汇")),
                createChunkWithTags("seg_5", 5, "外汇局审批流程", Arrays.asList("外汇", "审批"))
        );
        
        logger.info("步骤1：混合检索");
        logger.info("  向量检索: {} 个", vectorResults.size());
        logger.info("  关键词检索: {} 个", keywordResults.size());
        
        // 2. 合并去重
        Map<String, DocumentChunk> mergedMap = new LinkedHashMap<>();
        for (DocumentChunk chunk : vectorResults) {
            mergedMap.put(chunk.getSegId(), chunk);
        }
        for (DocumentChunk chunk : keywordResults) {
            mergedMap.putIfAbsent(chunk.getSegId(), chunk);
        }
        List<DocumentChunk> mergedResults = new ArrayList<>(mergedMap.values());
        logger.info("  合并后: {} 个", mergedResults.size());
        
        // 3. 业务规则过滤
        List<DocumentChunk> filtered = filterByBusinessRules(mergedResults, requiredTags);
        logger.info("\n步骤2：业务规则过滤");
        logger.info("  过滤后: {} 个", filtered.size());
        
        // 4. 重排序
        Map<String, Double> keywordScores = new HashMap<>();
        for (DocumentChunk chunk : filtered) {
            double score = calculateKeywordMatchScore(chunk.getContent(), query);
            keywordScores.put(chunk.getSegId(), score);
        }
        
        double vectorWeight = 0.7;
        double keywordWeight = 0.3;
        
        Map<String, Double> finalScores = new HashMap<>();
        for (DocumentChunk chunk : filtered) {
            double vectorScore = chunk.getSemanticCohesion() != null ? chunk.getSemanticCohesion() : 0.5;
            double keywordScore = keywordScores.getOrDefault(chunk.getSegId(), 0.0);
            double finalScore = vectorWeight * vectorScore + keywordWeight * keywordScore;
            finalScores.put(chunk.getSegId(), finalScore);
        }
        
        List<DocumentChunk> reranked = new ArrayList<>(filtered);
        reranked.sort((a, b) -> Double.compare(
                finalScores.getOrDefault(b.getSegId(), 0.0),
                finalScores.getOrDefault(a.getSegId(), 0.0)));
        
        logger.info("\n步骤3：重排序");
        for (int i = 0; i < reranked.size(); i++) {
            DocumentChunk chunk = reranked.get(i);
            logger.info("  排名{}: segId={}, finalScore={}", 
                    i + 1, chunk.getSegId(), finalScores.get(chunk.getSegId()));
        }
        
        // 5. 动态阈值过滤
        double threshold = 0.3;
        List<DocumentChunk> finalResults = new ArrayList<>();
        for (DocumentChunk chunk : reranked) {
            double score = finalScores.get(chunk.getSegId());
            if (score >= threshold) {
                finalResults.add(chunk);
            }
        }
        
        logger.info("\n步骤4：动态阈值过滤 (threshold={})", threshold);
        logger.info("  最终结果: {} 个", finalResults.size());
        for (DocumentChunk chunk : finalResults) {
            logger.info("  segId={}, score={}", chunk.getSegId(), finalScores.get(chunk.getSegId()));
        }
        
        // 验证
        assertTrue(finalResults.size() > 0, "应该有最终结果");
        
        logger.info("✓ 完整召回流程测试通过");
    }

    @Test
    @DisplayName("测试11：边界条件 - 空检索结果")
    void testEmptyRetrievalResults() {
        logger.info("\n========== 测试11：边界条件 - 空检索结果 ==========");
        
        List<DocumentChunk> vectorResults = new ArrayList<>();
        List<DocumentChunk> keywordResults = new ArrayList<>();
        
        Map<String, DocumentChunk> mergedMap = new LinkedHashMap<>();
        for (DocumentChunk chunk : vectorResults) {
            mergedMap.put(chunk.getSegId(), chunk);
        }
        for (DocumentChunk chunk : keywordResults) {
            mergedMap.putIfAbsent(chunk.getSegId(), chunk);
        }
        
        List<DocumentChunk> mergedResults = new ArrayList<>(mergedMap.values());
        
        assertEquals(0, mergedResults.size(), "空检索结果合并后应该仍为空");
        
        logger.info("✓ 空检索结果边界条件测试通过");
    }

    @Test
    @DisplayName("测试12：边界条件 - 全部重复结果")
    void testAllDuplicateResults() {
        logger.info("\n========== 测试12：边界条件 - 全部重复结果 ==========");
        
        List<DocumentChunk> vectorResults = Arrays.asList(
                createChunk("seg_1", 1, "内容1", 0.95),
                createChunk("seg_2", 2, "内容2", 0.90)
        );
        
        List<DocumentChunk> keywordResults = Arrays.asList(
                createChunk("seg_1", 1, "内容1", 0.0),
                createChunk("seg_2", 2, "内容2", 0.0)
        );
        
        Map<String, DocumentChunk> mergedMap = new LinkedHashMap<>();
        for (DocumentChunk chunk : vectorResults) {
            mergedMap.put(chunk.getSegId(), chunk);
        }
        for (DocumentChunk chunk : keywordResults) {
            mergedMap.putIfAbsent(chunk.getSegId(), chunk);
        }
        
        List<DocumentChunk> mergedResults = new ArrayList<>(mergedMap.values());
        
        assertEquals(2, mergedResults.size(), "全部重复时应该只保留向量结果");
        
        logger.info("✓ 全部重复结果边界条件测试通过");
    }

    @Test
    @DisplayName("测试13：边界条件 - 无必需标签")
    void testNoRequiredTags() {
        logger.info("\n========== 测试13：边界条件 - 无必需标签 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunkWithTags("seg_1", 1, "内容1", Arrays.asList("外汇")),
                createChunkWithTags("seg_2", 2, "内容2", Arrays.asList("存款"))
        );
        
        List<DocumentChunk> filtered = filterByBusinessRules(chunks, null);
        
        assertEquals(2, filtered.size(), "无必需标签时应该保留所有切片");
        
        filtered = filterByBusinessRules(chunks, new ArrayList<>());
        assertEquals(2, filtered.size(), "空必需标签列表时应该保留所有切片");
        
        logger.info("✓ 无必需标签边界条件测试通过");
    }

    @Test
    @DisplayName("测试14：性能测试 - 大量结果重排序")
    void testPerformanceLargeDataset() {
        logger.info("\n========== 测试14：性能测试 - 大量结果重排序 ==========");
        
        int chunkCount = 100;
        String query = "个人购汇的具体额度是多少？";
        
        List<DocumentChunk> chunks = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < chunkCount; i++) {
            double score = 0.5 + random.nextDouble() * 0.5;
            chunks.add(createChunk("seg_" + i, i, "内容_" + i, score));
        }
        
        logger.info("生成 {} 个切片", chunkCount);
        
        long startTime = System.currentTimeMillis();
        
        // 计算关键词分数
        Map<String, Double> keywordScores = new HashMap<>();
        for (DocumentChunk chunk : chunks) {
            double score = calculateKeywordMatchScore(chunk.getContent(), query);
            keywordScores.put(chunk.getSegId(), score);
        }
        
        // 加权融合
        double vectorWeight = 0.7;
        double keywordWeight = 0.3;
        
        Map<String, Double> finalScores = new HashMap<>();
        for (DocumentChunk chunk : chunks) {
            double vectorScore = chunk.getSemanticCohesion() != null ? chunk.getSemanticCohesion() : 0.5;
            double keywordScore = keywordScores.getOrDefault(chunk.getSegId(), 0.0);
            double finalScore = vectorWeight * vectorScore + keywordWeight * keywordScore;
            finalScores.put(chunk.getSegId(), finalScore);
        }
        
        // 重排序
        List<DocumentChunk> reranked = new ArrayList<>(chunks);
        reranked.sort((a, b) -> Double.compare(
                finalScores.getOrDefault(b.getSegId(), 0.0),
                finalScores.getOrDefault(a.getSegId(), 0.0)));
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        logger.info("重排序耗时: {} ms", totalTime);
        logger.info("切片数量: {}", reranked.size());
        
        // 验证排序
        boolean isSorted = true;
        for (int i = 0; i < reranked.size() - 1; i++) {
            if (finalScores.get(reranked.get(i).getSegId()) < finalScores.get(reranked.get(i + 1).getSegId())) {
                isSorted = false;
                break;
            }
        }
        
        assertTrue(isSorted, "重排序后应该按分数降序");
        assertTrue(totalTime < 1000, "重排序耗时应该小于1秒");
        
        logger.info("✓ 性能测试通过");
    }

    // ==================== 辅助方法 ====================

    private DocumentChunk createChunk(String segId, int pos, String content, double score) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setSegId(segId);
        chunk.setPos(pos);
        chunk.setContent(content);
        chunk.setSemanticCohesion(score);
        return chunk;
    }

    private DocumentChunk createChunkWithTags(String segId, int pos, String content, List<String> tags) {
        DocumentChunk chunk = createChunk(segId, pos, content, 0.8);
        chunk.setTags(tags);
        return chunk;
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
     * 动态阈值调整
     */
    private double adjustThreshold(double baseThreshold, int resultCount, double avgScore) {
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
     * 业务规则过滤
     */
    private List<DocumentChunk> filterByBusinessRules(
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
                .collect(java.util.stream.Collectors.toList());
    }
}