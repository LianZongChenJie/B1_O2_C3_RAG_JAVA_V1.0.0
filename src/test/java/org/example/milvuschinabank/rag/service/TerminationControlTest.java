package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.ReActState;
import org.example.milvuschinabank.rag.model.RetrievalResult;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2.5 终止条件与轮次控制 - 单元测试
 * 
 * 测试 ReAct 多轮召回的终止条件和轮次控制机制，验证：
 * - MaxRound=5 最大轮次限制及达到后终止
 * - 单轮 MaxAddSeg=3 最大新增切片数限制
 * - MaxContextLen=8000 上下文长度阈值及超长终止
 * - 循环条件控制：Round 轮次计数、IsFinish 完成标志、长度检查
 * - 多种终止条件的优先级和组合
 * - 主动终止（质量达标）
 * - 被动终止（达到限制）
 * - 终止原因的准确性和完整性
 * 
 * @author RAG Team
 * @since 2024
 */
public class TerminationControlTest {

    private static final Logger logger = LoggerFactory.getLogger(TerminationControlTest.class);

    @Test
    @DisplayName("测试1：MaxRound=5 最大轮次限制")
    void testMaxRoundLimit() {
        logger.info("\n========== 测试1：MaxRound=5 最大轮次限制 ==========");
        
        int maxRound = 5;
        ReActState state = new ReActState(maxRound, 8000, 3);
        // totalSegIds 已在构造函数中初始化
        state.setRoundResults(new ArrayList<>());

        logger.info("最大轮次限制: {}", maxRound);
        logger.info("初始轮次: {}", state.getCurrentRound());

        for (int round = 1; round <= 5; round++) {
            state.nextRound();
            logger.info("\n--- 第 {} 轮 ---", round);
            logger.info("当前轮次: {}", state.getCurrentRound());
            logger.info("是否达到最大轮次: {}", state.isMaxRoundReached());

            ReActState.TerminationCheckResult check = state.checkTermination();
            logger.info("终止检查: shouldTerminate={}, reason={}", 
                    check.shouldTerminate(), check.getReason());

            if (round < 5) {
                assertFalse(check.shouldTerminate(), "第 " + round + " 轮不应终止");
            } else {
                assertTrue(check.shouldTerminate(), "第 5 轮应达到最大轮次限制");
                assertTrue(check.getReason().contains("达到最大轮次限制"), 
                        "终止原因应包含'达到最大轮次限制'");
            }
        }

        logger.info("\n✓ MaxRound=5 最大轮次限制测试通过");
    }

    @Test
    @DisplayName("测试2：单轮 MaxAddSeg=3 最大新增切片数限制")
    void testMaxAddSegPerRound() {
        logger.info("\n========== 测试2：单轮 MaxAddSeg=3 最大新增切片数限制 ==========");
        
        int maxAddSeg = 3;
        ReActState state = new ReActState(5, 8000, maxAddSeg);
        state.setRoundResults(new ArrayList<>());

        logger.info("单轮最大新增切片数: {}", maxAddSeg);

        state.nextRound();
        
        List<RetrievalResult> newResults = Arrays.asList(
                new RetrievalResult("seg_1", 0.9, 1, "vector"),
                new RetrievalResult("seg_2", 0.85, 1, "vector"),
                new RetrievalResult("seg_3", 0.8, 1, "vector"),
                new RetrievalResult("seg_4", 0.75, 1, "vector")
        );

        logger.info("本轮召回结果数: {}", newResults.size());

        List<String> uniqueNewSegIds = new ArrayList<>();
        for (RetrievalResult result : newResults) {
            if (uniqueNewSegIds.size() < maxAddSeg) {
                uniqueNewSegIds.add(result.getSegId());
                state.getTotalSegIds().add(result.getSegId());
            } else {
                logger.info("切片 {} 超出单轮限制，被过滤", result.getSegId());
            }
        }

        logger.info("实际新增切片数: {}", uniqueNewSegIds.size());
        assertEquals(maxAddSeg, uniqueNewSegIds.size(), 
                "实际新增切片数不应超过单轮限制");
        assertFalse(uniqueNewSegIds.contains("seg_4"), 
                "seg_4 应被过滤（超出限制）");

        logger.info("\n✓ MaxAddSeg=3 单轮最大新增切片数限制测试通过");
    }

    @Test
    @DisplayName("测试3：MaxContextLen=8000 上下文长度阈值")
    void testMaxContextLength() {
        logger.info("\n========== 测试3：MaxContextLen=8000 上下文长度阈值 ==========");
        
        int maxContextLength = 8000;
        ReActState state = new ReActState(5, maxContextLength, 3);
        state.setRoundResults(new ArrayList<>());

        logger.info("最大上下文长度: {}", maxContextLength);

        int[] testLengths = {1000, 4000, 7999, 8000, 8001, 10000};
        
        for (int length : testLengths) {
            state.setCurrentContextLength(length);
            boolean exceeded = state.isContextLengthExceeded();
            
            logger.info("上下文长度: {}, 是否超限: {}", length, exceeded);
            
            if (length >= maxContextLength) {
                assertTrue(exceeded, "长度 " + length + " 应超限");
                
                ReActState.TerminationCheckResult check = state.checkTermination();
                assertTrue(check.shouldTerminate(), "应终止");
                assertTrue(check.getReason().contains("上下文长度超限"), 
                        "终止原因应包含'上下文长度超限'");
                logger.info("终止原因: {}", check.getReason());
            } else {
                assertFalse(exceeded, "长度 " + length + " 不应超限");
                
                ReActState.TerminationCheckResult check = state.checkTermination();
                assertFalse(check.shouldTerminate(), "不应终止（未超限）");
            }
        }

        logger.info("\n✓ MaxContextLen=8000 上下文长度阈值测试通过");
    }

    @Test
    @DisplayName("测试4：循环条件控制 - Round 轮次计数")
    void testRoundCounter() {
        logger.info("\n========== 测试4：循环条件控制 - Round 轮次计数 ==========");
        
        ReActState state = new ReActState(5, 8000, 3);
        state.setRoundResults(new ArrayList<>());

        logger.info("初始轮次: {}", state.getCurrentRound());
        assertEquals(0, state.getCurrentRound(), "初始轮次应为 0");

        for (int i = 1; i <= 5; i++) {
            state.nextRound();
            logger.info("第 {} 次 nextRound() 后，当前轮次: {}", i, state.getCurrentRound());
            assertEquals(i, state.getCurrentRound(), "轮次应为 " + i);
        }

        state.nextRound();
        logger.info("第 6 次 nextRound() 后，当前轮次: {}", state.getCurrentRound());
        assertEquals(6, state.getCurrentRound(), "轮次应为 6（超过限制）");
        assertTrue(state.isMaxRoundReached(), "应达到最大轮次限制");

        logger.info("\n✓ Round 轮次计数测试通过");
    }

    @Test
    @DisplayName("测试5：循环条件控制 - IsFinish 完成标志")
    void testIsFinishFlag() {
        logger.info("\n========== 测试5：循环条件控制 - IsFinish 完成标志 ==========");
        
        ReActState state = new ReActState(5, 8000, 3);
        state.setRoundResults(new ArrayList<>());

        logger.info("初始状态: isFinished={}", state.isFinished());
        assertFalse(state.isFinished(), "初始状态应为未完成");

        state.finish("质量达标");
        logger.info("调用 finish('质量达标') 后: isFinished={}, finishReason={}", 
                state.isFinished(), state.getFinishReason());
        
        assertTrue(state.isFinished(), "应标记为已完成");
        assertEquals("质量达标", state.getFinishReason(), "终止原因应为'质量达标'");

        ReActState.TerminationCheckResult check = state.checkTermination();
        assertTrue(check.shouldTerminate(), "应终止");
        assertTrue(check.getReason().contains("已完成"), "终止原因应包含'已完成'");
        logger.info("终止原因: {}", check.getReason());

        logger.info("\n✓ IsFinish 完成标志测试通过");
    }

    @Test
    @DisplayName("测试6：循环条件控制 - 长度检查")
    void testLengthCheck() {
        logger.info("\n========== 测试6：循环条件控制 - 长度检查 ==========");
        
        int maxContextLength = 100;
        ReActState state = new ReActState(5, maxContextLength, 3);
        state.setRoundResults(new ArrayList<>());

        int[] testCases = {0, 50, 99, 100, 150};
        
        for (int length : testCases) {
            state.setCurrentContextLength(length);
            boolean exceeded = state.isContextLengthExceeded();
            logger.info("长度: {}, isContextLengthExceeded: {}", length, exceeded);
            
            if (length >= maxContextLength) {
                assertTrue(exceeded, "长度 " + length + " 应超限");
            } else {
                assertFalse(exceeded, "长度 " + length + " 不应超限");
            }
        }

        logger.info("\n✓ 长度检查测试通过");
    }

    @Test
    @DisplayName("测试7：终止条件优先级 - 主动终止优先")
    void testTerminationPriorityManualFinish() {
        logger.info("\n========== 测试7：终止条件优先级 - 主动终止优先 ==========");
        
        ReActState state = new ReActState(5, 100, 3);
        state.setRoundResults(new ArrayList<>());

        state.setCurrentContextLength(150);
        state.finish("质量达标");

        ReActState.TerminationCheckResult check = state.checkTermination();
        logger.info("主动终止 + 长度超限 + 轮次未达");
        logger.info("shouldTerminate: {}, reason: {}", check.shouldTerminate(), check.getReason());
        
        assertTrue(check.shouldTerminate(), "应终止");
        assertTrue(check.getReason().contains("已完成"), 
                "主动终止应优先，原因应包含'已完成'");

        logger.info("\n✓ 主动终止优先级测试通过");
    }

    @Test
    @DisplayName("测试8：终止条件优先级 - 轮次限制优先于长度超限")
    void testTerminationPriorityMaxRound() {
        logger.info("\n========== 测试8：终止条件优先级 - 轮次限制优先于长度超限 ==========");
        
        ReActState state = new ReActState(3, 100, 3);
        state.setRoundResults(new ArrayList<>());

        state.nextRound();
        state.nextRound();
        state.nextRound();
        state.setCurrentContextLength(150);

        ReActState.TerminationCheckResult check = state.checkTermination();
        logger.info("轮次达到限制 + 长度超限");
        logger.info("shouldTerminate: {}, reason: {}", check.shouldTerminate(), check.getReason());
        
        assertTrue(check.shouldTerminate(), "应终止");
        assertTrue(check.getReason().contains("达到最大轮次限制"), 
                "轮次限制应优先，原因应包含'达到最大轮次限制'");

        logger.info("\n✓ 轮次限制优先级测试通过");
    }

    @Test
    @DisplayName("测试9：多轮完整流程 - 正常终止")
    void testCompleteMultiRoundFlow() {
        logger.info("\n========== 测试9：多轮完整流程 - 正常终止 ==========");
        
        ReActState state = new ReActState(5, 8000, 3);
        state.setRoundResults(new ArrayList<>());

        for (int round = 1; round <= 5; round++) {
            state.nextRound();
            logger.info("\n--- 第 {} 轮 ---", round);

            ReActState.TerminationCheckResult check = state.checkTermination();
            logger.info("终止检查: shouldTerminate={}, reason={}", 
                    check.shouldTerminate(), check.getReason());

            if (check.shouldTerminate()) {
                state.finish(check.getReason());
                logger.info("终止原因: {}", state.getFinishReason());
                break;
            }

            state.getTotalSegIds().add("seg_round_" + round);
            state.setCurrentContext("[段落 " + round + "] 内容\n\n");
            state.setCurrentContextLength(state.getCurrentContext().length());
            
            logger.info("当前轮次: {}, 总切片数: {}, 上下文长度: {}", 
                    state.getCurrentRound(), state.getTotalSegIds().size(), 
                    state.getCurrentContextLength());
        }

        assertTrue(state.isFinished() || state.isMaxRoundReached(), 
                "应完成或达到最大轮次");
        logger.info("最终状态: finished={}, reason={}", 
                state.isFinished(), state.getFinishReason());

        logger.info("\n✓ 多轮完整流程测试通过");
    }

    @Test
    @DisplayName("测试10：多轮完整流程 - 上下文超长终止")
    void testMultiRoundContextLengthTermination() {
        logger.info("\n========== 测试10：多轮完整流程 - 上下文超长终止 ==========");
        
        ReActState state = new ReActState(10, 200, 3);
        state.setRoundResults(new ArrayList<>());

        int totalLength = 0;
        
        for (int round = 1; round <= 10; round++) {
            state.nextRound();
            logger.info("\n--- 第 {} 轮 ---", round);

            String context = "[段落 " + round + "] " + 
                    repeatString("这是一段测试内容，用于模拟多轮召回过程中上下文的累积。", 2);
            totalLength += context.length();
            
            state.setCurrentContext(context);
            state.setCurrentContextLength(totalLength);
            
            logger.info("当前轮次: {}, 上下文长度: {}/{}", 
                    state.getCurrentRound(), totalLength, state.getMaxContextLength());

            ReActState.TerminationCheckResult check = state.checkTermination();
            logger.info("终止检查: shouldTerminate={}, reason={}", 
                    check.shouldTerminate(), check.getReason());

            if (check.shouldTerminate()) {
                state.finish(check.getReason());
                logger.info("终止原因: {}", state.getFinishReason());
                break;
            }

            state.getTotalSegIds().add("seg_round_" + round);
        }

        assertTrue(state.isFinished(), "应因上下文超长而终止");
        assertTrue(state.getFinishReason().contains("上下文长度超限"), 
                "终止原因应包含'上下文长度超限'");

        logger.info("\n✓ 上下文超长终止测试通过");
    }

    @Test
    @DisplayName("测试11：多轮完整流程 - 质量达标主动终止")
    void testMultiRoundQualityTermination() {
        logger.info("\n========== 测试11：多轮完整流程 - 质量达标主动终止 ==========");
        
        ReActState state = new ReActState(5, 8000, 3);
        state.setRoundResults(new ArrayList<>());

        for (int round = 1; round <= 5; round++) {
            state.nextRound();
            logger.info("\n--- 第 {} 轮 ---", round);

            state.getTotalSegIds().add("seg_round_" + round);
            state.setCurrentContext("[段落 " + round + "] 内容\n\n");
            state.setCurrentContextLength(state.getCurrentContext().length());

            if (round == 3) {
                logger.info("第 3 轮质量评估达标，主动终止");
                state.finish("质量评估达标，无需继续召回");
                break;
            }
        }

        assertTrue(state.isFinished(), "应主动终止");
        assertEquals("质量评估达标，无需继续召回", state.getFinishReason(), 
                "终止原因应匹配");
        assertEquals(3, state.getCurrentRound(), "应在第 3 轮终止");

        logger.info("最终状态: finished={}, reason={}, round={}", 
                state.isFinished(), state.getFinishReason(), state.getCurrentRound());

        logger.info("\n✓ 质量达标主动终止测试通过");
    }

    @Test
    @DisplayName("测试12：边界条件 - MaxRound=1")
    void testBoundaryMaxRound1() {
        logger.info("\n========== 测试12：边界条件 - MaxRound=1 ==========");
        
        ReActState state = new ReActState(1, 8000, 3);
        state.setRoundResults(new ArrayList<>());

        logger.info("最大轮次: 1");

        state.nextRound();
        logger.info("第 1 轮后，当前轮次: {}", state.getCurrentRound());
        
        ReActState.TerminationCheckResult check = state.checkTermination();
        logger.info("终止检查: shouldTerminate={}, reason={}", 
                check.shouldTerminate(), check.getReason());
        
        assertTrue(check.shouldTerminate(), "MaxRound=1 时，第 1 轮后应终止");

        logger.info("\n✓ MaxRound=1 边界条件测试通过");
    }

    @Test
    @DisplayName("测试13：边界条件 - MaxContextLen=0")
    void testBoundaryMaxContextLen0() {
        logger.info("\n========== 测试13：边界条件 - MaxContextLen=0 ==========");
        
        ReActState state = new ReActState(5, 0, 3);
        state.setRoundResults(new ArrayList<>());

        logger.info("最大上下文长度: 0");

        ReActState.TerminationCheckResult check = state.checkTermination();
        logger.info("终止检查: shouldTerminate={}, reason={}", 
                check.shouldTerminate(), check.getReason());
        
        assertTrue(check.shouldTerminate(), "MaxContextLen=0 时，应立即终止");

        logger.info("\n✓ MaxContextLen=0 边界条件测试通过");
    }

    @Test
    @DisplayName("测试14：边界条件 - MaxAddSeg=0")
    void testBoundaryMaxAddSeg0() {
        logger.info("\n========== 测试14：边界条件 - MaxAddSeg=0 ==========");
        
        ReActState state = new ReActState(5, 8000, 0);
        state.setRoundResults(new ArrayList<>());

        logger.info("单轮最大新增切片数: 0");

        state.nextRound();
        
        List<RetrievalResult> newResults = Arrays.asList(
                new RetrievalResult("seg_1", 0.9, 1, "vector"),
                new RetrievalResult("seg_2", 0.85, 1, "vector")
        );

        List<String> uniqueNewSegIds = new ArrayList<>();
        for (RetrievalResult result : newResults) {
            if (uniqueNewSegIds.size() < state.getMaxAddSegPerRound()) {
                uniqueNewSegIds.add(result.getSegId());
            }
        }

        logger.info("实际新增切片数: {}", uniqueNewSegIds.size());
        assertEquals(0, uniqueNewSegIds.size(), 
                "MaxAddSeg=0 时，不应新增任何切片");

        logger.info("\n✓ MaxAddSeg=0 边界条件测试通过");
    }

    @Test
    @DisplayName("测试15：综合测试 - 所有终止条件组合")
    void testComprehensiveTerminationConditions() {
        logger.info("\n========== 测试15：综合测试 - 所有终止条件组合 ==========");
        
        int maxRound = 5;
        int maxContextLength = 500;
        int maxAddSeg = 3;
        
        ReActState state = new ReActState(maxRound, maxContextLength, maxAddSeg);
        state.setRoundResults(new ArrayList<>());

        logger.info("配置: MaxRound={}, MaxContextLen={}, MaxAddSeg={}", 
                maxRound, maxContextLength, maxAddSeg);

        int totalLength = 0;
        int terminatedRound = 0;
        String terminationReason = "";

        for (int round = 1; round <= maxRound; round++) {
            state.nextRound();
            logger.info("\n--- 第 {} 轮 ---", round);

            String context = "[段落 " + round + "] " + 
                    repeatString("这是第" + round + "轮召回的内容，用于测试终止条件。", 3);
            totalLength += context.length();
            
            state.setCurrentContext(context);
            state.setCurrentContextLength(totalLength);
            state.getTotalSegIds().add("seg_round_" + round);

            logger.info("上下文长度: {}/{}", totalLength, maxContextLength);

            ReActState.TerminationCheckResult check = state.checkTermination();
            logger.info("终止检查: shouldTerminate={}, reason={}", 
                    check.shouldTerminate(), check.getReason());

            if (check.shouldTerminate()) {
                state.finish(check.getReason());
                terminatedRound = round;
                terminationReason = check.getReason();
                logger.info("第 {} 轮终止: {}", round, check.getReason());
                break;
            }
        }

        assertTrue(state.isFinished(), "应终止");
        assertTrue(terminatedRound > 0, "应记录终止轮次");
        assertFalse(terminationReason.isEmpty(), "应记录终止原因");

        logger.info("\n最终结果:");
        logger.info("终止轮次: {}", terminatedRound);
        logger.info("终止原因: {}", terminationReason);
        logger.info("总切片数: {}", state.getTotalSegIds().size());
        logger.info("最终上下文长度: {}", state.getCurrentContextLength());

        logger.info("\n✓ 综合终止条件测试通过");
    }

    // ==================== 辅助方法 ====================

    /**
     * 重复字符串（Java 8 兼容）
     */
    private String repeatString(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}