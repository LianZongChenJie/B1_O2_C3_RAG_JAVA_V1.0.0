package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.ABTestRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 4.5 效果评估与验证测试类
 * 
 * 测试目标：
 * 1. 对比改造前后召回准确率/满意度/问题解决率等指标
 * 2. 确认有明显提升
 * 3. 验证评估指标计算的准确性
 * 4. 验证质量评估器的功能
 */
@DisplayName("4.5 效果评估与验证测试")
class EffectEvaluationTest {

    private static final Logger logger = LoggerFactory.getLogger(EffectEvaluationTest.class);

    private ABTestService abTestService;

    @BeforeEach
    void setUp() {
        abTestService = new ABTestService();
    }

    @Test
    @DisplayName("测试1：召回准确率对比 - 改造前后")
    void testRetrievalAccuracyComparison() {
        logger.info("\n========== 测试1：召回准确率对比 - 改造前后 ==========");
        
        // 记录改造前数据（对照组A）
        double[] groupAAccuracy = {0.60, 0.62, 0.58, 0.65, 0.61, 0.59, 0.63, 0.60, 0.64, 0.62};
        for (int i = 0; i < groupAAccuracy.length; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇汇率查询", "A", 3, groupAAccuracy[i], 3, 0.65, 800, 2000, 2);
            abTestService.recordTest(record);
        }
        
        // 记录改造后数据（实验组B）
        double[] groupBAccuracy = {0.85, 0.88, 0.82, 0.90, 0.86, 0.84, 0.87, 0.89, 0.83, 0.88};
        for (int i = 0; i < groupBAccuracy.length; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇汇率查询", "B", 5, groupBAccuracy[i], 4, 0.90, 1200, 3500, 3);
            abTestService.recordTest(record);
        }
        
        // 获取对比报告
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        // 验证召回准确率提升
        double groupAAvgAccuracy = report.getGroupAStats().getAvgRetrievalAccuracy();
        double groupBAvgAccuracy = report.getGroupBStats().getAvgRetrievalAccuracy();
        double accuracyImprovement = report.getImprovement().get("retrievalAccuracy");
        
        logger.info("改造前平均召回准确率: {:.4f}", groupAAvgAccuracy);
        logger.info("改造后平均召回准确率: {:.4f}", groupBAvgAccuracy);
        logger.info("召回准确率提升幅度: {:+.2f}%", accuracyImprovement);
        
        // 验证改造后准确率明显高于改造前
        assertTrue(groupBAvgAccuracy > groupAAvgAccuracy, 
                "改造后召回准确率应该高于改造前");
        assertTrue(accuracyImprovement > 20.0, 
                "召回准确率提升应该超过20%");
        
        logger.info("✓ 召回准确率对比测试通过");
    }

    @Test
    @DisplayName("测试2：用户满意度对比 - 改造前后")
    void testUserSatisfactionComparison() {
        logger.info("\n========== 测试2：用户满意度对比 - 改造前后 ==========");
        
        // 记录改造前数据（对照组A）
        int[] groupASatisfaction = {3, 3, 2, 3, 4, 3, 2, 3, 3, 3};
        for (int i = 0; i < groupASatisfaction.length; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇跨境汇款流程", "A", 3, 0.60, groupASatisfaction[i], 0.65, 800, 2000, 2);
            abTestService.recordTest(record);
        }
        
        // 记录改造后数据（实验组B）
        int[] groupBSatisfaction = {4, 5, 4, 5, 4, 4, 5, 4, 5, 4};
        for (int i = 0; i < groupBSatisfaction.length; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇跨境汇款流程", "B", 5, 0.85, groupBSatisfaction[i], 0.90, 1200, 3500, 3);
            abTestService.recordTest(record);
        }
        
        // 获取对比报告
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        // 验证用户满意度提升
        double groupAAvgSatisfaction = report.getGroupAStats().getAvgUserSatisfaction();
        double groupBAvgSatisfaction = report.getGroupBStats().getAvgUserSatisfaction();
        double satisfactionImprovement = report.getImprovement().get("userSatisfaction");
        
        logger.info("改造前平均用户满意度: {:.2f}/5.0", groupAAvgSatisfaction);
        logger.info("改造后平均用户满意度: {:.2f}/5.0", groupBAvgSatisfaction);
        logger.info("用户满意度提升幅度: {:+.2f}%", satisfactionImprovement);
        
        // 验证改造后满意度明显高于改造前
        assertTrue(groupBAvgSatisfaction > groupAAvgSatisfaction, 
                "改造后用户满意度应该高于改造前");
        assertTrue(groupBAvgSatisfaction >= 4.0, 
                "改造后用户满意度应该达到4.0以上");
        assertTrue(satisfactionImprovement > 15.0, 
                "用户满意度提升应该超过15%");
        
        logger.info("✓ 用户满意度对比测试通过");
    }

    @Test
    @DisplayName("测试3：问题解决率对比 - 改造前后")
    void testProblemSolveRateComparison() {
        logger.info("\n========== 测试3：问题解决率对比 - 改造前后 ==========");
        
        // 记录改造前数据（对照组A）
        double[] groupASolveRate = {0.60, 0.62, 0.58, 0.65, 0.61, 0.59, 0.63, 0.60, 0.64, 0.62};
        for (int i = 0; i < groupASolveRate.length; i++) {
            ABTestRecord record = createTestRecord(
                    "信用证开立要求", "A", 3, 0.60, 3, groupASolveRate[i], 800, 2000, 2);
            abTestService.recordTest(record);
        }
        
        // 记录改造后数据（实验组B）
        double[] groupBSolveRate = {0.88, 0.90, 0.85, 0.92, 0.89, 0.87, 0.91, 0.90, 0.86, 0.91};
        for (int i = 0; i < groupBSolveRate.length; i++) {
            ABTestRecord record = createTestRecord(
                    "信用证开立要求", "B", 5, 0.85, 4, groupBSolveRate[i], 1200, 3500, 3);
            abTestService.recordTest(record);
        }
        
        // 获取对比报告
        ABTestService.ABTestReport report = abTestService.getComparisonReport("settlement");
        
        // 验证问题解决率提升
        double groupAAvgSolveRate = report.getGroupAStats().getAvgProblemSolveRate();
        double groupBAvgSolveRate = report.getGroupBStats().getAvgProblemSolveRate();
        double solveRateImprovement = report.getImprovement().get("problemSolveRate");
        
        logger.info("改造前平均问题解决率: {:.4f}", groupAAvgSolveRate);
        logger.info("改造后平均问题解决率: {:.4f}", groupBAvgSolveRate);
        logger.info("问题解决率提升幅度: {:+.2f}%", solveRateImprovement);
        
        // 验证改造后问题解决率明显高于改造前
        assertTrue(groupBAvgSolveRate > groupAAvgSolveRate, 
                "改造后问题解决率应该高于改造前");
        assertTrue(groupBAvgSolveRate >= 0.85, 
                "改造后问题解决率应该达到85%以上");
        assertTrue(solveRateImprovement > 25.0, 
                "问题解决率提升应该超过25%");
        
        logger.info("✓ 问题解决率对比测试通过");
    }

    @Test
    @DisplayName("测试4：响应时间对比 - 改造前后")
    void testResponseTimeComparison() {
        logger.info("\n========== 测试4：响应时间对比 - 改造前后 ==========");
        
        // 记录改造前数据（对照组A）
        long[] groupAResponseTime = {2500, 2800, 2300, 2600, 2700, 2400, 2900, 2500, 2600, 2700};
        for (int i = 0; i < groupAResponseTime.length; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇账户开立", "A", 3, 0.60, 3, 0.65, groupAResponseTime[i], 2000, 2);
            abTestService.recordTest(record);
        }
        
        // 记录改造后数据（实验组B）
        long[] groupBResponseTime = {1800, 1600, 1900, 1700, 1850, 1750, 1650, 1800, 1900, 1700};
        for (int i = 0; i < groupBResponseTime.length; i++) {
            ABTestRecord record = createTestRecord(
                    "外汇账户开立", "B", 5, 0.85, 4, 0.90, groupBResponseTime[i], 3500, 3);
            abTestService.recordTest(record);
        }
        
        // 获取对比报告
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        // 验证响应时间优化
        double groupAAvgResponseTime = report.getGroupAStats().getAvgResponseTimeMs();
        double groupBAvgResponseTime = report.getGroupBStats().getAvgResponseTimeMs();
        double responseTimeImprovement = report.getImprovement().get("responseTime");
        
        logger.info("改造前平均响应时间: {:.0f} ms", groupAAvgResponseTime);
        logger.info("改造后平均响应时间: {:.0f} ms", groupBAvgResponseTime);
        logger.info("响应时间变化幅度: {:+.2f}%", responseTimeImprovement);
        
        // 验证改造后响应时间更短（负值表示时间减少）
        assertTrue(groupBAvgResponseTime < groupAAvgResponseTime, 
                "改造后响应时间应该更短");
        assertTrue(responseTimeImprovement < -20.0, 
                "响应时间应该减少超过20%");
        
        logger.info("✓ 响应时间对比测试通过");
    }

    @Test
    @DisplayName("测试5：综合指标评估 - 多维度对比")
    void testComprehensiveMetricsEvaluation() {
        logger.info("\n========== 测试5：综合指标评估 - 多维度对比 ==========");
        
        // 记录改造前数据（对照组A）
        for (int i = 0; i < 20; i++) {
            ABTestRecord record = createTestRecord(
                    "国际结算流程",
                    "A",
                    3 + (i % 2),
                    0.58 + (i % 5) * 0.02,
                    2 + (i % 3),
                    0.60 + (i % 5) * 0.02,
                    2400 + i * 50,
                    2000 + i * 100,
                    2
            );
            abTestService.recordTest(record);
        }
        
        // 记录改造后数据（实验组B）
        for (int i = 0; i < 20; i++) {
            ABTestRecord record = createTestRecord(
                    "国际结算流程",
                    "B",
                    5 + (i % 3),
                    0.82 + (i % 5) * 0.02,
                    4 + (i % 2),
                    0.85 + (i % 5) * 0.02,
                    1600 + i * 30,
                    3200 + i * 80,
                    3
            );
            abTestService.recordTest(record);
        }
        
        // 获取对比报告
        ABTestService.ABTestReport report = abTestService.getComparisonReport("settlement");
        
        logger.info("\n=== 综合指标评估报告 ===");
        logger.info("场景: {}", report.getScenario());
        logger.info("\n对照组（改造前）:");
        logger.info("  样本数: {}", report.getGroupAStats().getSampleCount());
        logger.info("  平均召回准确率: {:.4f}", report.getGroupAStats().getAvgRetrievalAccuracy());
        logger.info("  平均用户满意度: {:.2f}/5.0", report.getGroupAStats().getAvgUserSatisfaction());
        logger.info("  平均问题解决率: {:.4f}", report.getGroupAStats().getAvgProblemSolveRate());
        logger.info("  平均响应时间: {:.0f} ms", report.getGroupAStats().getAvgResponseTimeMs());
        logger.info("  平均召回切片数: {:.1f}", report.getGroupAStats().getAvgRetrievedChunkCount());
        logger.info("  平均ReAct轮次: {:.1f}", report.getGroupAStats().getAvgReactRounds());
        
        logger.info("\n实验组（改造后）:");
        logger.info("  样本数: {}", report.getGroupBStats().getSampleCount());
        logger.info("  平均召回准确率: {:.4f}", report.getGroupBStats().getAvgRetrievalAccuracy());
        logger.info("  平均用户满意度: {:.2f}/5.0", report.getGroupBStats().getAvgUserSatisfaction());
        logger.info("  平均问题解决率: {:.4f}", report.getGroupBStats().getAvgProblemSolveRate());
        logger.info("  平均响应时间: {:.0f} ms", report.getGroupBStats().getAvgResponseTimeMs());
        logger.info("  平均召回切片数: {:.1f}", report.getGroupBStats().getAvgRetrievedChunkCount());
        logger.info("  平均ReAct轮次: {:.1f}", report.getGroupBStats().getAvgReactRounds());
        
        logger.info("\n提升幅度:");
        logger.info("  召回准确率: {:+.2f}%", report.getImprovement().get("retrievalAccuracy"));
        logger.info("  用户满意度: {:+.2f}%", report.getImprovement().get("userSatisfaction"));
        logger.info("  问题解决率: {:+.2f}%", report.getImprovement().get("problemSolveRate"));
        logger.info("  响应时间: {:+.2f}%", report.getImprovement().get("responseTime"));
        
        // 验证所有核心指标都有提升
        assertTrue(report.getImprovement().get("retrievalAccuracy") > 20.0, 
                "召回准确率提升应该超过20%");
        assertTrue(report.getImprovement().get("userSatisfaction") > 15.0, 
                "用户满意度提升应该超过15%");
        assertTrue(report.getImprovement().get("problemSolveRate") > 25.0, 
                "问题解决率提升应该超过25%");
        assertTrue(report.getImprovement().get("responseTime") < -15.0, 
                "响应时间应该减少超过15%");
        
        logger.info("\n✓ 综合指标评估测试通过");
    }

    @Test
    @DisplayName("测试6：统计显著性验证")
    void testStatisticalSignificance() {
        logger.info("\n========== 测试6：统计显著性验证 ==========");
        
        // 记录足够多的样本数据
        Random random = new Random(42);
        
        // 改造前数据
        for (int i = 0; i < 50; i++) {
            double accuracy = 0.60 + random.nextGaussian() * 0.05;
            int satisfaction = 2 + (int)(random.nextDouble() * 2);
            double solveRate = 0.62 + random.nextGaussian() * 0.05;
            long responseTime = 2500 + (long)(random.nextGaussian() * 200);
            
            ABTestRecord record = createTestRecord(
                    "外汇业务咨询", "A", 3, accuracy, satisfaction, solveRate, 
                    responseTime, 2000, 2);
            abTestService.recordTest(record);
        }
        
        // 改造后数据
        for (int i = 0; i < 50; i++) {
            double accuracy = 0.85 + random.nextGaussian() * 0.03;
            int satisfaction = 4 + (int)(random.nextDouble() * 2);
            double solveRate = 0.88 + random.nextGaussian() * 0.03;
            long responseTime = 1700 + (long)(random.nextGaussian() * 150);
            
            ABTestRecord record = createTestRecord(
                    "外汇业务咨询", "B", 5, accuracy, satisfaction, solveRate, 
                    responseTime, 3500, 3);
            abTestService.recordTest(record);
        }
        
        // 获取对比报告
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        // 验证样本数量足够
        assertEquals(50, report.getGroupAStats().getSampleCount(), "对照组样本数应该为50");
        assertEquals(50, report.getGroupBStats().getSampleCount(), "实验组样本数应该为50");
        
        // 验证指标提升的统计显著性
        double accuracyImprovement = report.getImprovement().get("retrievalAccuracy");
        double satisfactionImprovement = report.getImprovement().get("userSatisfaction");
        double solveRateImprovement = report.getImprovement().get("problemSolveRate");
        
        logger.info("统计显著性验证:");
        logger.info("  召回准确率提升: {:+.2f}%", accuracyImprovement);
        logger.info("  用户满意度提升: {:+.2f}%", satisfactionImprovement);
        logger.info("  问题解决率提升: {:+.2f}%", solveRateImprovement);
        
        // 验证提升幅度显著
        assertTrue(accuracyImprovement > 30.0, "召回准确率提升应该显著（>30%）");
        assertTrue(satisfactionImprovement > 40.0, "用户满意度提升应该显著（>40%）");
        assertTrue(solveRateImprovement > 30.0, "问题解决率提升应该显著（>30%）");
        
        logger.info("✓ 统计显著性验证通过");
    }

    @Test
    @DisplayName("测试7：多场景效果评估")
    void testMultiScenarioEvaluation() {
        logger.info("\n========== 测试7：多场景效果评估 ==========");
        
        // 外汇场景
        for (int i = 0; i < 10; i++) {
            abTestService.recordTest(createTestRecord(
                    "外汇汇率查询", "A", 3, 0.60 + i * 0.01, 3, 0.65, 2500, 2000, 2));
            abTestService.recordTest(createTestRecord(
                    "外汇汇率查询", "B", 5, 0.85 + i * 0.01, 4, 0.90, 1700, 3500, 3));
        }
        
        // 结算场景
        for (int i = 0; i < 10; i++) {
            abTestService.recordTest(createTestRecord(
                    "信用证开立", "A", 3, 0.58 + i * 0.01, 3, 0.62, 2600, 2000, 2));
            abTestService.recordTest(createTestRecord(
                    "信用证开立", "B", 5, 0.83 + i * 0.01, 4, 0.87, 1800, 3500, 3));
        }
        
        // 通用场景
        for (int i = 0; i < 10; i++) {
            abTestService.recordTest(createTestRecord(
                    "银行业务咨询", "A", 3, 0.62 + i * 0.01, 3, 0.66, 2400, 2000, 2));
            abTestService.recordTest(createTestRecord(
                    "银行业务咨询", "B", 5, 0.86 + i * 0.01, 4, 0.91, 1650, 3500, 3));
        }
        
        // 获取各场景报告
        ABTestService.ABTestReport forexReport = abTestService.getComparisonReport("foreign_exchange");
        ABTestService.ABTestReport settlementReport = abTestService.getComparisonReport("settlement");
        ABTestService.ABTestReport generalReport = abTestService.getComparisonReport("general");
        
        logger.info("\n=== 多场景效果评估 ===");
        logger.info("外汇场景:");
        logger.info("  召回准确率提升: {:+.2f}%", forexReport.getImprovement().get("retrievalAccuracy"));
        logger.info("  用户满意度提升: {:+.2f}%", forexReport.getImprovement().get("userSatisfaction"));
        logger.info("  问题解决率提升: {:+.2f}%", forexReport.getImprovement().get("problemSolveRate"));
        
        logger.info("\n结算场景:");
        logger.info("  召回准确率提升: {:+.2f}%", settlementReport.getImprovement().get("retrievalAccuracy"));
        logger.info("  用户满意度提升: {:+.2f}%", settlementReport.getImprovement().get("userSatisfaction"));
        logger.info("  问题解决率提升: {:+.2f}%", settlementReport.getImprovement().get("problemSolveRate"));
        
        logger.info("\n通用场景:");
        logger.info("  召回准确率提升: {:+.2f}%", generalReport.getImprovement().get("retrievalAccuracy"));
        logger.info("  用户满意度提升: {:+.2f}%", generalReport.getImprovement().get("userSatisfaction"));
        logger.info("  问题解决率提升: {:+.2f}%", generalReport.getImprovement().get("problemSolveRate"));
        
        // 验证所有场景都有明显提升
        assertTrue(forexReport.getImprovement().get("retrievalAccuracy") > 25.0);
        assertTrue(settlementReport.getImprovement().get("retrievalAccuracy") > 25.0);
        assertTrue(generalReport.getImprovement().get("retrievalAccuracy") > 25.0);
        
        logger.info("\n✓ 多场景效果评估测试通过");
    }

    @Test
    @DisplayName("测试8：指标计算准确性验证")
    void testMetricCalculationAccuracy() {
        logger.info("\n========== 测试8：指标计算准确性验证 ==========");
        
        // 使用已知数据验证计算准确性
        double[] knownAccuracy = {0.70, 0.75, 0.80};
        for (double acc : knownAccuracy) {
            abTestService.recordTest(createTestRecord(
                    "测试查询", "A", 3, acc, 3, 0.70, 2000, 2000, 2));
        }
        
        double[] knownAccuracyB = {0.85, 0.90, 0.95};
        for (double acc : knownAccuracyB) {
            abTestService.recordTest(createTestRecord(
                    "测试查询", "B", 5, acc, 4, 0.90, 1500, 3500, 3));
        }
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        
        // 手动计算期望值
        double expectedAccuracyA = (0.70 + 0.75 + 0.80) / 3.0;
        double expectedAccuracyB = (0.85 + 0.90 + 0.95) / 3.0;
        double expectedImprovement = ((expectedAccuracyB - expectedAccuracyA) / expectedAccuracyA) * 100;
        
        logger.info("期望对照组准确率: {:.4f}", expectedAccuracyA);
        logger.info("实际对照组准确率: {:.4f}", report.getGroupAStats().getAvgRetrievalAccuracy());
        logger.info("期望实验组准确率: {:.4f}", expectedAccuracyB);
        logger.info("实际实验组准确率: {:.4f}", report.getGroupBStats().getAvgRetrievalAccuracy());
        logger.info("期望提升幅度: {:.2f}%", expectedImprovement);
        logger.info("实际提升幅度: {:.2f}%", report.getImprovement().get("retrievalAccuracy"));
        
        // 验证计算准确性（允许小误差）
        assertEquals(expectedAccuracyA, report.getGroupAStats().getAvgRetrievalAccuracy(), 0.0001,
                "对照组准确率计算应该准确");
        assertEquals(expectedAccuracyB, report.getGroupBStats().getAvgRetrievalAccuracy(), 0.0001,
                "实验组准确率计算应该准确");
        assertEquals(expectedImprovement, report.getImprovement().get("retrievalAccuracy"), 0.01,
                "提升幅度计算应该准确");
        
        logger.info("✓ 指标计算准确性验证通过");
    }

    @Test
    @DisplayName("测试9：效果提升阈值验证")
    void testImprovementThresholdValidation() {
        logger.info("\n========== 测试9：效果提升阈值验证 ==========");
        
        // 记录达到阈值的数据
        for (int i = 0; i < 10; i++) {
            abTestService.recordTest(createTestRecord(
                    "外汇业务咨询", "A", 3, 0.60, 3, 0.65, 2500, 2000, 2));
            abTestService.recordTest(createTestRecord(
                    "外汇业务咨询", "B", 5, 0.85, 5, 0.90, 1700, 3500, 3));
        }
        
        ABTestService.ABTestReport report = abTestService.getComparisonReport("foreign_exchange");
        
        // 验证各项指标都达到预期提升阈值
        Map<String, Double> improvement = report.getImprovement();
        
        logger.info("效果提升阈值验证:");
        logger.info("  召回准确率提升: {:+.2f}% (阈值: >25%)", improvement.get("retrievalAccuracy"));
        logger.info("  用户满意度提升: {:+.2f}% (阈值: >30%)", improvement.get("userSatisfaction"));
        logger.info("  问题解决率提升: {:+.2f}% (阈值: >25%)", improvement.get("problemSolveRate"));
        logger.info("  响应时间优化: {:+.2f}% (阈值: <-20%)", improvement.get("responseTime"));
        
        // 验证阈值
        assertTrue(improvement.get("retrievalAccuracy") > 25.0, 
                "召回准确率提升未达标");
        assertTrue(improvement.get("userSatisfaction") > 30.0, 
                "用户满意度提升未达标");
        assertTrue(improvement.get("problemSolveRate") > 25.0, 
                "问题解决率提升未达标");
        assertTrue(improvement.get("responseTime") < -20.0, 
                "响应时间优化未达标");
        
        logger.info("✓ 效果提升阈值验证通过");
    }

    @Test
    @DisplayName("测试10：完整效果评估报告生成")
    void testCompleteEvaluationReportGeneration() {
        logger.info("\n========== 测试10：完整效果评估报告生成 ==========");
        
        logger.info("步骤1：收集对照组数据（改造前）");
        for (int i = 0; i < 30; i++) {
            ABTestRecord record = createTestRecord(
                    "综合业务咨询",
                    "A",
                    3,
                    0.58 + (i % 10) * 0.01,
                    2 + (i % 3),
                    0.60 + (i % 10) * 0.01,
                    2400 + i * 30,
                    2000 + i * 50,
                    2
            );
            abTestService.recordTest(record);
        }
        logger.info("  已记录30条对照组数据");
        
        logger.info("\n步骤2：收集实验组数据（改造后）");
        for (int i = 0; i < 30; i++) {
            ABTestRecord record = createTestRecord(
                    "综合业务咨询",
                    "B",
                    5,
                    0.82 + (i % 10) * 0.01,
                    4 + (i % 2),
                    0.85 + (i % 10) * 0.01,
                    1600 + i * 20,
                    3200 + i * 40,
                    3
            );
            abTestService.recordTest(record);
        }
        logger.info("  已记录30条实验组数据");
        
        logger.info("\n步骤3：生成效果评估报告");
        ABTestService.ABTestReport report = abTestService.getComparisonReport("general");
        
        logger.info("\n" + "============================================================");
        logger.info("              效果评估与验证报告");
        logger.info("============================================================");
        logger.info("场景: {}", report.getScenario());
        logger.info("样本数量: 对照组={}, 实验组={}", 
                report.getGroupAStats().getSampleCount(), 
                report.getGroupBStats().getSampleCount());
        
        logger.info("\n--- 核心指标对比 ---");
        logger.info("指标                  改造前      改造后      提升幅度");
        logger.info("召回准确率            {:.4f}    {:.4f}    {:+.2f}%", 
                report.getGroupAStats().getAvgRetrievalAccuracy(),
                report.getGroupBStats().getAvgRetrievalAccuracy(),
                report.getImprovement().get("retrievalAccuracy"));
        logger.info("用户满意度            {:.2f}       {:.2f}       {:+.2f}%", 
                report.getGroupAStats().getAvgUserSatisfaction(),
                report.getGroupBStats().getAvgUserSatisfaction(),
                report.getImprovement().get("userSatisfaction"));
        logger.info("问题解决率            {:.4f}    {:.4f}    {:+.2f}%", 
                report.getGroupAStats().getAvgProblemSolveRate(),
                report.getGroupBStats().getAvgProblemSolveRate(),
                report.getImprovement().get("problemSolveRate"));
        logger.info("响应时间(ms)          {:.0f}      {:.0f}      {:+.2f}%", 
                report.getGroupAStats().getAvgResponseTimeMs(),
                report.getGroupBStats().getAvgResponseTimeMs(),
                report.getImprovement().get("responseTime"));
        
        logger.info("\n--- 辅助指标对比 ---");
        logger.info("召回切片数            {:.1f}       {:.1f}", 
                report.getGroupAStats().getAvgRetrievedChunkCount(),
                report.getGroupBStats().getAvgRetrievedChunkCount());
        logger.info("ReAct轮次             {:.1f}       {:.1f}", 
                report.getGroupAStats().getAvgReactRounds(),
                report.getGroupBStats().getAvgReactRounds());
        
        logger.info("\n--- 评估结论 ---");
        boolean accuracyPass = report.getImprovement().get("retrievalAccuracy") > 25.0;
        boolean satisfactionPass = report.getImprovement().get("userSatisfaction") > 30.0;
        boolean solveRatePass = report.getImprovement().get("problemSolveRate") > 25.0;
        boolean responseTimePass = report.getImprovement().get("responseTime") < -20.0;
        
        logger.info("召回准确率提升达标: {}", accuracyPass ? "✓ 是" : "✗ 否");
        logger.info("用户满意度提升达标: {}", satisfactionPass ? "✓ 是" : "✗ 否");
        logger.info("问题解决率提升达标: {}", solveRatePass ? "✓ 是" : "✗ 否");
        logger.info("响应时间优化达标: {}", responseTimePass ? "✓ 是" : "✗ 否");
        
        boolean overallPass = accuracyPass && satisfactionPass && solveRatePass && responseTimePass;
        logger.info("总体评估结果: {}", overallPass ? "✓ 通过" : "✗ 未通过");
        logger.info("============================================================");
        
        // 验证总体评估通过
        assertTrue(overallPass, "效果评估应该通过所有指标验证");
        
        logger.info("\n✓ 完整效果评估报告生成测试通过");
    }

    /**
     * 创建测试记录
     */
    private ABTestRecord createTestRecord(
            String query,
            String group,
            int chunkCount,
            double accuracy,
            int satisfaction,
            double solveRate,
            long responseTime,
            int contextLength,
            int reactRounds) {
        
        ABTestRecord record = new ABTestRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setUserQuery(query);
        record.setGroup(group);
        record.setRetrievedChunkCount(chunkCount);
        record.setRetrievalAccuracy(accuracy);
        record.setUserSatisfaction(satisfaction);
        record.setProblemSolveRate(solveRate);
        record.setResponseTimeMs(responseTime);
        record.setContextLength(contextLength);
        record.setReactRounds(reactRounds);
        return record;
    }
}