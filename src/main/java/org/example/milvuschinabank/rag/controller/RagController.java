package org.example.milvuschinabank.rag.controller;

import org.example.milvuschinabank.rag.model.ABTestRecord;
import org.example.milvuschinabank.rag.service.ABTestService;
import org.example.milvuschinabank.rag.service.DocumentIngestionService;
import org.example.milvuschinabank.rag.service.RagPipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RAG 问答控制器
 * 提供 REST API 接口
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagPipelineService ragPipelineService;

    @Autowired
    private ABTestService abTestService;

    @Autowired
    private DocumentIngestionService documentIngestionService;

    /**
     * 问答接口
     * @param request 请求参数
     * @return 问答响应
     */
    @PostMapping("/query")
    public Map<String, Object> query(@RequestBody QueryRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String sceneName = request.getSceneName() != null ? request.getSceneName() : "default";
            String userQuery = request.getQuery();
            List<Float> queryVector = request.getQueryVector();

            if (userQuery == null || userQuery.isEmpty()) {
                response.put("success", false);
                response.put("message", "查询不能为空");
                return response;
            }

            if (queryVector == null || queryVector.isEmpty()) {
                response.put("success", false);
                response.put("message", "查询向量不能为空");
                return response;
            }

            long startTime = System.currentTimeMillis();

            String answer = ragPipelineService.executeRagPipeline(
                    sceneName, userQuery, queryVector);

            long responseTime = System.currentTimeMillis() - startTime;

            response.put("success", true);
            response.put("answer", answer);
            response.put("responseTime", responseTime);
            response.put("scene", sceneName);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "系统错误: " + e.getMessage());
        }

        return response;
    }

    /**
     * 获取 A/B 测试报告
     * @param scenario 场景名称
     * @return 测试报告
     */
    @GetMapping("/abtest/report")
    public Map<String, Object> getABTestReport(@RequestParam String scenario) {
        Map<String, Object> response = new HashMap<>();

        try {
            ABTestService.ABTestReport report = abTestService.getComparisonReport(scenario);

            response.put("success", true);
            response.put("report", report);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取报告失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 记录 A/B 测试数据
     * @param record 测试记录
     * @return 操作结果
     */
    @PostMapping("/abtest/record")
    public Map<String, Object> recordABTest(@RequestBody ABTestRecord record) {
        Map<String, Object> response = new HashMap<>();

        try {
            record.setRecordId(UUID.randomUUID().toString());
            abTestService.recordTest(record);

            response.put("success", true);
            response.put("recordId", record.getRecordId());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "记录失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 文件上传接口
     * 支持PDF、Word(.doc/.docx)、Excel(.xls/.xlsx)、图片(.png/.jpg/.jpeg/.bmp/.gif/.tiff)
     * 
     * @param file 上传的文件
     * @param chunkSize 切片大小（可选，默认500字符）
     * @return 入库结果
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chunkSize", defaultValue = "500") int chunkSize) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "上传文件不能为空");
                return response;
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                response.put("success", false);
                response.put("message", "文件名不能为空");
                return response;
            }

            long startTime = System.currentTimeMillis();

            DocumentIngestionService.IngestionResult result = 
                    documentIngestionService.ingestFile(file, chunkSize);

            long duration = System.currentTimeMillis() - startTime;

            response.put("success", true);
            response.put("docId", result.getDocId());
            response.put("fileName", result.getFileName());
            response.put("chunkCount", result.getChunkCount());
            response.put("textLength", result.getTextLength());
            response.put("processingTime", duration);
            response.put("message", "文档入库成功");

        } catch (UnsupportedOperationException e) {
            response.put("success", false);
            response.put("message", "不支持的文件格式: " + e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "文档入库失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 批量文件上传接口
     * 
     * @param files 上传的多个文件
     * @param chunkSize 切片大小（可选，默认500字符）
     * @return 批量入库结果
     */
    @PostMapping("/upload/batch")
    public Map<String, Object> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "chunkSize", defaultValue = "500") int chunkSize) {
        Map<String, Object> response = new HashMap<>();

        if (files == null || files.length == 0) {
            response.put("success", false);
            response.put("message", "上传文件不能为空");
            return response;
        }

        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> results = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            Map<String, Object> fileResult = new HashMap<>();
            fileResult.put("fileName", file.getOriginalFilename());

            try {
                DocumentIngestionService.IngestionResult result = 
                        documentIngestionService.ingestFile(file, chunkSize);
                
                fileResult.put("success", true);
                fileResult.put("docId", result.getDocId());
                fileResult.put("chunkCount", result.getChunkCount());
                successCount++;
            } catch (Exception e) {
                fileResult.put("success", false);
                fileResult.put("message", e.getMessage());
                failCount++;
            }

            results.add(fileResult);
        }

        response.put("success", true);
        response.put("totalCount", files.length);
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("results", results);

        return response;
    }

    /**
     * 请求参数
     */
    public static class QueryRequest {
        private String sceneName;
        private String query;
        private List<Float> queryVector;

        public String getSceneName() {
            return sceneName;
        }

        public void setSceneName(String sceneName) {
            this.sceneName = sceneName;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public List<Float> getQueryVector() {
            return queryVector;
        }

        public void setQueryVector(List<Float> queryVector) {
            this.queryVector = queryVector;
        }
    }
}