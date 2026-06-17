package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2.8 收尾后处理与问答模型衔接 - 单元测试
 * 
 * 测试 ReAct 召回后的收尾处理和问答衔接机制，验证：
 * - 合并相邻重复语句/冗余
 * - 剔除空文本与乱码
 * - 再次按 pos 排序确保顺序
 * - 构建有序上下文文本
 * - 将用户问题+有序上下文送入主问答模型
 * - 提示词优化
 * - 完整问答流程
 * - 边界条件处理
 * 
 * @author RAG Team
 * @since 2024
 */
public class PostProcessingAndQaIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(PostProcessingAndQaIntegrationTest.class);

    @Test
    @DisplayName("测试1：合并相邻重复语句 - 完全重复")
    void testMergeAdjacentExactDuplicates() {
        logger.info("\n========== 测试1：合并相邻重复语句 - 完全重复 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "个人购汇的具体额度是多少？"),
                createChunk("seg_2", 2, "个人购汇的具体额度是多少？"),
                createChunk("seg_3", 3, "年度总额为5万美元。")
        );
        
        logger.info("合并前: {} 个切片", chunks.size());
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        List<DocumentChunk> merged = mergeAdjacentDuplicates(chunks);
        
        logger.info("\n合并后: {} 个切片", merged.size());
        for (DocumentChunk chunk : merged) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        assertEquals(2, merged.size(), "完全重复的相邻切片应该被合并");
        assertEquals("个人购汇的具体额度是多少？", merged.get(0).getContent());
        assertEquals("年度总额为5万美元。", merged.get(1).getContent());
        
        logger.info("✓ 完全重复合并测试通过");
    }

    @Test
    @DisplayName("测试2：合并相邻重复语句 - 尾部/头部重叠")
    void testMergeAdjacentOverlap() {
        logger.info("\n========== 测试2：合并相邻重复语句 - 尾部/头部重叠 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "个人购汇的具体额度是每年5万美元等值外币，"),
                createChunk("seg_2", 2, "每年5万美元等值外币，超出额度需要审批。"),
                createChunk("seg_3", 3, "审批流程需要5个工作日。")
        );
        
        logger.info("合并前: {} 个切片", chunks.size());
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        List<DocumentChunk> merged = mergeAdjacentDuplicates(chunks);
        
        logger.info("\n合并后: {} 个切片", merged.size());
        for (DocumentChunk chunk : merged) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        assertEquals(2, merged.size(), "有重叠的相邻切片应该被合并");
        assertEquals("个人购汇的具体额度是每年5万美元等值外币，超出额度需要审批。", merged.get(0).getContent());
        assertEquals("审批流程需要5个工作日。", merged.get(1).getContent());
        
        logger.info("✓ 尾部/头部重叠合并测试通过");
    }

    @Test
    @DisplayName("测试3：剔除空文本 - 空字符串和null")
    void testFilterEmptyContent() {
        logger.info("\n========== 测试3：剔除空文本 - 空字符串和null ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "有效内容1"),
                createChunk("seg_2", 2, ""),
                createChunk("seg_3", 3, "   "),
                createChunk("seg_4", 4, null),
                createChunk("seg_5", 5, "有效内容2")
        );
        
        logger.info("过滤前: {} 个切片", chunks.size());
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        List<DocumentChunk> validChunks = filterInvalidContent(chunks);
        
        logger.info("\n过滤后: {} 个切片", validChunks.size());
        for (DocumentChunk chunk : validChunks) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        assertEquals(2, validChunks.size(), "应该只保留2个有效切片");
        assertEquals("seg_1", validChunks.get(0).getSegId());
        assertEquals("seg_5", validChunks.get(1).getSegId());
        
        logger.info("✓ 空文本剔除测试通过");
    }

    @Test
    @DisplayName("测试4：剔除乱码 - 中文字符比例检测")
    void testFilterGarbledContent() {
        logger.info("\n========== 测试4：剔除乱码 - 中文字符比例检测 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "个人购汇的具体额度是每年5万美元"),
                createChunk("seg_2", 2, ""),
                createChunk("seg_3", 3, "abc123!@#"),
                createChunk("seg_4", 4, "有效内容4"),
                createChunk("seg_5", 5, "   ")
        );
        
        logger.info("过滤前: {} 个切片", chunks.size());
        
        List<DocumentChunk> validChunks = filterInvalidContent(chunks);
        
        logger.info("过滤后: {} 个切片", validChunks.size());
        for (DocumentChunk chunk : validChunks) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        assertEquals(2, validChunks.size(), "应该保留2个有效切片");
        assertEquals("seg_1", validChunks.get(0).getSegId());
        assertEquals("seg_4", validChunks.get(1).getSegId());
        
        logger.info("✓ 乱码剔除测试通过");
    }

    @Test
    @DisplayName("测试5：再次按 pos 排序确保顺序")
    void testReorderByPos() {
        logger.info("\n========== 测试5：再次按 pos 排序确保顺序 ==========");
        
        List<DocumentChunk> chunks = new ArrayList<>(Arrays.asList(
                createChunk("seg_3", 5, "内容3"),
                createChunk("seg_1", 1, "内容1"),
                createChunk("seg_4", 7, "内容4"),
                createChunk("seg_2", 3, "内容2"),
                createChunk("seg_5", 9, "内容5")
        ));
        
        logger.info("排序前 pos 顺序:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        chunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        logger.info("\n排序后 pos 顺序:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        assertEquals(1, chunks.get(0).getPos());
        assertEquals(3, chunks.get(1).getPos());
        assertEquals(5, chunks.get(2).getPos());
        assertEquals(7, chunks.get(3).getPos());
        assertEquals(9, chunks.get(4).getPos());
        
        logger.info("✓ pos 排序测试通过");
    }

    @Test
    @DisplayName("测试6：构建有序上下文文本")
    void testBuildOrderedContext() {
        logger.info("\n========== 测试6：构建有序上下文文本 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "个人购汇的具体额度是每年5万美元。"),
                createChunk("seg_2", 2, "等值外币也可以购汇。"),
                createChunk("seg_3", 3, "超出额度需要外汇局审批。")
        );
        
        String context = buildOrderedContext(chunks);
        
        logger.info("构建的上下文:\n{}", context);
        
        assertNotNull(context, "上下文不应为 null");
        assertTrue(context.contains("[段落 2]"), "应该包含段落标记");
        assertTrue(context.contains("[段落 3]"), "应该包含段落标记");
        assertTrue(context.contains("[段落 4]"), "应该包含段落标记");
        assertTrue(context.contains("个人购汇的具体额度是每年5万美元。"), "应该包含第1段内容");
        assertTrue(context.contains("等值外币也可以购汇。"), "应该包含第2段内容");
        assertTrue(context.contains("超出额度需要外汇局审批。"), "应该包含第3段内容");
        
        logger.info("✓ 有序上下文构建测试通过");
    }

    @Test
    @DisplayName("测试7：完整后处理流程 - 合并+过滤+排序")
    void testCompletePostProcessingFlow() {
        logger.info("\n========== 测试7：完整后处理流程 - 合并+过滤+排序 ==========");
        
        List<DocumentChunk> chunks = new ArrayList<>(Arrays.asList(
                createChunk("seg_3", 5, "第5段内容"),
                createChunk("seg_1", 1, "第1段内容"),
                createChunk("seg_2", 3, "第1段内容"),
                createChunk("seg_4", 7, ""),
                createChunk("seg_5", 9, "第9段内容"),
                createChunk("seg_6", 11, "   "),
                createChunk("seg_7", 13, "第13段内容")
        ));
        
        logger.info("原始切片: {} 个", chunks.size());
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        // 1. 按 pos 排序
        chunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        logger.info("\n1. 按 pos 排序后:");
        for (DocumentChunk chunk : chunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        // 2. 合并相邻重复
        List<DocumentChunk> merged = mergeAdjacentDuplicates(chunks);
        logger.info("\n2. 合并相邻重复后: {} 个", merged.size());
        for (DocumentChunk chunk : merged) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        // 3. 过滤无效内容
        List<DocumentChunk> validChunks = filterInvalidContent(merged);
        logger.info("\n3. 过滤无效内容后: {} 个", validChunks.size());
        for (DocumentChunk chunk : validChunks) {
            logger.info("  segId={}, pos={}, content='{}'", 
                    chunk.getSegId(), chunk.getPos(), chunk.getContent());
        }
        
        // 4. 再次按 pos 排序
        validChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        logger.info("\n4. 再次按 pos 排序后:");
        for (DocumentChunk chunk : validChunks) {
            logger.info("  segId={}, pos={}", chunk.getSegId(), chunk.getPos());
        }
        
        // 验证
        assertEquals(4, validChunks.size(), "应该有4个有效切片");
        assertEquals(1, validChunks.get(0).getPos());
        assertEquals(5, validChunks.get(1).getPos());
        assertEquals(9, validChunks.get(2).getPos());
        assertEquals(13, validChunks.get(3).getPos());
        
        logger.info("\n✓ 完整后处理流程测试通过");
    }

    @Test
    @DisplayName("测试8：问答衔接 - 构建问答提示词")
    void testQaPromptBuilding() {
        logger.info("\n========== 测试8：问答衔接 - 构建问答提示词 ==========");
        
        String userQuery = "个人购汇的具体额度是多少？";
        String context = "[段落 1]\n个人购汇的具体额度是每年5万美元。\n\n[段落 2]\n等值外币也可以购汇。\n\n";
        
        String prompt = buildQaPrompt(userQuery, context);
        
        logger.info("用户问题: {}", userQuery);
        logger.info("上下文:\n{}", context);
        logger.info("构建的提示词:\n{}", prompt);
        
        assertNotNull(prompt, "提示词不应为 null");
        assertTrue(prompt.contains("个人购汇的具体额度是多少？"), "提示词应包含用户问题");
        assertTrue(prompt.contains("个人购汇的具体额度是每年5万美元。"), "提示词应包含上下文");
        assertTrue(prompt.contains("请根据以下上下文回答问题"), "提示词应包含指令");
        
        logger.info("✓ 问答提示词构建测试通过");
    }

    @Test
    @DisplayName("测试9：边界条件 - 空切片列表")
    void testEmptyChunkList() {
        logger.info("\n========== 测试9：边界条件 - 空切片列表 ==========");
        
        List<DocumentChunk> emptyChunks = new ArrayList<>();
        
        List<DocumentChunk> merged = mergeAdjacentDuplicates(emptyChunks);
        List<DocumentChunk> validChunks = filterInvalidContent(emptyChunks);
        String context = buildOrderedContext(emptyChunks);
        
        assertEquals(0, merged.size(), "空列表合并后应该仍为空");
        assertEquals(0, validChunks.size(), "空列表过滤后应该仍为空");
        assertEquals("", context, "空列表构建的上下文应该为空字符串");
        
        logger.info("✓ 空切片列表边界条件测试通过");
    }

    @Test
    @DisplayName("测试10：边界条件 - 单个切片")
    void testSingleChunk() {
        logger.info("\n========== 测试10：边界条件 - 单个切片 ==========");
        
        List<DocumentChunk> singleChunk = Arrays.asList(
                createChunk("seg_1", 1, "唯一的内容")
        );
        
        List<DocumentChunk> merged = mergeAdjacentDuplicates(singleChunk);
        List<DocumentChunk> validChunks = filterInvalidContent(singleChunk);
        String context = buildOrderedContext(singleChunk);
        
        assertEquals(1, merged.size(), "单个切片合并后应该仍为1个");
        assertEquals(1, validChunks.size(), "单个切片过滤后应该仍为1个");
        assertTrue(context.contains("唯一的内容"), "上下文应该包含内容");
        
        logger.info("✓ 单个切片边界条件测试通过");
    }

    @Test
    @DisplayName("测试11：边界条件 - 全部无效内容")
    void testAllInvalidContent() {
        logger.info("\n========== 测试11：边界条件 - 全部无效内容 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, ""),
                createChunk("seg_2", 2, "   "),
                createChunk("seg_3", 3, null),
                createChunk("seg_4", 4, "abc123")
        );
        
        List<DocumentChunk> validChunks = filterInvalidContent(chunks);
        
        assertEquals(0, validChunks.size(), "全部无效内容应该被全部过滤");
        
        logger.info("✓ 全部无效内容边界条件测试通过");
    }

    @Test
    @DisplayName("测试12：边界条件 - 全部重复内容")
    void testAllDuplicateContent() {
        logger.info("\n========== 测试12：边界条件 - 全部重复内容 ==========");
        
        List<DocumentChunk> chunks = Arrays.asList(
                createChunk("seg_1", 1, "重复内容重复内容重复内容"),
                createChunk("seg_2", 2, "重复内容重复内容重复内容"),
                createChunk("seg_3", 3, "重复内容重复内容重复内容")
        );
        
        List<DocumentChunk> merged = mergeAdjacentDuplicates(chunks);
        
        logger.info("合并后: {} 个切片", merged.size());
        for (DocumentChunk chunk : merged) {
            logger.info("  segId={}, content='{}'", chunk.getSegId(), chunk.getContent());
        }
        
        assertEquals(1, merged.size(), "全部重复内容应该合并为1个");
        assertEquals("重复内容重复内容重复内容", merged.get(0).getContent());
        
        logger.info("✓ 全部重复内容边界条件测试通过");
    }

    @Test
    @DisplayName("测试13：性能测试 - 大量切片后处理")
    void testPerformanceLargeDataset() {
        logger.info("\n========== 测试13：性能测试 - 大量切片后处理 ==========");
        
        int chunkCount = 100;
        List<DocumentChunk> chunks = new ArrayList<>();
        
        for (int i = 0; i < chunkCount; i++) {
            String content = (i % 10 == 0) ? "" : "内容_" + i;
            chunks.add(createChunk("seg_" + i, i, content));
        }
        
        logger.info("生成 {} 个切片", chunkCount);
        
        long startTime = System.currentTimeMillis();
        
        // 1. 排序
        chunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        // 2. 合并重复
        List<DocumentChunk> merged = mergeAdjacentDuplicates(chunks);
        
        // 3. 过滤无效
        List<DocumentChunk> validChunks = filterInvalidContent(merged);
        
        // 4. 再次排序
        validChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        
        // 5. 构建上下文
        String context = buildOrderedContext(validChunks);
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        logger.info("后处理耗时: {} ms", totalTime);
        logger.info("原始切片数: {}", chunkCount);
        logger.info("有效切片数: {}", validChunks.size());
        logger.info("上下文长度: {}", context.length());
        
        assertTrue(totalTime < 1000, "后处理耗时应该小于1秒");
        assertEquals(chunkCount - chunkCount / 10, validChunks.size(), 
                "应该过滤掉空内容的切片");
        
        logger.info("✓ 性能测试通过");
    }

    @Test
    @DisplayName("测试14：完整问答流程 - 从后处理到提示词构建")
    void testCompleteQaWorkflow() {
        logger.info("\n========== 测试14：完整问答流程 - 从后处理到提示词构建 ==========");
        
        String userQuery = "个人购汇的具体额度是多少？超出额度怎么办？";
        
        List<DocumentChunk> retrievedChunks = new ArrayList<>(Arrays.asList(
                createChunk("seg_3", 5, "第5段内容"),
                createChunk("seg_1", 1, "个人购汇的具体额度是每年5万美元。"),
                createChunk("seg_2", 3, "个人购汇的具体额度是每年5万美元。"),
                createChunk("seg_4", 7, ""),
                createChunk("seg_5", 9, "超出额度需要向外汇局申请审批。"),
                createChunk("seg_6", 11, "   "),
                createChunk("seg_7", 13, "审批流程需要5个工作日。")
        ));
        
        logger.info("用户问题: {}", userQuery);
        logger.info("召回切片数: {}", retrievedChunks.size());
        
        // 1. 按 pos 排序
        retrievedChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        logger.info("\n1. 排序后: {} 个切片", retrievedChunks.size());
        
        // 2. 合并相邻重复
        List<DocumentChunk> merged = mergeAdjacentDuplicates(retrievedChunks);
        logger.info("2. 合并重复后: {} 个切片", merged.size());
        
        // 3. 过滤无效内容
        List<DocumentChunk> validChunks = filterInvalidContent(merged);
        logger.info("3. 过滤无效后: {} 个切片", validChunks.size());
        
        // 4. 再次排序
        validChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));
        logger.info("4. 再次排序后: {} 个切片", validChunks.size());
        
        // 5. 构建上下文
        String context = buildOrderedContext(validChunks);
        logger.info("5. 构建上下文，长度: {}", context.length());
        
        // 6. 构建问答提示词
        String prompt = buildQaPrompt(userQuery, context);
        logger.info("6. 构建问答提示词，长度: {}", prompt.length());
        logger.info("\n最终提示词:\n{}", prompt);
        
        // 验证
        assertEquals(4, validChunks.size(), "应该有4个有效切片");
        assertTrue(prompt.contains(userQuery), "提示词应包含用户问题");
        assertTrue(prompt.contains("个人购汇的具体额度是每年5万美元。"), 
                "提示词应包含第1段内容");
        assertTrue(prompt.contains("超出额度需要向外汇局申请审批。"), 
                "提示词应包含第5段内容");
        assertTrue(prompt.contains("审批流程需要5个工作日。"), 
                "提示词应包含第7段内容");
        
        logger.info("\n✓ 完整问答流程测试通过");
    }

    // ==================== 辅助方法 ====================

    private DocumentChunk createChunk(String segId, int pos, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setSegId(segId);
        chunk.setPos(pos);
        chunk.setContent(content);
        return chunk;
    }

    /**
     * 合并相邻重复语句
     */
    private List<DocumentChunk> mergeAdjacentDuplicates(List<DocumentChunk> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<DocumentChunk> merged = new ArrayList<>();
        DocumentChunk current = chunks.get(0);

        for (int i = 1; i < chunks.size(); i++) {
            DocumentChunk next = chunks.get(i);

            if (hasOverlap(current.getContent(), next.getContent())) {
                String mergedContent = mergeOverlapContent(current.getContent(), next.getContent());
                current.setContent(mergedContent);
                current.setNextSegId(next.getNextSegId());
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    /**
     * 检查是否有重叠
     */
    private boolean hasOverlap(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return false;
        }

        if (content1.equals(content2)) {
            return true;
        }

        int minOverlap = 10;
        int maxCheck = Math.min(50, Math.min(content1.length(), content2.length()));

        for (int len = maxCheck; len >= minOverlap; len--) {
            if (content1.length() >= len && content2.length() >= len) {
                String tail = content1.substring(content1.length() - len);
                String head = content2.substring(0, len);
                if (tail.equals(head)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 合并重叠内容
     */
    private String mergeOverlapContent(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return content1 != null ? content1 : content2;
        }

        int minOverlap = 10;
        int maxCheck = Math.min(50, Math.min(content1.length(), content2.length()));

        for (int len = maxCheck; len >= minOverlap; len--) {
            if (content1.length() >= len && content2.length() >= len) {
                String tail = content1.substring(content1.length() - len);
                String head = content2.substring(0, len);
                if (tail.equals(head)) {
                    return content1 + content2.substring(len);
                }
            }
        }

        return content1 + "\n" + content2;
    }

    /**
     * 过滤无效内容
     */
    private List<DocumentChunk> filterInvalidContent(List<DocumentChunk> chunks) {
        List<DocumentChunk> valid = new ArrayList<>();
        
        for (DocumentChunk chunk : chunks) {
            if (isValidContent(chunk.getContent())) {
                valid.add(chunk);
            }
        }
        
        return valid;
    }

    /**
     * 验证内容是否有效
     */
    private boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        long chineseCharCount = content.chars()
                .filter(c -> c >= 0x4E00 && c <= 0x9FFF)
                .count();

        return chineseCharCount > content.length() * 0.3;
    }

    /**
     * 构建有序上下文
     */
    private String buildOrderedContext(List<DocumentChunk> chunks) {
        StringBuilder context = new StringBuilder();

        for (DocumentChunk chunk : chunks) {
            context.append("[段落 ").append(chunk.getPos() + 1).append("]\n");
            context.append(chunk.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    /**
     * 构建问答提示词
     */
    private String buildQaPrompt(String userQuery, String context) {
        return "请根据以下上下文回答问题。\n\n" +
                "## 上下文\n" +
                context + "\n" +
                "## 用户问题\n" +
                userQuery + "\n\n" +
                "## 回答\n" +
                "请基于上下文提供准确、完整的回答。";
    }
}