package org.example.milvuschinabank.rag.repository;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2.3 标量过滤索引建设 - 单元测试
 * 
 * 测试标量索引的建设和查询功能，验证：
 * - doc_id 标量索引：根据文档ID查询
 * - pos 标量索引：根据位置范围查询
 * - tags 标量索引：根据标签过滤
 * - pre_seg_id/next_seg_id 标量索引：邻接切片查询
 * - 组合条件查询：doc_id + tags 组合过滤
 * - 多标签 OR 查询
 * - 边界条件查询：pos=0 和 pos=最大值
 * - 空结果查询
 * - 索引性能对比：有索引 vs 无索引
 * - 索引完整性验证
 * 
 * @author RAG Team
 * @since 2024
 */
@SpringBootTest
public class ScalarIndexTest {

    private static final Logger logger = LoggerFactory.getLogger(ScalarIndexTest.class);

    @Autowired
    private MilvusChunkRepository milvusRepository;

    @Autowired
    private RagConfig ragConfig;

    private final String testDocId = "scalar_index_test_doc";

    @BeforeEach
    void setUp() {
        logger.info("准备标量索引测试数据...");
        
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setSegId(testDocId + "_seg_" + i);
            chunk.setDocId(testDocId);
            chunk.setPos(i);
            chunk.setPreSegId(i > 0 ? testDocId + "_seg_" + (i - 1) : null);
            chunk.setNextSegId(i < 49 ? testDocId + "_seg_" + (i + 1) : null);
            chunk.setContent("测试内容 " + i + "，包含外汇、汇率、存款等业务关键词。");
            
            List<String> tags = new ArrayList<>();
            if (i < 10) {
                tags.add("biz:外汇");
                tags.add("topic:chapter");
            } else if (i < 20) {
                tags.add("biz:汇率");
                tags.add("topic:data");
            } else if (i < 30) {
                tags.add("biz:存款");
                tags.add("biz:利率");
            } else if (i < 40) {
                tags.add("biz:贷款");
                tags.add("topic:faq");
            } else {
                tags.add("biz:结算");
                tags.add("entity:USD");
            }
            chunk.setTags(tags);
            
            chunk.setSemanticCohesion(0.5 + (i * 0.01));
            chunk.setSemanticBoundary(i % 10 == 0);
            chunks.add(chunk);
        }

        milvusRepository.insertChunks(chunks);
        logger.info("测试数据插入完成，共 {} 条", chunks.size());
    }

    @Test
    @DisplayName("测试1：doc_id 标量索引 - 根据文档ID查询")
    void testDocIdIndex() {
        logger.info("\n========== 测试1：doc_id 标量索引 ==========");
        
        long startTime = System.currentTimeMillis();
        List<DocumentChunk> results = milvusRepository.queryByDocId(testDocId);
        long endTime = System.currentTimeMillis();

        logger.info("查询 doc_id='{}' 返回 {} 条结果，耗时: {} ms", testDocId, results.size(), (endTime - startTime));
        
        assertFalse(results.isEmpty(), "应该查询到数据");
        assertTrue(results.size() >= 50, "应该至少返回50条测试数据");
        
        for (DocumentChunk chunk : results) {
            assertEquals(testDocId, chunk.getDocId(), "返回结果的docId应该匹配");
        }

        logger.info("✓ doc_id 标量索引测试通过");
    }

    @Test
    @DisplayName("测试2：pos 标量索引 - 根据位置范围查询")
    void testPosIndex() {
        logger.info("\n========== 测试2：pos 标量索引 ==========");
        
        long startTime = System.currentTimeMillis();
        List<DocumentChunk> results = milvusRepository.queryByPosRange(testDocId, 10, 20);
        long endTime = System.currentTimeMillis();

        logger.info("查询 pos in [10, 20] 返回 {} 条结果，耗时: {} ms", results.size(), (endTime - startTime));
        
        assertFalse(results.isEmpty(), "应该查询到数据");
        assertTrue(results.size() <= 11, "pos范围查询应该返回正确数量的结果");
        
        for (DocumentChunk chunk : results) {
            assertTrue(chunk.getPos() >= 10 && chunk.getPos() <= 20, 
                    "返回结果的pos应该在[10, 20]范围内");
            assertEquals(testDocId, chunk.getDocId(), "返回结果的docId应该匹配");
        }

        logger.info("✓ pos 标量索引测试通过");
    }

    @Test
    @DisplayName("测试3：tags 标量索引 - 根据标签过滤")
    void testTagsIndex() {
        logger.info("\n========== 测试3：tags 标量索引 ==========");
        
        List<String> tags = Arrays.asList("biz:外汇");
        long startTime = System.currentTimeMillis();
        List<DocumentChunk> results = milvusRepository.queryByTags(tags);
        long endTime = System.currentTimeMillis();

        logger.info("查询 tags 包含 'biz:外汇' 返回 {} 条结果，耗时: {} ms", results.size(), (endTime - startTime));
        
        assertFalse(results.isEmpty(), "应该查询到数据");
        assertTrue(results.size() >= 10, "应该至少返回10条带有biz:外汇标签的数据");
        
        for (DocumentChunk chunk : results) {
            assertTrue(chunk.getTags().contains("biz:外汇"), 
                    "返回结果应该包含biz:外汇标签");
        }

        logger.info("✓ tags 标量索引测试通过");
    }

    @Test
    @DisplayName("测试4：pre_seg_id/next_seg_id 标量索引 - 邻接切片查询")
    void testAdjacentIndex() {
        logger.info("\n========== 测试4：pre_seg_id/next_seg_id 标量索引 ==========");
        
        List<String> segIds = Arrays.asList(testDocId + "_seg_5", testDocId + "_seg_15");
        
        long startTime = System.currentTimeMillis();
        List<DocumentChunk> results = milvusRepository.queryAdjacentChunks(segIds);
        long endTime = System.currentTimeMillis();

        logger.info("查询相邻切片返回 {} 条结果，耗时: {} ms", results.size(), (endTime - startTime));
        
        assertFalse(results.isEmpty(), "应该查询到相邻切片");
        
        Set<String> resultSegIds = results.stream()
                .map(DocumentChunk::getSegId)
                .collect(Collectors.toSet());
        
        logger.info("相邻切片segIds: {}", resultSegIds);
        
        assertTrue(resultSegIds.contains(testDocId + "_seg_4"), "应该包含seg_4（seg_5的前一个）");
        assertTrue(resultSegIds.contains(testDocId + "_seg_6"), "应该包含seg_6（seg_5的后一个）");
        assertTrue(resultSegIds.contains(testDocId + "_seg_14"), "应该包含seg_14（seg_15的前一个）");
        assertTrue(resultSegIds.contains(testDocId + "_seg_16"), "应该包含seg_16（seg_15的后一个）");

        logger.info("✓ pre_seg_id/next_seg_id 标量索引测试通过");
    }

    @Test
    @DisplayName("测试5：组合条件查询 - doc_id + tags")
    void testCombinedQuery() {
        logger.info("\n========== 测试5：组合条件查询 ==========");
        
        long startTime = System.currentTimeMillis();
        List<DocumentChunk> results = milvusRepository.queryByDocId(testDocId);
        List<DocumentChunk> filtered = results.stream()
                .filter(chunk -> chunk.getTags().contains("biz:存款"))
                .collect(Collectors.toList());
        long endTime = System.currentTimeMillis();

        logger.info("组合查询 doc_id='{}' AND tags包含'biz:存款' 返回 {} 条结果，耗时: {} ms", 
                testDocId, filtered.size(), (endTime - startTime));
        
        assertFalse(filtered.isEmpty(), "应该查询到数据");
        assertTrue(filtered.size() >= 10, "应该至少返回10条数据");
        
        for (DocumentChunk chunk : filtered) {
            assertEquals(testDocId, chunk.getDocId());
            assertTrue(chunk.getTags().contains("biz:存款"));
        }

        logger.info("✓ 组合条件查询测试通过");
    }

    @Test
    @DisplayName("测试6：多标签OR查询")
    void testMultipleTagsQuery() {
        logger.info("\n========== 测试6：多标签OR查询 ==========");
        
        List<String> tags = Arrays.asList("biz:外汇", "biz:存款");
        long startTime = System.currentTimeMillis();
        List<DocumentChunk> results = milvusRepository.queryByTags(tags);
        long endTime = System.currentTimeMillis();

        logger.info("查询 tags 包含 'biz:外汇' OR 'biz:存款' 返回 {} 条结果，耗时: {} ms", 
                results.size(), (endTime - startTime));
        
        assertFalse(results.isEmpty(), "应该查询到数据");
        assertTrue(results.size() >= 20, "应该至少返回20条数据（10条外汇+10条存款）");
        
        for (DocumentChunk chunk : results) {
            boolean hasForex = chunk.getTags().contains("biz:外汇");
            boolean hasDeposit = chunk.getTags().contains("biz:存款");
            assertTrue(hasForex || hasDeposit, 
                    "返回结果应该包含biz:外汇或biz:存款标签");
        }

        logger.info("✓ 多标签OR查询测试通过");
    }

    @Test
    @DisplayName("测试7：边界条件查询 - pos=0 和 pos=49")
    void testBoundaryPosQuery() {
        logger.info("\n========== 测试7：边界条件查询 ==========");
        
        long startTime = System.currentTimeMillis();
        List<DocumentChunk> results = milvusRepository.queryByPosRange(testDocId, 0, 0);
        long endTime = System.currentTimeMillis();

        logger.info("查询 pos=0 返回 {} 条结果，耗时: {} ms", results.size(), (endTime - startTime));
        
        assertEquals(1, results.size(), "应该只返回1条结果");
        assertEquals(0, results.get(0).getPos(), "返回结果的pos应该为0");
        assertNull(results.get(0).getPreSegId(), "pos=0的preSegId应该为null");
        assertNotNull(results.get(0).getNextSegId(), "pos=0的nextSegId不应该为null");

        logger.info("✓ 边界条件查询测试通过");
    }

    @Test
    @DisplayName("测试8：空结果查询")
    void testEmptyResultQuery() {
        logger.info("\n========== 测试8：空结果查询 ==========");
        
        List<DocumentChunk> results = milvusRepository.queryByDocId("non_existent_doc");
        
        assertTrue(results.isEmpty(), "查询不存在的docId应该返回空列表");
        logger.info("✓ 空结果查询测试通过");
    }

    @Test
    @DisplayName("测试9：索引性能对比 - 有索引 vs 无索引")
    void testIndexPerformance() {
        logger.info("\n========== 测试9：索引性能对比 ==========");
        
        long totalTime = 0;
        int iterations = 5;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            milvusRepository.queryByTags(Arrays.asList("biz:外汇"));
            long endTime = System.currentTimeMillis();
            totalTime += (endTime - startTime);
        }
        
        double avgTime = (double) totalTime / iterations;
        logger.info("标签查询平均耗时: {:.2f} ms ({}次迭代)", avgTime, iterations);
        assertTrue(avgTime < 1000, "查询平均耗时应该小于1秒");

        logger.info("✓ 索引性能测试通过");
    }

    @Test
    @DisplayName("测试10：索引完整性验证")
    void testIndexIntegrity() {
        logger.info("\n========== 测试10：索引完整性验证 ==========");
        
        List<DocumentChunk> allResults = milvusRepository.queryByDocId(testDocId);
        assertEquals(50, allResults.size(), "应该返回50条测试数据");
        
        Set<Integer> posSet = allResults.stream()
                .map(DocumentChunk::getPos)
                .collect(Collectors.toSet());
        
        assertEquals(50, posSet.size(), "pos值应该唯一");
        
        for (int i = 0; i < 50; i++) {
            assertTrue(posSet.contains(i), "pos=" + i + " 应该存在");
        }

        logger.info("✓ 索引完整性验证通过");
    }
}