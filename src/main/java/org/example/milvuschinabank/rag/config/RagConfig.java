package org.example.milvuschinabank.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置类
 * 用于管理 ReAct 召回相关的配置参数
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    /**
     * ReAct 最大轮次
     */
    private int maxRound = 5;

    /**
     * 单轮最大新增切片数
     */
    private int maxAddSegPerRound = 3;

    /**
     * 最大上下文长度（字符数）
     */
    private int maxContextLength = 8000;

    /**
     * 语义连贯度阈值（低于此值判定为语义割裂）
     */
    private double semanticCohesionThreshold = 0.6;

    /**
     * 向量检索 TopK
     */
    private int vectorTopK = 10;

    /**
     * 混合检索权重（向量检索权重，关键词权重为 1-此值）
     */
    private double vectorWeight = 0.7;

    /**
     * 重排序阈值（低于此分数的结果将被过滤）
     */
    private double rerankThreshold = 0.3;

    /**
     * Milvus Collection 名称
     */
    private String collectionName = "chinabank_rag";

    /**
     * LLM API 地址（兼容 OpenAI 协议）
     */
    private String llmApiUrl;

    /**
     * LLM API Key
     */
    private String llmApiKey;

    /**
     * LLM 模型名称
     */
    private String llmModelName;

    /**
     * Milvus 主机地址
     */
    private String milvusHost = "localhost";

    /**
     * Milvus 端口
     */
    private int milvusPort = 19530;

    /**
     * Milvus 用户名
     */
    private String milvusUsername = "root";

    /**
     * Milvus 密码
     */
    private String milvusPassword = "Milvus";

    /**
     * 是否启用 Milvus 认证
     */
    private boolean milvusUseAuth = true;

    /**
     * 向量维度（默认 768，根据 embedding 模型调整）
     */
    private int vectorDimension = 768;

    /**
     * 内容重叠检测阈值（字符数）
     */
    private int overlapDetectionThreshold = 20;

    // Getters and Setters

    public int getMaxRound() {
        return maxRound;
    }

    public void setMaxRound(int maxRound) {
        this.maxRound = maxRound;
    }

    public int getMaxAddSegPerRound() {
        return maxAddSegPerRound;
    }

    public void setMaxAddSegPerRound(int maxAddSegPerRound) {
        this.maxAddSegPerRound = maxAddSegPerRound;
    }

    public int getMaxContextLength() {
        return maxContextLength;
    }

    public void setMaxContextLength(int maxContextLength) {
        this.maxContextLength = maxContextLength;
    }

    public double getSemanticCohesionThreshold() {
        return semanticCohesionThreshold;
    }

    public void setSemanticCohesionThreshold(double semanticCohesionThreshold) {
        this.semanticCohesionThreshold = semanticCohesionThreshold;
    }

    public int getVectorTopK() {
        return vectorTopK;
    }

    public void setVectorTopK(int vectorTopK) {
        this.vectorTopK = vectorTopK;
    }

    public double getVectorWeight() {
        return vectorWeight;
    }

    public void setVectorWeight(double vectorWeight) {
        this.vectorWeight = vectorWeight;
    }

    public double getRerankThreshold() {
        return rerankThreshold;
    }

    public void setRerankThreshold(double rerankThreshold) {
        this.rerankThreshold = rerankThreshold;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getLlmApiUrl() {
        return llmApiUrl;
    }

    public void setLlmApiUrl(String llmApiUrl) {
        this.llmApiUrl = llmApiUrl;
    }

    public String getLlmApiKey() {
        return llmApiKey;
    }

    public void setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
    }

    public String getLlmModelName() {
        return llmModelName;
    }

    public void setLlmModelName(String llmModelName) {
        this.llmModelName = llmModelName;
    }

    public String getMilvusHost() {
        return milvusHost;
    }

    public void setMilvusHost(String milvusHost) {
        this.milvusHost = milvusHost;
    }

    public int getMilvusPort() {
        return milvusPort;
    }

    public void setMilvusPort(int milvusPort) {
        this.milvusPort = milvusPort;
    }

    public String getMilvusUsername() {
        return milvusUsername;
    }

    public void setMilvusUsername(String milvusUsername) {
        this.milvusUsername = milvusUsername;
    }

    public String getMilvusPassword() {
        return milvusPassword;
    }

    public void setMilvusPassword(String milvusPassword) {
        this.milvusPassword = milvusPassword;
    }

    public boolean isMilvusUseAuth() {
        return milvusUseAuth;
    }

    public void setMilvusUseAuth(boolean milvusUseAuth) {
        this.milvusUseAuth = milvusUseAuth;
    }

    public int getVectorDimension() {
        return vectorDimension;
    }

    public void setVectorDimension(int vectorDimension) {
        this.vectorDimension = vectorDimension;
    }

    public int getOverlapDetectionThreshold() {
        return overlapDetectionThreshold;
    }

    public void setOverlapDetectionThreshold(int overlapDetectionThreshold) {
        this.overlapDetectionThreshold = overlapDetectionThreshold;
    }
}