package org.example.milvuschinabank.rag.model;

/**
 * 召回结果
 */
public class RetrievalResult {

    /**
     * 切片ID
     */
    private String segId;

    /**
     * 相似度分数
     */
    private double score;

    /**
     * 召回轮次
     */
    private int round;

    /**
     * 召回来源（vector/keyword/context）
     */
    private String source;

    public RetrievalResult() {
    }

    public RetrievalResult(String segId, double score, int round, String source) {
        this.segId = segId;
        this.score = score;
        this.round = round;
        this.source = source;
    }

    public String getSegId() {
        return segId;
    }

    public void setSegId(String segId) {
        this.segId = segId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}