package org.example.milvuschinabank.rag.model;

/**
 * 语义割裂类型枚举
 * 定义文档切片之间语义不连贯的判定标准
 */
public enum SemanticBoundaryType {

    /**
     * 无割裂（语义连贯）
     */
    NONE("无割裂", 0.8),

    /**
     * 段落边界（自然段分隔）
     */
    PARAGRAPH("段落边界", 0.6),

    /**
     * 章节边界（标题/章节分隔）
     */
    CHAPTER("章节边界", 0.4),

    /**
     * 主题切换（话题/业务领域变化）
     */
    TOPIC_SWITCH("主题切换", 0.3),

    /**
     * 文档边界（不同文档之间）
     */
    DOCUMENT("文档边界", 0.0);

    /**
     * 割裂类型描述
     */
    private final String description;

    /**
     * 语义连贯度阈值（低于此值判定为该类型割裂）
     */
    private final double cohesionThreshold;

    SemanticBoundaryType(String description, double cohesionThreshold) {
        this.description = description;
        this.cohesionThreshold = cohesionThreshold;
    }

    public String getDescription() {
        return description;
    }

    public double getCohesionThreshold() {
        return cohesionThreshold;
    }

    /**
     * 根据语义连贯度分数判定割裂类型
     * @param cohesionScore 语义连贯度分数（0-1）
     * @return 割裂类型
     */
    public static SemanticBoundaryType fromCohesionScore(double cohesionScore) {
        if (cohesionScore >= 0.8) {
            return NONE;
        } else if (cohesionScore >= 0.6) {
            return PARAGRAPH;
        } else if (cohesionScore >= 0.4) {
            return CHAPTER;
        } else if (cohesionScore >= 0.3) {
            return TOPIC_SWITCH;
        } else {
            return DOCUMENT;
        }
    }
}