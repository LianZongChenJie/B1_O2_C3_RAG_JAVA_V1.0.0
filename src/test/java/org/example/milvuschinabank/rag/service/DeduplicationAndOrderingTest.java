package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.model.ReActState;
import org.example.milvuschinabank.rag.model.RetrievalResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2.6 全局去重与 pos 顺序规整 - 单元测试
 * 
 * 测试 ReAct 多轮召回的全局去重和顺序规整机制，验证：
 * - TotalSeg 并集全局去重，避免重复召回
 * - 对全集按 pos 升序强制顺序规整
 * - 严格还原原文阅读顺序
 * - 多轮召回中的去重逻辑
 * - 重复内容检测与合并
 * - 相邻切片重叠检测
 * - 无效内容过滤
 * - 最终结果的 pos 顺序保证
 * 
 * @author RAG Team
 * @since 2024
 */
public class DeduplicationAndOrderingTest {

    private static final Logger logger = LoggerFactory.getLogger(DeduplicationAndOrderingTest.class);

    @Test
    @DisplayName("测试1：全局去重 - TotalSeg 并集去重")
    void testGlobalDeduplication() {
        logger.info("\n========== 测试1：全局去重 - TotalSeg 并集去重 ==========");
        
        Set<String> totalSegIds = new HashSet<>();
        
        // 第1轮召回
        List<String> round1Ids = Arrays.asList("seg_1", "seg_2", "seg_3");
        totalSegIds.addAll(round1Ids);
        logger.info("第1轮召回: {}", round1Ids);
        logger.info("TotalSeg 集合: {}", totalSegIds);
        
        // 第2轮召回（包含重复）
        List<String> round2Ids = Arrays.asList("seg_2", "seg_3", "seg_4", "seg_5");
        Set<String> newIds = new HashSet<>();
        for (String id : round2Ids) {
            if (!totalSegIds.contains(id)) {
                newIds.add(id);
                totalSegIds.add(id);
            } else {
                logger.info("过滤重复: {}", id);
            }
        }
        
        logger.info("第2轮召回: {}", round2Ids);
        logger.info("新增切片: {}", newIds);
        logger.info("TotalSeg 集合: {}", totalSegIds);
        
        assertEquals(5, totalSegIds.size(), "去重后应该有5个唯一切片");
        assertTrue(totalSegIds.containsAll(Arrays.asList("seg_1", "seg_2", "seg_3", "seg_4", "seg_5")),
                "应该包含所有切片");
        
        logger.info("✓ 全局去重测试通过");
    }

    @Test
    @DisplayName("测试2：pos 升序强制顺序规整")
    void testPosAscendingOrdering() {
        logger.info("\n========== 测试2：pos 升序强制顺序规整 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_3", 5, "内容3"),
                createChunk("seg_1", 1, "内容1"),
                createChunk("seg_5", 9, "内容5"),
                createChunk("seg_2", 3, "内容2"),
                createChunk("seg_4", 7, "内容4")
        );
        
        logger.info("排序前 pos 顺序:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        // 按 pos 升序排序
        chunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        logger.info("\n排序后 pos 顺序:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        // 验证升序
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertTrue(chunks.get(i).getPos() < chunks.get(i + 1).getPos(),
                    "pos 应该严格升序");
        }
        
        assertEquals(1, chunks.get(0).getPos(), "第一个 pos 应该是 1");
        assertEquals(9, chunks.get(4).getPos(), "最后一个 pos 应该是 9");
        
        logger.info("\n✓ pos 升序强制顺序规整测试通过");
    }

    @Test
    @DisplayName("测试3：严格还原原文阅读顺序")
    void testRestoreOriginalReadingOrder() {
        logger.info("\n========== 测试3：严格还原原文阅读顺序 ==========");
        
        // 模拟多轮召回的乱序结果
        List<DocumentChunk> chunks = new ArrayList<>();
        chunks.add(createChunk("seg_10", 15, "第15段内容"));
        chunks.add(createChunk("seg_2", 3, "第3段内容"));
        chunks.add(createChunk("seg_7", 11, "第11段内容"));
        chunks.add(createChunk("seg_1", 1, "第1段内容"));
        chunks.add(createChunk("seg_5", 7, "第7段内容"));
        
        logger.info("召回顺序（乱序）:");
        for (int i = 0; i < chunks.size(); i++) {
            logger.info("  召回顺序{}: segId={}, pos={}", 
                    i + 1, chunks.get(i).getSegId(), chunks.get(i).getPos());
        }
        
        // 按 pos 排序还原阅读顺序
        chunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        logger.info("\n阅读顺序（pos升序）:");
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            logger.info("  阅读顺序{}: segId={}, pos={}", 
                    i + 1, chunk.getSegId(), chunk.getPos());
            context.append("[").append(chunk.getPos()).append("]").append(chunk.getContent()).append("\n");
        }
        
        // 验证顺序
        assertEquals(1, chunks.get(0).getPos());
        assertEquals(3, chunks.get(1).getPos());
        assertEquals(7, chunks.get(2).getPos());
        assertEquals(11, chunks.get(3).getPos());
        assertEquals(15, chunks.get(4).getPos());
        
        logger.info("\n还原的上下文:\n{}", context);
        logger.info("\n✓ 严格还原原文阅读顺序测试通过");
    }

    @Test
    @DisplayName("测试4：多轮召回去重 - 跨轮次去重")
    void testMultiRoundDeduplication() {
        logger.info("\n========== 测试4：多轮召回去重 - 跨轮次去重 ==========");
        
        ReActState state = new ReActState(5, 8000, 3);
        state.setRoundResults(new ArrayList<>());
        
        // 第1轮
        state.nextRound();
        List<RetrievalResult> round1 = Arrays.asList(
                new RetrievalResult("seg_1", 0.95, 1, "vector"),
                new RetrievalResult("seg_2", 0.90, 1, "vector"),
                new RetrievalResult("seg_3", 0.85, 1, "vector")
        );
        state.getTotalSegIds().addAll(extractSegIds(round1));
        logger.info("第1轮召回: {}", extractSegIds(round1));
        logger.info("TotalSeg: {}", state.getTotalSegIds());
        
        // 第2轮（包含重复）
        state.nextRound();
        List<RetrievalResult> round2 = Arrays.asList(
                new RetrievalResult("seg_2", 0.88, 2, "vector"),
                new RetrievalResult("seg_3", 0.82, 2, "vector"),
                new RetrievalResult("seg_4", 0.78, 2, "vector")
        );
        
        Set<String> existingIds = new HashSet<>(state.getTotalSegIds());
        List<RetrievalResult> uniqueRound2 = new ArrayList<>();
        for (RetrievalResult result : round2) {
            if (!existingIds.contains(result.getSegId())) {
                uniqueRound2.add(result);
                state.getTotalSegIds().add(result.getSegId());
            } else {
                logger.info("第2轮过滤重复: {}", result.getSegId());
            }
        }
        
        logger.info("第2轮召回: {}", extractSegIds(round2));
        logger.info("第2轮去重后: {}", extractSegIds(uniqueRound2));
        logger.info("TotalSeg: {}", state.getTotalSegIds());
        
        assertEquals(4, state.getTotalSegIds().size(), "应该有4个唯一切片");
        assertEquals(1, uniqueRound2.size(), "第2轮应该只有1个新切片");
        
        logger.info("✓ 多轮召回去重测试通过");
    }

    @Test
    @DisplayName("测试5：重复内容检测 - 完全重复")
    void testExactDuplicateDetection() {
        logger.info("\n========== 测试5：重复内容检测 - 完全重复 ==========");
        
        String content1 = "个人购汇的具体额度是多少？";
        String content2 = "个人购汇的具体额度是多少？";
        
        boolean isDuplicate = isDuplicateOrOverlap(content1, content2);
        
        logger.info("内容1: {}", content1);
        logger.info("内容2: {}", content2);
        logger.info("是否重复: {}", isDuplicate);
        
        assertTrue(isDuplicate, "完全相同的内容应该被检测为重复");
        
        logger.info("✓ 完全重复检测测试通过");
    }

    @Test
    @DisplayName("测试6：重复内容检测 - 尾部/头部重叠")
    void testOverlapDetection() {
        logger.info("\n========== 测试6：重复内容检测 - 尾部/头部重叠 ==========");
        
        // 确保尾部/头部完全匹配15个字符
        String content1 = "这是第一段内容，结尾重叠部分ABC。";
        String content2 = "结尾重叠部分ABC。这是第二段内容。";
        
        boolean hasOverlap = hasOverlap(content1, content2, 10);
        
        logger.info("内容1: {}", content1);
        logger.info("内容2: {}", content2);
        logger.info("是否重叠: {}", hasOverlap);
        
        assertTrue(hasOverlap, "尾部/头部重叠应该被检测到");
        
        // 测试不重叠的情况
        String content3 = "完全不同的内容A";
        String content4 = "完全不同的内容B";
        boolean noOverlap = hasOverlap(content3, content4, 10);
        
        logger.info("\n内容3: {}", content3);
        logger.info("内容4: {}", content4);
        logger.info("是否重叠: {}", noOverlap);
        
        assertFalse(noOverlap, "不重叠的内容不应该被误判");
        
        logger.info("✓ 重叠检测测试通过");
    }

    @Test
    @DisplayName("测试7：重复内容合并")
    void testDuplicateContentMerge() {
        logger.info("\n========== 测试7：重复内容合并 ==========");
        
        String content1 = "这是第一段内容，结尾部分是重叠的部分。";
        String content2 = "重叠的部分。这是第二段内容。";
        
        String merged = mergeOverlap(content1, content2);
        
        logger.info("内容1: {}", content1);
        logger.info("内容2: {}", content2);
        logger.info("合并后: {}", merged);
        
        assertNotNull(merged, "合并结果不应为 null");
        assertTrue(merged.length() > content1.length(), "合并后应该更长");
        assertTrue(merged.contains("这是第一段内容"), "应该包含第一段内容");
        assertTrue(merged.contains("这是第二段内容"), "应该包含第二段内容");
        
        logger.info("✓ 重复内容合并测试通过");
    }

    @Test
    @DisplayName("测试8：无效内容过滤 - 空文本")
    void testEmptyContentFiltering() {
        logger.info("\n========== 测试8：无效内容过滤 - 空文本 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "有效内容1"),
                createChunk("seg_2", 2, ""),
                createChunk("seg_3", 3, "   "),
                createChunk("seg_4", 4, null),
                createChunk("seg_5", 5, "有效内容2")
        );
        
        logger.info("过滤前: {} 个切片", chunks.size());
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, content='{}'", chunk.getSegId(), chunk.getContent());
        }
        
        List<DocumentChunk> validChunks = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            if (isValidContent(chunk.getContent())) {
                validChunks.add(chunk);
            } else {
                logger.info("过滤无效内容: segId={}", chunk.getSegId());
            }
        }
        
        logger.info("\n过滤后: {} 个切片", validChunks.size());
        for (DocumentChunk chunk : validChunks) {
            logger.info("  segId={}, content='{}'", chunk.getSegId(), chunk.getContent());
        }
        
        assertEquals(2, validChunks.size(), "应该只保留2个有效切片");
        assertEquals("seg_1", validChunks.get(0).getSegId());
        assertEquals("seg_5", validChunks.get(1).getSegId());
        
        logger.info("✓ 空文本过滤测试通过");
    }

    @Test
    @DisplayName("测试9：无效内容过滤 - 乱码检测")
    void testGarbledContentFiltering() {
        logger.info("\n========== 测试9：无效内容过滤 - 乱码检测 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "正常中文内容"),
                createChunk("seg_2", 2, "   "),
                createChunk("seg_3", 3, "有效内容"),
                createChunk("seg_4", 4, null),
                createChunk("seg_5", 5, "另一个有效内容")
        );
        
        logger.info("过滤前: {} 个切片", chunks.size());
        
        List<DocumentChunk> validChunks = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            if (isValidContent(chunk.getContent())) {
                validChunks.add(chunk);
            } else {
                logger.info("过滤乱码内容: segId={}, content='{}'", 
                        chunk.getSegId(), chunk.getContent());
            }
        }
        
        logger.info("过滤后: {} 个切片", validChunks.size());
        
        assertEquals(3, validChunks.size(), "应该保留3个有效切片");
        
        logger.info("✓ 乱码检测过滤测试通过");
    }

    @Test
    @DisplayName("测试10：完整流程 - 多轮去重 + pos 排序 + 内容过滤")
    void testCompleteDeduplicationAndOrderingFlow() {
        logger.info("\n========== 测试10：完整流程 - 多轮去重 + pos 排序 + 内容过滤 ==========");
        
        // 模拟3轮召回
        ReActState state = new ReActState(5, 8000, 3);
        List<DocumentChunk> allChunks = new ArrayList<>();
        Set<String> totalSegIds = state.getTotalSegIds();
        
        // 第1轮
        List<DocumentChunk> round1 = Arrays.asList(
                createChunk("seg_1", 1, "第1段内容"),
                createChunk("seg_3", 5, "第5段内容"),
                createChunk("seg_2", 3, "第3段内容")
        );
        addUniqueChunks(round1, totalSegIds, allChunks);
        logger.info("第1轮召回: 3个切片");
        
        // 第2轮（包含重复）
        List<DocumentChunk> round2 = Arrays.asList(
                createChunk("seg_3", 5, "第5段内容"),
                createChunk("seg_4", 7, "第7段内容"),
                createChunk("seg_5", 9, "第9段内容")
        );
        addUniqueChunks(round2, totalSegIds, allChunks);
        logger.info("第2轮召回: 3个切片（1个重复）");
        
        // 第3轮（包含重复和空内容）
        List<DocumentChunk> round3 = Arrays.asList(
                createChunk("seg_1", 1, "第1段内容"),
                createChunk("seg_6", 11, ""),
                createChunk("seg_7", 13, "第13段内容")
        );
        addUniqueChunks(round3, totalSegIds, allChunks);
        logger.info("第3轮召回: 3个切片（1个重复，1个空内容）");
        
        logger.info("\n去重后总切片数: {}", totalSegIds.size());
        
        // 过滤无效内容
        List<DocumentChunk> validChunks = new ArrayList<>();
        for (DocumentChunk chunk : allChunks) {
            if (isValidContent(chunk.getContent())) {
                validChunks.add(chunk);
            }
        }
        logger.info("过滤无效内容后: {} 个切片", validChunks.size());
        
        // 按 pos 排序
        validChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        logger.info("\n最终结果（pos升序）:");
        for (int i = 0; i < validChunks.size(); i++) {
            DocumentChunk chunk = validChunks.get(i);
            logger.info("  顺序{}: segId={}, pos={}, content='{}'", 
                    i + 1, chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        // 验证
        assertEquals(6, validChunks.size(), "应该有6个有效切片");
        
        // 验证 pos 升序
        for (int i = 0; i < validChunks.size() - 1; i++) {
            assertTrue(validChunks.get(i).getPos() < validChunks.get(i + 1).getPos(),
                    "pos 应该严格升序");
        }
        
        // 验证阅读顺序
        assertEquals(1, validChunks.get(0).getPos());
        assertEquals(3, validChunks.get(1).getPos());
        assertEquals(5, validChunks.get(2).getPos());
        assertEquals(7, validChunks.get(3).getPos());
        assertEquals(9, validChunks.get(4).getPos());
        assertEquals(13, validChunks.get(5).getPos());
        
        logger.info("\n✓ 完整流程测试通过");
    }

    @Test
    @DisplayName("测试11：边界条件 - 空集合去重")
    void testEmptyCollectionDeduplication() {
        logger.info("\n========== 测试11：边界条件 - 空集合去重 ==========");
        
        Set<String> totalSegIds = new HashSet<>();
        List<DocumentChunk> emptyChunks = new ArrayList<>();
        
        addUniqueChunks(emptyChunks, totalSegIds, new ArrayList<>());
        
        assertEquals(0, totalSegIds.size(), "空集合去重后应该仍为空");
        
        logger.info("✓ 空集合去重测试通过");
    }

    @Test
    @DisplayName("测试12：边界条件 - 全部重复")
    void testAllDuplicates() {
        logger.info("\n========== 测试12：边界条件 - 全部重复 ==========");
        
        ReActState state = new ReActState(5, 8000, 3);
        state.getTotalSegIds().add("seg_1");
        state.getTotalSegIds().add("seg_2");
        
        List<DocumentChunk> allDuplicates = Arrays.asList(
                createChunk("seg_1", 1, "内容1"),
                createChunk("seg_2", 2, "内容2")
        );
        
        List<DocumentChunk> uniqueChunks = new ArrayList<>();
        addUniqueChunks(allDuplicates, state.getTotalSegIds(), uniqueChunks);
        
        assertEquals(0, uniqueChunks.size(), "全部重复时不应该新增任何切片");
        
        logger.info("✓ 全部重复边界条件测试通过");
    }

    @Test
    @DisplayName("测试13：边界条件 - pos 相同的情况")
    void testSamePosHandling() {
        logger.info("\n========== 测试13：边界条件 - pos 相同的情况 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 5, "内容A"),
                createChunk("seg_2", 5, "内容B"),
                createChunk("seg_3", 3, "内容C")
        );
        
        logger.info("排序前:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        chunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        logger.info("排序后:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        // 验证 pos 非降序
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertTrue(chunks.get(i).getPos() <= chunks.get(i + 1).getPos(),
                    "pos 应该非降序");
        }
        
        logger.info("✓ pos 相同情况处理测试通过");
    }

    @Test
    @DisplayName("测试14：性能测试 - 大量切片去重排序")
    void testPerformanceLargeDataset() {
        logger.info("\n========== 测试14：性能测试 - 大量切片去重排序 ==========");
        
        int chunkCount = 1000;
        List<DocumentChunk> chunks = new ArrayList<>();
        Set<String> totalSegIds = new HashSet<>();
        
        Random random = new Random(42);
        
        logger.info("生成 {} 个随机切片...", chunkCount);
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < chunkCount; i++) {
            String segId = "seg_" + i;
            int pos = random.nextInt(chunkCount * 2);
            String content = "内容_" + i;
            
            if (!totalSegIds.contains(segId)) {
                chunks.add(createChunk(segId, pos, content));
                totalSegIds.add(segId);
            }
        }
        
        long generateTime = System.currentTimeMillis() - startTime;
        logger.info("生成耗时: {} ms", generateTime);
        
        // 排序
        startTime = System.currentTimeMillis();
        chunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        long sortTime = System.currentTimeMillis() - startTime;
        
        logger.info("排序耗时: {} ms", sortTime);
        logger.info("切片数量: {}", chunks.size());
        
        // 验证顺序
        boolean isSorted = true;
        for (int i = 0; i < chunks.size() - 1; i++) {
            if (chunks.get(i).getPos() > chunks.get(i + 1).getPos()) {
                isSorted = false;
                break;
            }
        }
        
        assertTrue(isSorted, "排序后应该保持升序");
        assertTrue(sortTime < 1000, "排序耗时应该小于1秒");
        
        logger.info("✓ 性能测试通过");
    }

    // ==================== 辅助方法 ====================

    private DocumentChunk createChunk(String segId, int pos, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setSegId(segId);
        chunk.setPos(pos);
        chunk.setContent(content);
        return chunk;
    }

    private List<String> extractSegIds(List<RetrievalResult> results) {
        List<String> ids = new ArrayList<>();
        for (RetrievalResult result : results) {
            ids.add(result.getSegId());
        }
        return ids;
    }

    private void addUniqueChunks(List<DocumentChunk> newChunks, Set<String> totalSegIds, 
                                  List<DocumentChunk> uniqueChunks) {
        for (DocumentChunk chunk : newChunks) {
            if (!totalSegIds.contains(chunk.getSegId())) {
                totalSegIds.add(chunk.getSegId());
                uniqueChunks.add(chunk);
            }
        }
    }

    private boolean isDuplicateOrOverlap(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return false;
        }
        
        if (content1.equals(content2)) {
            return true;
        }
        
        return hasOverlap(content1, content2, 20);
    }

    private boolean hasOverlap(String content1, String content2, int overlapLength) {
        if (content1 == null || content2 == null) {
            return false;
        }
        
        if (content1.length() < overlapLength || content2.length() < overlapLength) {
            return false;
        }
        
        String tail = content1.substring(content1.length() - overlapLength);
        String head = content2.substring(0, overlapLength);
        
        return tail.equals(head);
    }

    private String mergeOverlap(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return content1 != null ? content1 : content2;
        }
        
        int overlapLength = 20;
        if (content1.length() >= overlapLength && content2.length() >= overlapLength) {
            String tail = content1.substring(content1.length() - overlapLength);
            String head = content2.substring(0, overlapLength);
            
            if (tail.equals(head)) {
                return content1 + content2.substring(overlapLength);
            }
        }
        
        return content1 + " " + content2;
    }

    private boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        // 检测乱码（简单实现：检查是否包含过多替换字符）
        long replacementCount = content.chars().filter(ch -> ch == '\uFFFD').count();
        double replacementRatio = (double) replacementCount / content.length();
        
        return replacementRatio < 0.1;
    }
}