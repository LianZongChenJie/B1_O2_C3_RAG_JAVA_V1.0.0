package org.example.milvuschinabank.rag.model;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * ReAct 召回状态
 * 记录多轮上下文补全过程中的状态信息
 */
public class ReActState {

    /**
     * 当前轮次（从1开始）
     */
    private int currentRound;

    /**
     * 最大轮次限制
     */
    private final int maxRound;

    /**
     * 当前已收集的切片ID集合（全局去重）
     */
    private Set<String> totalSegIds;

    /**
     * 当前上下文文本
     */
    private String currentContext;

    /**
     * 当前上下文长度
     */
    private int currentContextLength;

    /**
     * 最大上下文长度阈值
     */
    private final int maxContextLength;

    /**
     * 单轮最大新增切片数
     */
    private final int maxAddSegPerRound;

    /**
     * 是否已完成（满足终止条件）
     */
    private boolean isFinished;

    /**
     * 终止原因
     */
    private String finishReason;

    /**
     * 每轮召回结果列表
     */
    private List<RetrievalResult> roundResults;

    public ReActState(int maxRound, int maxContextLength, int maxAddSegPerRound) {
        this.maxRound = maxRound;
        this.maxContextLength = maxContextLength;
        this.maxAddSegPerRound = maxAddSegPerRound;
        this.currentRound = 0;
        this.isFinished = false;
        this.currentContextLength = 0;
        this.totalSegIds = new HashSet<>();
    }

    /**
     * 进入下一轮
     */
    public void nextRound() {
        this.currentRound++;
    }

    /**
     * 检查是否达到最大轮次
     */
    public boolean isMaxRoundReached() {
        return this.currentRound >= this.maxRound;
    }

    /**
     * 检查是否超过上下文长度限制
     */
    public boolean isContextLengthExceeded() {
        return this.currentContextLength >= this.maxContextLength;
    }

    /**
     * 标记为完成
     */
    public void finish(String reason) {
        this.isFinished = true;
        this.finishReason = reason;
    }

    /**
     * 获取终止条件检查结果
     */
    public TerminationCheckResult checkTermination() {
        if (isFinished) {
            return TerminationCheckResult.alreadyFinished(finishReason);
        }
        if (isMaxRoundReached()) {
            return TerminationCheckResult.maxRoundReached(maxRound);
        }
        if (isContextLengthExceeded()) {
            return TerminationCheckResult.contextLengthExceeded(currentContextLength, maxContextLength);
        }
        return TerminationCheckResult.continueRetrieval();
    }

    // Getters and Setters

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRound() {
        return maxRound;
    }

    public Set<String> getTotalSegIds() {
        return totalSegIds;
    }

    public void setTotalSegIds(Set<String> totalSegIds) {
        this.totalSegIds = totalSegIds;
    }

    public String getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(String currentContext) {
        this.currentContext = currentContext;
    }

    public int getCurrentContextLength() {
        return currentContextLength;
    }

    public void setCurrentContextLength(int currentContextLength) {
        this.currentContextLength = currentContextLength;
    }

    public int getMaxContextLength() {
        return maxContextLength;
    }

    public int getMaxAddSegPerRound() {
        return maxAddSegPerRound;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public List<RetrievalResult> getRoundResults() {
        return roundResults;
    }

    public void setRoundResults(List<RetrievalResult> roundResults) {
        this.roundResults = roundResults;
    }

    /**
     * 终止条件检查结果
     */
    public static class TerminationCheckResult {
        private final boolean shouldTerminate;
        private final String reason;

        private TerminationCheckResult(boolean shouldTerminate, String reason) {
            this.shouldTerminate = shouldTerminate;
            this.reason = reason;
        }

        public static TerminationCheckResult alreadyFinished(String reason) {
            return new TerminationCheckResult(true, "已完成: " + reason);
        }

        public static TerminationCheckResult maxRoundReached(int maxRound) {
            return new TerminationCheckResult(true, "达到最大轮次限制: " + maxRound);
        }

        public static TerminationCheckResult contextLengthExceeded(int current, int max) {
            return new TerminationCheckResult(true,
                    "上下文长度超限: " + current + "/" + max);
        }

        public static TerminationCheckResult continueRetrieval() {
            return new TerminationCheckResult(false, "继续召回");
        }

        public boolean shouldTerminate() {
            return shouldTerminate;
        }

        public String getReason() {
            return reason;
        }
    }
}