package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.model.SemanticBoundaryType;
import org.example.milvuschinabank.rag.repository.MilvusChunkRepository;
import org.example.milvuschinabank.rag.strategy.SemanticBoundaryDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 2.2 入库阶段元数据自动生成
 * 
 * 切片元数据服务，负责在文档入库阶段自动生成完整的元数据并入库到 Milvus。
 * 
 * 核心功能：
 * 1. 文档切分：按固定大小切分文档，保留句子边界
 * 2. 元数据生成：为每个切片生成 segId、docId、pos 等基础元数据
 * 3. 邻接关系绑定：维护前后切片的邻接关系（preSegId/nextSegId）
 * 4. 标签提取：自动提取业务标签(biz:)、实体标签(entity:)、主题标签(topic:)
 * 5. 语义割裂检测：计算语义连贯度分数，识别语义边界点
 * 6. Milvus 入库：将带有完整元数据的切片批量入库到 Milvus
 * 
 * 入库流程：
 * 原始文档 -> 文档切分 -> 元数据生成 -> 语义分析 -> Milvus 入库
 * 
 * @author RAG Team
 * @since 2024
 */
@Service
public class ChunkMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMetadataService.class);

    @Autowired
    private SemanticBoundaryDetector boundaryDetector;

    @Autowired
    private TagExtractor tagExtractor;

    @Autowired
    private MilvusChunkRepository milvusRepository;

    /**
     * 处理文档切片列表，自动生成完整元数据
     * @param rawChunks 原始切片列表（仅包含 content）
     * @param docId 文档ID
     * @return 带有完整元数据的切片列表
     */
    public List<DocumentChunk> processChunks(List<String> rawChunks, String docId) {
        long startTime = System.currentTimeMillis();
        
        if (rawChunks == null || rawChunks.isEmpty()) {
            return new ArrayList<>();
        }

        logger.info("开始处理 {} 个切片", rawChunks.size());

        List<DocumentChunk> processedChunks = new ArrayList<>();

        for (int i = 0; i < rawChunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();

            // 生成唯一 segId
            chunk.setSegId(generateSegId(docId, i));
            chunk.setDocId(docId);
            chunk.setContent(rawChunks.get(i));

            // 分配全局 pos（从 0 开始）
            chunk.setPos(i);

            // 绑定前后邻接关系
            if (i > 0) {
                chunk.setPreSegId(processedChunks.get(i - 1).getSegId());
            } else {
                chunk.setPreSegId(null); // 首切片
            }

            if (i < rawChunks.size() - 1) {
                // 暂时设置为下一个，后续会更新
                chunk.setNextSegId(generateSegId(docId, i + 1));
            } else {
                chunk.setNextSegId(null); // 尾切片
            }

            // 提取标签
            List<String> tags = tagExtractor.extractTags(rawChunks.get(i), docId, i);
            chunk.setTags(tags);

            processedChunks.add(chunk);
        }

        logger.info("标签提取完成，共处理 {} 个切片", processedChunks.size());

        long tagTime = System.currentTimeMillis();

        // 检测语义割裂
        logger.info("开始检测语义割裂...");
        detectSemanticBoundaries(processedChunks);
        long boundaryTime = System.currentTimeMillis();
        logger.info("语义割裂检测完成，耗时: {} ms", boundaryTime - tagTime);

        logger.info("切片处理完成，总耗时: {} ms", System.currentTimeMillis() - startTime);

        return processedChunks;
    }

    /**
     * 检测切片之间的语义割裂
     */
    private void detectSemanticBoundaries(List<DocumentChunk> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk current = chunks.get(i);

            if (i == 0) {
                // 首切片，检查是否与后续切片有割裂
                if (chunks.size() > 1) {
                    double cohesion = boundaryDetector.calculateCohesion(
                            current.getContent(), chunks.get(1).getContent());
                    current.setSemanticCohesion(cohesion);
                    SemanticBoundaryType boundaryType = SemanticBoundaryType.fromCohesionScore(cohesion);
                    current.setSemanticBoundary(boundaryType != SemanticBoundaryType.NONE);
                    current.setBoundaryType(boundaryType);
                }
            } else if (i == chunks.size() - 1) {
                // 尾切片，检查是否与前序切片有割裂
                DocumentChunk previous = chunks.get(i - 1);
                double cohesion = boundaryDetector.calculateCohesion(
                        previous.getContent(), current.getContent());
                current.setSemanticCohesion(cohesion);
                SemanticBoundaryType boundaryType = SemanticBoundaryType.fromCohesionScore(cohesion);
                current.setSemanticBoundary(boundaryType != SemanticBoundaryType.NONE);
                current.setBoundaryType(boundaryType);
            } else {
                // 中间切片，取前后连贯度的最小值
                DocumentChunk previous = chunks.get(i - 1);
                DocumentChunk next = chunks.get(i + 1);

                double cohesionWithPrev = boundaryDetector.calculateCohesion(
                        previous.getContent(), current.getContent());
                double cohesionWithNext = boundaryDetector.calculateCohesion(
                        current.getContent(), next.getContent());

                // 取较小值作为该切片的连贯度
                double minCohesion = Math.min(cohesionWithPrev, cohesionWithNext);
                current.setSemanticCohesion(minCohesion);

                SemanticBoundaryType boundaryType = SemanticBoundaryType.fromCohesionScore(minCohesion);
                current.setSemanticBoundary(boundaryType != SemanticBoundaryType.NONE);
                current.setBoundaryType(boundaryType);
            }
        }
    }

    /**
     * 生成切片唯一ID
     * 格式：{docId}_seg_{pos}_{shortHash}
     * 使用 MD5 哈希的前 8 位确保 ID 长度可控，避免超过 Milvus 字段限制
     */
    private String generateSegId(String docId, int pos) {
        String hash = Integer.toHexString(UUID.randomUUID().hashCode());
        // 确保哈希字符串至少 8 位，不足则左侧补 0
        String shortHash = String.format("%8s", hash).replace(' ', '0').substring(0, 8);
        return docId + "_seg_" + pos + "_" + shortHash;
    }

    /**
     * 单个文档入库处理（生成元数据 + 入库到 Milvus）
     * @param docId 文档ID
     * @param content 完整文档内容
     * @param chunkSize 切片大小（字符数）
     * @return 处理后的切片列表
     */
    public List<DocumentChunk> ingestDocument(String docId, String content, int chunkSize) {
        // 按 chunkSize 切分文档
        List<String> rawChunks = splitDocument(content, chunkSize);

        // 生成元数据
        List<DocumentChunk> processedChunks = processChunks(rawChunks, docId);

        // 入库到 Milvus
        if (!processedChunks.isEmpty()) {
            logger.info("开始入库文档 [{}]，共 {} 个切片", docId, processedChunks.size());
            milvusRepository.insertChunks(processedChunks);
            logger.info("文档 [{}] 入库完成", docId);
        }

        return processedChunks;
    }

    /**
     * 按固定大小切分文档（保留句子边界）
     */
    private List<String> splitDocument(String content, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int length = content.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            // 如果不是最后一个切片，尝试在句子边界处截断
            if (end < length) {
                // 向前查找最近的句子边界
                int lastSentenceEnd = findLastSentenceBoundary(content, start, end);
                if (lastSentenceEnd > start) {
                    end = lastSentenceEnd;
                }
            }

            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end;
        }

        return chunks;
    }

    /**
     * 查找最近的句子边界
     */
    private int findLastSentenceBoundary(String content, int start, int end) {
        // 优先查找中文句号、问号、叹号
        String sentenceEndings = "。！？.?!\n";

        for (int i = end - 1; i >= start; i--) {
            char c = content.charAt(i);
            if (sentenceEndings.indexOf(c) != -1) {
                return i + 1; // 包含标点符号
            }
        }

        return end; // 未找到句子边界，使用原始位置
    }
}