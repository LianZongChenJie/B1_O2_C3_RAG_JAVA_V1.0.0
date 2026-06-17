package org.example.milvuschinabank.rag.model;

import java.util.List;

/**
 * 2.1 切片元数据模型设计
 * 
 * 文档切片元数据模型，用于存储和管理文档切片的完整元数据信息。
 * 
 * 核心字段说明：
 * - segId: 切片唯一标识
 * - docId: 所属文档ID
 * - pos: 全局顺序号（按原文顺序分配）
 * - preSegId/nextSegId: 前后邻接关系（首/尾切片为null）
 * - tags: 多标签数组（业务/主题/实体标签）
 * - semanticCohesion: 语义连贯度分数（0-1，越高越连贯）
 * - isSemanticBoundary: 是否为语义边界（语义割裂点）
 * - boundaryType: 语义割裂类型
 * - vector: 切片向量（用于向量检索）
 * 
 * @author RAG Team
 * @since 2024
 */
public class DocumentChunk {

    /**
     * 切片唯一标识
     */
    private String segId;

    /**
     * 所属文档ID
     */
    private String docId;

    /**
     * 切片文本内容
     */
    private String content;

    /**
     * 全局顺序号（按原文顺序分配）
     */
    private Integer pos;

    /**
     * 前一个切片ID（首切片为null）
     */
    private String preSegId;

    /**
     * 后一个切片ID（尾切片为null）
     */
    private String nextSegId;

    /**
     * 多标签数组（业务/主题/实体标签）
     */
    private List<String> tags;

    /**
     * 语义连贯度分数（0-1，越高越连贯）
     */
    private Double semanticCohesion;

    /**
     * 是否为语义边界（语义割裂点）
     */
    private Boolean isSemanticBoundary;

    /**
     * 语义割裂类型
     */
    private SemanticBoundaryType boundaryType;

    /**
     * 切片向量（用于向量检索）
     */
    private List<Float> vector;

    public DocumentChunk() {
    }

    public DocumentChunk(String segId, String docId, String content, Integer pos) {
        this.segId = segId;
        this.docId = docId;
        this.content = content;
        this.pos = pos;
        this.isSemanticBoundary = false;
        this.semanticCohesion = 1.0;
    }

    // Getters and Setters

    public String getSegId() {
        return segId;
    }

    public void setSegId(String segId) {
        this.segId = segId;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPos() {
        return pos;
    }

    public void setPos(Integer pos) {
        this.pos = pos;
    }

    public String getPreSegId() {
        return preSegId;
    }

    public void setPreSegId(String preSegId) {
        this.preSegId = preSegId;
    }

    public String getNextSegId() {
        return nextSegId;
    }

    public void setNextSegId(String nextSegId) {
        this.nextSegId = nextSegId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Double getSemanticCohesion() {
        return semanticCohesion;
    }

    public void setSemanticCohesion(Double semanticCohesion) {
        this.semanticCohesion = semanticCohesion;
    }

    public Boolean getSemanticBoundary() {
        return isSemanticBoundary;
    }

    public void setSemanticBoundary(Boolean semanticBoundary) {
        isSemanticBoundary = semanticBoundary;
    }

    public SemanticBoundaryType getBoundaryType() {
        return boundaryType;
    }

    public void setBoundaryType(SemanticBoundaryType boundaryType) {
        this.boundaryType = boundaryType;
    }

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    @Override
    public String toString() {
        return "DocumentChunk{" +
                "segId='" + segId + '\'' +
                ", docId='" + docId + '\'' +
                ", pos=" + pos +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", tags=" + tags +
                ", isSemanticBoundary=" + isSemanticBoundary +
                '}';
    }
}