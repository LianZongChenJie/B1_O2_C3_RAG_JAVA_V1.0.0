package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.ABTestRecord;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 4.4 试点集成与A/B测试环境搭建 - 单元测试
 * 
 * 测试 A/B 测试环境和试点集成功能，验证：
 * - 实验组和对照组数据记录
 * - 改造前后对比数据收集
 * - 统计数据计算（准确率、满意度、解决率、响应时间等）
 * - 提升幅度计算
 * - 场景分类（外汇、结算、通用）
 * - 对比报告生成
 * - 现有功能不受影响
 * - 多场景测试数据管理
 * - 边界条件处理
 * - 性能测试
 * 
 * @author RAG Team
 * @since 2024
 */
public class ABTestEnvironmentTest {

    private static final Logger logger = LoggerFactory.getLogger(ABTestEnvironmentTest.class);

    private ABTestService abTestService;

    @BeforeEach
    void setUp() {
        abTestService = new ABTestService();
    }

    @Test
    @DisplayName("测试1：A/B测试记录 - 实验组数据记录")
    void testABTestRecordGroupB() {
        logger.info("\n========== 测试1：A/B测试记录 - 实验组数据记录 ==========");
        
        ABTestRecord record = createTestRecord(
                "个人购汇的具体额度是多少？",
                "B",
                5,
                0.85,
                4,
                0.90,
                1200,
                3500,
                3
        );
        
        abTestService.recordTest(record);
        
        logger.info("记录实验组数据:");
        logger.info("  用户查询: {}", record.getUserQuery());
        logger.info("  组别: {}", record.getGroup());
        logger.info("  召回切片数: {}", record.getRetrievedChunkCount());
        logger.info("  召回准确率: {}", record.getRetrievalAccuracy());
        logger.info("  用户满意度: {}", record.getUserSatisfaction());
        logger.info("  问题解决率: {}", record.getProblemSolveRate());
        logger.info("  响应时间: {} ms", record.getResponseTimeMs());
        logger.info("  上下文长度: {}", record.getContextLength());
        logger.info("  ReAct轮次: {}", record.getReactRounds());
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        
        assertNotNull(report, "报告不应为 null");
        assertEquals("general", report.getScenario());
        assertNotNull(report.getGroupBStats(), "实验组统计数据不应为 null");
        assertEquals(1, report.getGroupBStats().getSampleCount(), "实验组样本数应该为1");
        
        logger.info("✓ 实验组数据记录测试通过");
    }

    @Test
    @DisplayName("测试2：A/B测试记录 - 对照组数据记录")
    void testABTestRecordGroupA() {
        logger.info("\n========== 测试2：A/B测试记录 - 对照组数据记录 ==========");
        
        ABTestRecord record = createTestRecord(
                "外汇汇率如何查询？",
                "A",
                3,
                0.65,
                3,
                0.70,
                800,
                2000,
                2
        );
        
        abTestService.recordTest(record);
        
        logger.info("记录对照组数据:");
        logger.info("  用户查询: {}", record.getUserQuery());
        logger.info("  组别: {}", record.getGroup());
        logger.info("  召回切片数: {}", record.getRetrievedChunkCount());
        logger.info("  召回准确率: {}", record.getRetrievalAccuracy());
        logger.info("  用户满意度: {}", record.getUserSatisfaction());
        logger.info("  问题解决率: {}", record.getProblemSolveRate());
        logger.info("  响应时间: {} ms", record.getResponseTimeMs());
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        assertNotNull(report, "报告不应为 null");
        assertEquals(1, report.getGroupAStats().getSampleCount(), "对照组样本数应该为1");
        
        logger.info("✓ 对照组数据记录测试通过");
    }

    @Test
    @DisplayName("测试3：对比报告生成 - 改造前后对比")
    void testComparisonReportGeneration() {
        logger.info("\n========== 测试3：对比报告生成 - 改造前后对比 ==========");
        
        // 记录对照组数据（改造前）- 使用外汇相关查询
        for (int i = 0; i < 5; i++) {
            ABTestRecord record = createTestRecord(
                    "个人外汇购汇的具体额度是多少？",
                    "A",
                    3 + i,
                    0.60 + i * 0.05,
                    3,
                    0.65 + i * 0.05,
                    800 + i * 100,
                    2000 + i * 200,
                    2
            );
            abTestService.recordTest(record);
        }
        
        // 记录实验组数据（改造后）- 使用外汇相关查询
        for (int i = 0; i < 5; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇汇率如何查询？",
                    "B",
                    5 + i,
                    0.80 + i * 0.03,
                    4,
                    0.85 + i * 0.03,
                    1200 + i * 100,
                    3000 + i * 200,
                    3
            );
            abTestService.recordTest(record);
        }
        
        // 获取对比报告
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        logger.info("=== A/B 测试对比报告 ===");
        logger.info("场景: {}", report.getScenario());
        
        logger.info("\n对照组（改造前）:");
        logger.info("  样本数: {}", report.getGroupAStats().getSampleCount());
        logger.info("  平均召回准确率: {:.2f}%", report.getGroupAStats().getAvgRetrievalAccuracy() * 100);
        logger.info("  平均用户满意度: {:.1f}/5", report.getGroupAStats().getAvgUserSatisfaction());
        logger.info("  平均问题解决率: {:.2f}%", report.getGroupAStats().getAvgProblemSolveRate() * 100);
        logger.info("  平均响应时间: {:.0f} ms", report.getGroupAStats().getAvgResponseTimeMs());
        logger.info("  平均召回切片数: {:.1f}", report.getGroupAStats().getAvgRetrievedChunkCount());
        logger.info("  平均ReAct轮次: {:.1f}", report.getGroupAStats().getAvgReactRounds());
        
        logger.info("\n实验组（改造后）:");
        logger.info("  样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("  平均召回准确率: {:.2f}%", report.getGroupBStats().getAvgRetrievalAccuracy() * 100);
        logger.info("  平均用户满意度: {:.1f}/5", report.getGroupBStats().getAvgUserSatisfaction());
        logger.info("  平均问题解决率: {:.2f}%", report.getGroupBStats().getAvgProblemSolveRate() * 100);
        logger.info("  平均响应时间: {:.0f} ms", report.getGroupBStats().getAvgResponseTimeMs());
        logger.info("  平均召回切片数: {:.1f}", report.getGroupBStats().getAvgRetrievedChunkCount());
        logger.info("  平均ReAct轮次: {:.1f}", report.getGroupBStats().getAvgReactRounds());
        
        logger.info("\n提升幅度:");
        Map<String, Double> improvement = report.getImprovement();
        logger.info("  召回准确率: {:+.2f}%", improvement.get("retrievalAccuracy"));
        logger.info("  用户满意度: {:+.2f}%", improvement.get("userSatisfaction"));
        logger.info("  问题解决率: {:+.2f}%", improvement.get("problemSolveRate"));
        logger.info("  响应时间: {:+.2f}%", improvement.get("responseTime"));
        
        // 验证报告
        assertEquals(5, report.getGroupAStats().getSampleCount(), "对照组样本数应该为5");
        assertEquals(5, report.getGroupBStats().getSampleCount(), "实验组样本数应该为5");
        assertTrue(report.getGroupBStats().getAvgRetrievalAccuracy() > report.getGroupAStats().getAvgRetrievalAccuracy(),
                "实验组准确率应该高于对照组");
        assertTrue(report.getGroupBStats().getAvgUserSatisfaction() > report.getGroupAStats().getAvgUserSatisfaction(),
                "实验组满意度应该高于对照组");
        
        logger.info("✓ 对比报告生成测试通过");
    }

    @Test
    @DisplayName("测试4：统计数据计算 - 多指标统计")
    void testStatisticsCalculation() {
        logger.info("\n========== 测试4：统计数据计算 - 多指标统计 ==========");
        
        // 记录多组数据
        double[] accuracies = {0.70, 0.75, 0.80, 0.85, 0.90};
        int[] satisfactions = {3, 4, 4, 5, 5};
        double[] solveRates = {0.65, 0.70, 0.75, 0.80, 0.85};
        long[] responseTimes = {1000, 1100, 1200, 1300, 1400};
        int[] chunkCounts = {3, 4, 5, 6, 7};
        int[] reactRounds = {2, 2, 3, 3, 4};
        
        for (int i = 0; i < 5; i++) {
            ABTestRecord record = createTestRecord(
                    "测试查询" + i,
                    "B",
                    chunkCounts[i],
                    accuracies[i],
                    satisfactions[i],
                    solveRates[i],
                    responseTimes[i],
                    3000 + i * 500,
                    reactRounds[i]
            );
            abTestService.recordTest(record);
        }
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        ABTestService.GroupStats stats = report.getGroupBStats();
        
        logger.info("统计数据:");
        logger.info("  样本数: {}", stats.getSampleCount());
        logger.info("  平均召回准确率: {:.4f}", stats.getAvgRetrievalAccuracy());
        logger.info("  平均用户满意度: {:.2f}", stats.getAvgUserSatisfaction());
        logger.info("  平均问题解决率: {:.4f}", stats.getAvgProblemSolveRate());
        logger.info("  平均响应时间: {:.0f} ms", stats.getAvgResponseTimeMs());
        logger.info("  平均召回切片数: {:.1f}", stats.getAvgRetrievedChunkCount());
        logger.info("  平均ReAct轮次: {:.1f}", stats.getAvgReactRounds());
        
        // 验证计算
        assertEquals(5, stats.getSampleCount());
        assertEquals(0.80, stats.getAvgRetrievalAccuracy(), 0.01);
        assertEquals(4.2, stats.getAvgUserSatisfaction(), 0.01);
        assertEquals(0.75, stats.getAvgProblemSolveRate(), 0.01);
        assertEquals(1200.0, stats.getAvgResponseTimeMs(), 0.01);
        assertEquals(5.0, stats.getAvgRetrievedChunkCount(), 0.01);
        assertEquals(2.8, stats.getAvgReactRounds(), 0.01);
        
        logger.info("✓ 统计数据计算测试通过");
    }

    @Test
    @DisplayName("测试5：提升幅度计算 - 百分比提升")
    void testImprovementCalculation() {
        logger.info("\n========== 测试5：提升幅度计算 - 百分比提升 ==========");
        
        // 对照组数据
        for (int i = 0; i < 3; i++) {
            ABTestRecord record = createTestRecord(
                    "测试查询",
                    "A",
                    3,
                    0.60,
                    3,
                    0.65,
                    1000,
                    2000,
                    2
            );
            abTestService.recordTest(record);
        }
        
        // 实验组数据
        for (int i = 0; i < 3; i++) {
            ABTestRecord record = createTestRecord(
                    "测试查询",
                    "B",
                    5,
                    0.80,
                    4,
                    0.85,
                    1200,
                    3000,
                    3
            );
            abTestService.recordTest(record);
        }
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        Map<String, Double> improvement = report.getImprovement();
        
        logger.info("提升幅度:");
        logger.info("  召回准确率: {:+.2f}%", improvement.get("retrievalAccuracy"));
        logger.info("  用户满意度: {:+.2f}%", improvement.get("userSatisfaction"));
        logger.info("  问题解决率: {:+.2f}%", improvement.get("problemSolveRate"));
        logger.info("  响应时间: {:+.2f}%", improvement.get("responseTime"));
        
        // 验证提升幅度
        double expectedAccuracyImprovement = ((0.80 - 0.60) / 0.60) * 100;
        assertEquals(expectedAccuracyImprovement, improvement.get("retrievalAccuracy"), 0.01);
        
        double expectedSatisfactionImprovement = ((4.0 - 3.0) / 3.0) * 100;
        assertEquals(expectedSatisfactionImprovement, improvement.get("userSatisfaction"), 0.01);
        
        double expectedSolveRateImprovement = ((0.85 - 0.65) / 0.65) * 100;
        assertEquals(expectedSolveRateImprovement, improvement.get("problemSolveRate"), 0.01);
        
        logger.info("✓ 提升幅度计算测试通过");
    }

    @Test
    @DisplayName("测试6：场景分类 - 外汇场景")
    void testScenarioClassificationForeignExchange() {
        logger.info("\n========== 测试6：场景分类 - 外汇场景 ==========");
        
        String[] forexQueries = {
                "个人外汇购汇的具体额度是多少？",
                "外汇汇率如何查询？",
                "外汇跨境汇款需要多长时间？",
                "外汇账户如何开立？"
        };
        
        for (String query : forexQueries) {
            ABTestRecord record = createTestRecord(query, "B", 5, 0.85, 4, 0.90, 1200, 3500, 3);
            abTestService.recordTest(record);
        }
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        assertEquals(4, report.getGroupBStats().getSampleCount(), "外汇场景应该有4个样本");
        assertEquals("foreign_exchange", report.getScenario());
        
        logger.info("外汇场景样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("✓ 外汇场景分类测试通过");
    }

    @Test
    @DisplayName("测试7：场景分类 - 结算场景")
    void testScenarioClassificationSettlement() {
        logger.info("\n========== 测试7：场景分类 - 结算场景 ==========");
        
        String[] settlementQueries = {
                "跨境贸易结算的流程是什么？",
                "信用证如何开立？",
                "国际结算需要哪些材料？"
        };
        
        for (String query : settlementQueries) {
            ABTestRecord record = createTestRecord(query, "B", 5, 0.85, 4, 0.90, 1200, 3500, 3);
            abTestService.recordTest(record);
        }
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("settlement");
        
        assertEquals(3, report.getGroupBStats().getSampleCount(), "结算场景应该有3个样本");
        assertEquals("settlement", report.getScenario());
        
        logger.info("结算场景样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("✓ 结算场景分类测试通过");
    }

    @Test
    @DisplayName("测试8：场景分类 - 通用场景")
    void testScenarioClassificationGeneral() {
        logger.info("\n========== 测试8：场景分类 - 通用场景 ==========");
        
        String[] generalQueries = {
                "银行存款利率是多少？",
                "如何办理信用卡？",
                "贷款审批需要多长时间？"
        };
        
        for (String query : generalQueries) {
            ABTestRecord record = createTestRecord(query, "B", 5, 0.85, 4, 0.90, 1200, 3500, 3);
            abTestService.recordTest(record);
        }
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        
        assertEquals(3, report.getGroupBStats().getSampleCount(), "通用场景应该有3个样本");
        assertEquals("general", report.getScenario());
        
        logger.info("通用场景样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("✓ 通用场景分类测试通过");
    }

    @Test
    @DisplayName("测试9：多场景测试数据管理")
    void testMultiScenarioDataManagement() {
        logger.info("\n========== 测试9：多场景测试数据管理 ==========");
        
        // 外汇场景
        ABTestRecord forexRecord = createTestRecord("个人外汇购汇额度", "B", 5, 0.85, 4, 0.90, 1200, 3500, 3);
        abTestService.recordTest(forexRecord);
        
        // 结算场景
        ABTestRecord settlementRecord = createTestRecord("信用证开立流程", "B", 5, 0.85, 4, 0.90, 1200, 3500, 3);
        abTestService.recordTest(settlementRecord);
        
        // 通用场景
        ABTestRecord generalRecord = createTestRecord("银行存款利率", "B", 5, 0.85, 4, 0.90, 1200, 3500, 3);
        abTestService.recordTest(generalRecord);
        
        // 验证各场景独立
        ABTestService.ABTestReport forexReport = abTestService.getComparisonReport("foreign_exchange");
        ABTestService.ABTestReport settlementReport = abTestService.getComparisonReport("settlement");
        ABTestService.ABTestReport generalReport = abTestService.getComparisonReport("general");
        
        assertEquals(1, forexReport.getGroupBStats().getSampleCount());
        assertEquals(1, settlementReport.getGroupBStats().getSampleCount());
        assertEquals(1, generalReport.getGroupBStats().getSampleCount());
        
        logger.info("外汇场景样本数: {}", forexReport.getGroupBStats().getSampleCount());
        logger.info("结算场景样本数: {}", settlementReport.getGroupBStats().getSampleCount());
        logger.info("通用场景样本数: {}", generalReport.getGroupBStats().getSampleCount());
        
        logger.info("✓ 多场景测试数据管理测试通过");
    }

    @Test
    @DisplayName("测试10：边界条件 - 空数据报告")
    void testEmptyDataReport() {
        logger.info("\n========== 测试10：边界条件 - 空数据报告 ==========");
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("nonexistent");
        
        assertNotNull(report, "报告不应为 null");
        assertEquals("nonexistent", report.getScenario());
        assertEquals(0, report.getGroupAStats().getSampleCount());
        assertEquals(0, report.getGroupBStats().getSampleCount());
        
        logger.info("空数据报告样本数: A={}, B={}", 
                report.getGroupAStats().getSampleCount(),
                report.getGroupBStats().getSampleCount());
        
        logger.info("✓ 空数据报告边界条件测试通过");
    }

    @Test
    @DisplayName("测试11：边界条件 - 只有对照组数据")
    void testOnlyGroupAData() {
        logger.info("\n========== 测试11：边界条件 - 只有对照组数据 ==========");
        
        ABTestRecord record = createTestRecord("测试查询", "A", 3, 0.65, 3, 0.70, 800, 2000, 2);
        abTestService.recordTest(record);
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        
        assertEquals(1, report.getGroupAStats().getSampleCount());
        assertEquals(0, report.getGroupBStats().getSampleCount());
        assertTrue(report.getImprovement().isEmpty(), "只有一组数据时提升幅度应该为空");
        
        logger.info("只有对照组数据时:");
        logger.info("  对照组样本数: {}", report.getGroupAStats().getSampleCount());
        logger.info("  实验组样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("  提升幅度: {}", report.getImprovement().size());
        
        logger.info("✓ 只有对照组数据边界条件测试通过");
    }

    @Test
    @DisplayName("测试12：边界条件 - 只有实验组数据")
    void testOnlyGroupBData() {
        logger.info("\n========== 测试12：边界条件 - 只有实验组数据 ==========");
        
        ABTestRecord record = createTestRecord("测试查询", "B", 5, 0.85, 4, 0.90, 1200, 3500, 3);
        abTestService.recordTest(record);
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        
        assertEquals(0, report.getGroupAStats().getSampleCount());
        assertEquals(1, report.getGroupBStats().getSampleCount());
        assertTrue(report.getImprovement().isEmpty(), "只有一组数据时提升幅度应该为空");
        
        logger.info("只有实验组数据时:");
        logger.info("  对照组样本数: {}", report.getGroupAStats().getSampleCount());
        logger.info("  实验组样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("  提升幅度: {}", report.getImprovement().size());
        
        logger.info("✓ 只有实验组数据边界条件测试通过");
    }

    @Test
    @DisplayName("测试13：性能测试 - 大量数据记录")
    void testPerformanceLargeDataset() {
        logger.info("\n========== 测试13：性能测试 - 大量数据记录 ==========");
        
        int recordCount = 100;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < recordCount; i++) {
            String group = (i % 2 == 0) ? "A" : "B";
            ABTestRecord record = createTestRecord(
                    "测试查询" + i,
                    group,
                    3 + (i % 5),
                    0.60 + (i % 10) * 0.04,
                    3 + (i % 3),
                    0.65 + (i % 10) * 0.03,
                    800 + i * 10,
                    2000 + i * 20,
                    2 + (i % 3)
            );
            abTestService.recordTest(record);
        }
        
        long recordTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        long reportTime = System.currentTimeMillis() - startTime;
        
        logger.info("记录 {} 条数据耗时: {} ms", recordCount, recordTime);
        logger.info("生成报告耗时: {} ms", reportTime);
        logger.info("对照组样本数: {}", report.getGroupAStats().getSampleCount());
        logger.info("实验组样本数: {}", report.getGroupBStats().getSampleCount());
        
        assertEquals(50, report.getGroupAStats().getSampleCount());
        assertEquals(50, report.getGroupBStats().getSampleCount());
        assertTrue(recordTime < 1000, "记录耗时应该小于1秒");
        assertTrue(reportTime < 1000, "报告生成耗时应该小于1秒");
        
        logger.info("✓ 性能测试通过");
    }

    @Test
    @DisplayName("测试14：完整A/B测试流程 - 从记录到报告")
    void testCompleteABTestWorkflow() {
        logger.info("\n========== 测试14：完整A/B测试流程 - 从记录到报告 ==========");
        
        logger.info("步骤1：记录对照组数据（改造前）");
        for (int i = 0; i < 10; i++) {
            ABTestRecord record = createTestRecord(
                    "个人外汇购汇的具体额度是多少？",
                    "A",
                    3 + (i % 3),
                    0.60 + (i % 5) * 0.05,
                    3,
                    0.65 + (i % 5) * 0.05,
                    800 + i * 50,
                    2000 + i * 100,
                    2
            );
            abTestService.recordTest(record);
        }
        logger.info("  记录10条对照组数据");
        
        logger.info("\n步骤2：记录实验组数据（改造后）");
        for (int i = 0; i < 10; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇汇率如何查询？",
                    "B",
                    5 + (i % 3),
                    0.80 + (i % 5) * 0.03,
                    4,
                    0.85 + (i % 5) * 0.03,
                    1200 + i * 50,
                    3000 + i * 100,
                    3
            );
            abTestService.recordTest(record);
        }
        logger.info("  记录10条实验组数据");
        
        logger.info("\n步骤3：生成对比报告");
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        logger.info("\n=== A/B 测试最终报告 ===");
        logger.info("场景: {}", report.getScenario());
        logger.info("\n对照组（改造前）:");
        logger.info("  样本数: {}", report.getGroupAStats().getSampleCount());
        logger.info("  平均召回准确率: {:.2f}%", report.getGroupAStats().getAvgRetrievalAccuracy() * 100);
        logger.info("  平均用户满意度: {:.1f}/5", report.getGroupAStats().getAvgUserSatisfaction());
        logger.info("  平均问题解决率: {:.2f}%", report.getGroupAStats().getAvgProblemSolveRate() * 100);
        logger.info("  平均响应时间: {:.0f} ms", report.getGroupAStats().getAvgResponseTimeMs());
        
        logger.info("\n实验组（改造后）:");
        logger.info("  样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("  平均召回准确率: {:.2f}%", report.getGroupBStats().getAvgRetrievalAccuracy() * 100);
        logger.info("  平均用户满意度: {:.1f}/5", report.getGroupBStats().getAvgUserSatisfaction());
        logger.info("  平均问题解决率: {:.2f}%", report.getGroupBStats().getAvgProblemSolveRate() * 100);
        logger.info("  平均响应时间: {:.0f} ms", report.getGroupBStats().getAvgResponseTimeMs());
        
        logger.info("\n提升幅度:");
        Map<String, Double> improvement = report.getImprovement();
        logger.info("  召回准确率: {:+.2f}%", improvement.get("retrievalAccuracy"));
        logger.info("  用户满意度: {:+.2f}%", improvement.get("userSatisfaction"));
        logger.info("  问题解决率: {:+.2f}%", improvement.get("problemSolveRate"));
        logger.info("  响应时间: {:+.2f}%", improvement.get("responseTime"));
        
        // 验证
        assertEquals(10, report.getGroupAStats().getSampleCount());
        assertEquals(10, report.getGroupBStats().getSampleCount());
        assertTrue(report.getGroupBStats().getAvgRetrievalAccuracy() > report.getGroupAStats().getAvgRetrievalAccuracy());
        assertTrue(report.getGroupBStats().getAvgUserSatisfaction() > report.getGroupAStats().getAvgUserSatisfaction());
        assertTrue(improvement.get("retrievalAccuracy") > 0);
        
        logger.info("\n✓ 完整A/B测试流程测试通过");
    }

    // ==================== 辅助方法 ====================

    private ABTestRecord createTestRecord(
            String userQuery,
            String group,
            int retrievedChunkCount,
            double retrievalAccuracy,
            int userSatisfaction,
            double problemSolveRate,
            long responseTimeMs,
            int contextLength,
            int reactRounds) {
        
        ABTestRecord record = new ABTestRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setUserQuery(userQuery);
        record.setGroup(group);
        record.setRetrievedChunkCount(retrievedChunkCount);
        record.setRetrievalAccuracy(retrievalAccuracy);
        record.setUserSatisfaction(userSatisfaction);
        record.setProblemSolveRate(problemSolveRate);
        record.setResponseTimeMs(responseTimeMs);
        record.setContextLength(contextLength);
        record.setReactRounds(reactRounds);
        record.setTestTime(new Date());
        
        return record;
    }
}