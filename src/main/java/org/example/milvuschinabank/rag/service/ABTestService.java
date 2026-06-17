package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.ABTestRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A/B 测试服务
 * 记录改造前后的对比数据，支持效果评估
 */
@Service
public class ABTestService {

    private static final Logger logger = LoggerFactory.getLogger(ABTestService.class);

    /**
     * 存储测试记录（实际应使用数据库）
     */
    private final Map<String, List<ABTestRecord>> testRecords = new ConcurrentHashMap<>();

    /**
     * 记录测试结果
     */
    public void recordTest(ABTestRecord record) {
        String scenario = getScenarioFromQuery(record.getUserQuery());

        testRecords.computeIfAbsent(scenario, k -> new ArrayList<>())
                .add(record);

        logger.info("记录 A/B 测试: 场景={}, 组别={}, 准确率={}",
                scenario, record.getGroup(), record.getRetrievalAccuracy());
    }

    /**
     * 获取对比报告
     */
    public ABTestReport getComparisonReport(String scenario) {
        List<ABTestRecord> records = testRecords.getOrDefault(scenario, new ArrayList<>());

        List<ABTestRecord> groupA = records.stream()
                .filter(r -> "A".equals(r.getGroup()))
                .collect(Collectors.toList());

        List<ABTestRecord> groupB = records.stream()
                .filter(r -> "B".equals(r.getGroup()))
                .collect(Collectors.toList());

        ABTestReport report = new ABTestReport();
        report.setScenario(scenario);
        report.setGroupAStats(calculateStats(groupA));
        report.setGroupBStats(calculateStats(groupB));
        report.setImprovement(calculateImprovement(report.getGroupAStats(), report.getGroupBStats()));

        return report;
    }

    /**
     * 计算统计数据
     */
    private GroupStats calculateStats(List<ABTestRecord> records) {
        if (records.isEmpty()) {
            return new GroupStats();
        }

        GroupStats stats = new GroupStats();
        stats.setSampleCount(records.size());

        stats.setAvgRetrievalAccuracy(records.stream()
                .mapToDouble(ABTestRecord::getRetrievalAccuracy)
                .average().orElse(0.0));

        stats.setAvgUserSatisfaction(records.stream()
                .mapToInt(ABTestRecord::getUserSatisfaction)
                .average().orElse(0.0));

        stats.setAvgProblemSolveRate(records.stream()
                .mapToDouble(ABTestRecord::getProblemSolveRate)
                .average().orElse(0.0));

        stats.setAvgResponseTimeMs(records.stream()
                .mapToLong(ABTestRecord::getResponseTimeMs)
                .average().orElse(0.0));

        stats.setAvgRetrievedChunkCount(records.stream()
                .mapToInt(ABTestRecord::getRetrievedChunkCount)
                .average().orElse(0.0));

        stats.setAvgReactRounds(records.stream()
                .mapToInt(ABTestRecord::getReactRounds)
                .average().orElse(0.0));

        return stats;
    }

    /**
     * 计算提升幅度
     */
    private Map<String, Double> calculateImprovement(GroupStats groupA, GroupStats groupB) {
        Map<String, Double> improvement = new HashMap<>();

        if (groupA.getSampleCount() == 0 || groupB.getSampleCount() == 0) {
            return improvement;
        }

        improvement.put("retrievalAccuracy",
                calculatePercentImprovement(groupA.getAvgRetrievalAccuracy(), groupB.getAvgRetrievalAccuracy()));

        improvement.put("userSatisfaction",
                calculatePercentImprovement(groupA.getAvgUserSatisfaction(), groupB.getAvgUserSatisfaction()));

        improvement.put("problemSolveRate",
                calculatePercentImprovement(groupA.getAvgProblemSolveRate(), groupB.getAvgProblemSolveRate()));

        improvement.put("responseTime",
                calculatePercentImprovement(groupA.getAvgResponseTimeMs(), groupB.getAvgResponseTimeMs()));

        return improvement;
    }

    /**
     * 计算百分比提升
     */
    private double calculatePercentImprovement(double base, double current) {
        if (base == 0) {
            return 0;
        }
        return ((current - base) / base) * 100;
    }

    /**
     * 从查询中推断场景
     */
    private String getScenarioFromQuery(String query) {
        if (query.contains("外汇") || query.contains("汇率")) {
            return "foreign_exchange";
        }
        if (query.contains("结算") || query.contains("信用证")) {
            return "settlement";
        }
        return "general";
    }

    /**
     * 组统计数据
     */
    public static class GroupStats {
        private int sampleCount;
        private double avgRetrievalAccuracy;
        private double avgUserSatisfaction;
        private double avgProblemSolveRate;
        private double avgResponseTimeMs;
        private double avgRetrievedChunkCount;
        private double avgReactRounds;

        // Getters and Setters

        public int getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }

        public double getAvgRetrievalAccuracy() {
            return avgRetrievalAccuracy;
        }

        public void setAvgRetrievalAccuracy(double avgRetrievalAccuracy) {
            this.avgRetrievalAccuracy = avgRetrievalAccuracy;
        }

        public double getAvgUserSatisfaction() {
            return avgUserSatisfaction;
        }

        public void setAvgUserSatisfaction(double avgUserSatisfaction) {
            this.avgUserSatisfaction = avgUserSatisfaction;
        }

        public double getAvgProblemSolveRate() {
            return avgProblemSolveRate;
        }

        public void setAvgProblemSolveRate(double avgProblemSolveRate) {
            this.avgProblemSolveRate = avgProblemSolveRate;
        }

        public double getAvgResponseTimeMs() {
            return avgResponseTimeMs;
        }

        public void setAvgResponseTimeMs(double avgResponseTimeMs) {
            this.avgResponseTimeMs = avgResponseTimeMs;
        }

        public double getAvgRetrievedChunkCount() {
            return avgRetrievedChunkCount;
        }

        public void setAvgRetrievedChunkCount(double avgRetrievedChunkCount) {
            this.avgRetrievedChunkCount = avgRetrievedChunkCount;
        }

        public double getAvgReactRounds() {
            return avgReactRounds;
        }

        public void setAvgReactRounds(double avgReactRounds) {
            this.avgReactRounds = avgReactRounds;
        }
    }

    /**
     * A/B 测试报告
     */
    public static class ABTestReport {
        private String scenario;
        private GroupStats groupAStats;
        private GroupStats groupBStats;
        private Map<String, Double> improvement;

        // Getters and Setters

        public String getScenario() {
            return scenario;
        }

        public void setScenario(String scenario) {
            this.scenario = scenario;
        }

        public GroupStats getGroupAStats() {
            return groupAStats;
        }

        public void setGroupAStats(GroupStats groupAStats) {
            this.groupAStats = groupAStats;
        }

        public GroupStats getGroupBStats() {
            return groupBStats;
        }

        public void setGroupBStats(GroupStats groupBStats) {
            this.groupBStats = groupBStats;
        }

        public Map<String, Double> getImprovement() {
            return improvement;
        }

        public void setImprovement(Map<String, Double> improvement) {
            this.improvement = improvement;
        }
    }
}