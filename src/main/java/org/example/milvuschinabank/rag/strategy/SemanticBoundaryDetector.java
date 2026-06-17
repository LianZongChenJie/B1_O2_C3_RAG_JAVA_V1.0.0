package org.example.milvuschinabank.rag.strategy;

/**
 * 语义割裂检测器接口
 */
public interface SemanticBoundaryDetector {

    /**
     * 计算两个文本片段之间的语义连贯度
     * @param text1 文本1
     * @param text2 文本2
     * @return 连贯度分数（0-1，越高越连贯）
     */
    double calculateCohesion(String text1, String text2);
}