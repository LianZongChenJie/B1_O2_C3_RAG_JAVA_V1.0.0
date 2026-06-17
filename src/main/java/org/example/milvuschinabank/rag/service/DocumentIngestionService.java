package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.parser.DocumentParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 文档入库服务
 * 串联文档解析、切片、元数据生成、Milvus入库的完整流程
 * 
 * 流程：
 * 用户上传文件 -> 文档解析器 -> 提取纯文本 -> 切片服务 -> 元数据生成 -> Milvus入库
 * 
 * @author RAG Team
 * @since 2024
 */
@Service
public class DocumentIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionService.class);

    @Autowired
    private DocumentParserService documentParserService;

    @Autowired
    private ChunkMetadataService chunkMetadataService;

    /**
     * 处理文件上传并入库
     * 
     * @param file 上传的文件
     * @param chunkSize 切片大小（字符数），默认500
     * @return 入库结果信息
     */
    public IngestionResult ingestFile(MultipartFile file, int chunkSize) throws IOException {
        long startTime = System.currentTimeMillis();
        String fileName = file.getOriginalFilename();
        
        logger.info("========== 开始文档入库流程 ==========");
        logger.info("文件名: {}", fileName);
        logger.info("文件大小: {} bytes", file.getSize());

        // 1. 生成文档ID
        String docId = generateDocId(fileName);
        logger.info("生成文档ID: {}", docId);

        // 2. 解析文档
        logger.info("步骤1: 解析文档内容...");
        String textContent = documentParserService.parse(file);
        
        if (textContent == null || textContent.trim().isEmpty()) {
            throw new IOException("文档解析结果为空，可能是不支持的文件格式或文件内容为空");
        }
        logger.info("文档解析完成，文本长度: {} 字符", textContent.length());

        // 3. 切片 + 元数据生成
        logger.info("步骤2: 文档切片和元数据生成...");
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument(docId, textContent, chunkSize);
        
        if (chunks.isEmpty()) {
            throw new IOException("文档切片失败，未生成任何切片");
        }
        logger.info("文档切片完成，生成 {} 个切片", chunks.size());

        // 4. 统计信息
        long duration = System.currentTimeMillis() - startTime;
        IngestionResult result = new IngestionResult();
        result.setDocId(docId);
        result.setFileName(fileName);
        result.setChunkCount(chunks.size());
        result.setTextLength(textContent.length());
        result.setProcessingTime(duration);
        result.setSuccess(true);
        result.setMessage("文档入库成功");

        logger.info("========== 文档入库流程完成 ==========");
        logger.info("文档ID: {}", docId);
        logger.info("切片数量: {}", chunks.size());
        logger.info("处理耗时: {} ms", duration);

        return result;
    }

    /**
     * 处理文件上传并入库（使用默认切片大小500）
     */
    public IngestionResult ingestFile(MultipartFile file) throws IOException {
        return ingestFile(file, 500);
    }

    /**
     * 生成文档唯一ID
     */
    private String generateDocId(String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        
        if (fileName != null && !fileName.isEmpty()) {
            String baseName = fileName.contains(".") 
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;
            
            String sanitizedName = baseName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
            return "doc_" + sanitizedName + "_" + timestamp + "_" + shortUuid;
        } else {
            return "doc_unknown_" + timestamp + "_" + shortUuid;
        }
    }

    /**
     * 入库结果信息
     */
    public static class IngestionResult {
        private boolean success;
        private String message;
        private String docId;
        private String fileName;
        private int chunkCount;
        private int textLength;
        private long processingTime;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
        }

        public int getTextLength() {
            return textLength;
        }

        public void setTextLength(int textLength) {
            this.textLength = textLength;
        }

        public long getProcessingTime() {
            return processingTime;
        }

        public void setProcessingTime(long processingTime) {
            this.processingTime = processingTime;
        }

        @Override
        public String toString() {
            return "IngestionResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", docId='" + docId + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", chunkCount=" + chunkCount +
                    ", textLength=" + textLength +
                    ", processingTime=" + processingTime +
                    '}';
        }
    }
}