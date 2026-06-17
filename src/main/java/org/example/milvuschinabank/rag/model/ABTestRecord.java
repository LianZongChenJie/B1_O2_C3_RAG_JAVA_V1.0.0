package org.example.milvuschinabank.rag.model;

import java.util.Date;
import java.util.Map;

/**
 * A/B 测试记录
 * 记录改造前后的对比数据
 */
public class ABTestRecord {

    /**
     * 记录ID
     */
    private String recordId;

    /**
     * 用户查询
     */
    private String userQuery;

    /**
     * 实验组（A=改造前，B=改造后）
     */
    private String group;

    /**
     * 召回切片数
     */
    private int retrievedChunkCount;

    /**
     * 召回准确率（0-1）
     */
    private double retrievalAccuracy;

    /**
     * 用户满意度（1-5）
     */
    private int userSatisfaction;

    /**
     * 问题解决率（0-1）
     */
    private double problemSolveRate;

    /**
     * 响应时间（毫秒）
     */
    private long responseTimeMs;

    /**
     * 上下文长度
     */
    private int contextLength;

    /**
     * ReAct 轮次
     */
    private int reactRounds;

    /**
     * 额外指标
     */
    private Map<String, Object> extraMetrics;

    /**
     * 测试时间
     */
    private Date testTime;

    public ABTestRecord() {
        this.testTime = new Date();
    }

    // Getters and Setters

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getUserQuery() {
        return userQuery;
    }

    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getRetrievedChunkCount() {
        return retrievedChunkCount;
    }

    public void setRetrievedChunkCount(int retrievedChunkCount) {
        this.retrievedChunkCount = retrievedChunkCount;
    }

    public double getRetrievalAccuracy() {
        return retrievalAccuracy;
    }

    public void setRetrievalAccuracy(double retrievalAccuracy) {
        this.retrievalAccuracy = retrievalAccuracy;
    }

    public int getUserSatisfaction() {
        return userSatisfaction;
    }

    public void setUserSatisfaction(int userSatisfaction) {
        this.userSatisfaction = userSatisfaction;
    }

    public double getProblemSolveRate() {
        return problemSolveRate;
    }

    public void setProblemSolveRate(double problemSolveRate) {
        this.problemSolveRate = problemSolveRate;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public int getContextLength() {
        return contextLength;
    }

    public void setContextLength(int contextLength) {
        this.contextLength = contextLength;
    }

    public int getReactRounds() {
        return reactRounds;
    }

    public void setReactRounds(int reactRounds) {
        this.reactRounds = reactRounds;
    }

    public Map<String, Object> getExtraMetrics() {
        return extraMetrics;
    }

    public void setExtraMetrics(Map<String, Object> extraMetrics) {
        this.extraMetrics = extraMetrics;
    }

    public Date getTestTime() {
        return testTime;
    }

    public void setTestTime(Date testTime) {
        this.testTime = testTime;
    }
}