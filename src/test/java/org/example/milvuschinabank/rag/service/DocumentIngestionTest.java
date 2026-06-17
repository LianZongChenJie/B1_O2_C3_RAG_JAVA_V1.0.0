package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文档解析和入库功能测试
 * 
 * 测试PDF、Word、Excel、图片等文档的解析和入库流程
 * 
 * @author RAG Team
 * @since 2024
 */
@SpringBootTest
public class DocumentIngestionTest {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionTest.class);

    @Autowired
    private DocumentIngestionService documentIngestionService;

    @Test
    @DisplayName("测试1：纯文本文档入库")
    void testPlainTextIngestion() throws IOException {
        logger.info("\n========== 测试1：纯文本文档入库 ==========");
        
        String textContent = 
            "第一章 外汇管理政策\n" +
            "外汇管理是指国家对外汇收支、买卖、借贷、转移以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。\n" +
            "我国实行以市场供求为基础的、单一的、有管理的浮动汇率制度。\n\n" +
            "第二章 结售汇业务管理\n" +
            "实行银行结汇、售汇制度，境内机构的经常项目外汇收入必须结汇给外汇指定银行。\n" +
            "个人年度购汇额度为等值5万美元。";

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test_document.txt",
                "text/plain",
                textContent.getBytes("UTF-8")
        );

        DocumentIngestionService.IngestionResult result = documentIngestionService.ingestFile(mockFile, 200);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "入库应该成功");
        assertNotNull(result.getDocId(), "文档ID不应为空");
        assertTrue(result.getChunkCount() > 0, "应该生成至少一个切片");
        assertTrue(result.getTextLength() > 0, "文本长度应大于0");
        assertTrue(result.getProcessingTime() > 0, "处理时间应大于0");

        logger.info("入库结果:");
        logger.info("  文档ID: {}", result.getDocId());
        logger.info("  文件名: {}", result.getFileName());
        logger.info("  切片数量: {}", result.getChunkCount());
        logger.info("  文本长度: {}", result.getTextLength());
        logger.info("  处理时间: {} ms", result.getProcessingTime());
    }

    @Test
    @DisplayName("测试2：PDF文档解析和入库")
    void testPdfIngestion() throws IOException {
        logger.info("\n========== 测试2：PDF文档解析和入库 ==========");
        
        InputStream pdfStream = getClass().getClassLoader().getResourceAsStream("test.pdf");
        
        if (pdfStream == null) {
            logger.warn("测试PDF文件不存在，跳过此测试");
            return;
        }

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                pdfStream
        );

        DocumentIngestionService.IngestionResult result = documentIngestionService.ingestFile(mockFile, 300);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "PDF入库应该成功");
        assertTrue(result.getChunkCount() > 0, "应该生成至少一个切片");

        logger.info("PDF入库结果:");
        logger.info("  文档ID: {}", result.getDocId());
        logger.info("  切片数量: {}", result.getChunkCount());
        logger.info("  文本长度: {}", result.getTextLength());
    }

    @Test
    @DisplayName("测试3：Word文档解析和入库")
    void testWordIngestion() throws IOException {
        logger.info("\n========== 测试3：Word文档解析和入库 ==========");
        
        InputStream docxStream = getClass().getClassLoader().getResourceAsStream("test.docx");
        
        if (docxStream == null) {
            logger.warn("测试Word文件不存在，跳过此测试");
            return;
        }

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxStream
        );

        DocumentIngestionService.IngestionResult result = documentIngestionService.ingestFile(mockFile, 300);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Word入库应该成功");
        assertTrue(result.getChunkCount() > 0, "应该生成至少一个切片");

        logger.info("Word入库结果:");
        logger.info("  文档ID: {}", result.getDocId());
        logger.info("  切片数量: {}", result.getChunkCount());
        logger.info("  文本长度: {}", result.getTextLength());
    }

    @Test
    @DisplayName("测试4：Excel文档解析和入库")
    void testExcelIngestion() throws IOException {
        logger.info("\n========== 测试4：Excel文档解析和入库 ==========");
        
        InputStream xlsxStream = getClass().getClassLoader().getResourceAsStream("test.xlsx");
        
        if (xlsxStream == null) {
            logger.warn("测试Excel文件不存在，跳过此测试");
            return;
        }

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxStream
        );

        DocumentIngestionService.IngestionResult result = documentIngestionService.ingestFile(mockFile, 300);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "Excel入库应该成功");
        assertTrue(result.getChunkCount() > 0, "应该生成至少一个切片");

        logger.info("Excel入库结果:");
        logger.info("  文档ID: {}", result.getDocId());
        logger.info("  切片数量: {}", result.getChunkCount());
        logger.info("  文本长度: {}", result.getTextLength());
    }

    @Test
    @DisplayName("测试5：不同切片大小对入库的影响")
    void testDifferentChunkSizes() throws IOException {
        logger.info("\n========== 测试5：不同切片大小对入库的影响 ==========");
        
        String textContent = 
            "外汇管理政策是指国家对外汇收支、买卖、借贷、转移以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。" +
            "我国实行以市场供求为基础的、单一的、有管理的浮动汇率制度。" +
            "境内机构的外汇收入可以调回境内或者存放境外。" +
            "实行银行结汇、售汇制度，境内机构的经常项目外汇收入必须结汇给外汇指定银行。" +
            "个人年度购汇额度为等值5万美元。超过年度总额的，需提供相关证明材料。";

        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test_chunks.txt",
                "text/plain",
                textContent.getBytes("UTF-8")
        );

        int[] chunkSizes = {100, 200, 300, 500};
        
        for (int chunkSize : chunkSizes) {
            DocumentIngestionService.IngestionResult result = 
                    documentIngestionService.ingestFile(mockFile, chunkSize);
            
            logger.info("切片大小: {} -> 生成切片数: {}", chunkSize, result.getChunkCount());
            assertTrue(result.isSuccess(), "入库应该成功");
        }
    }

    @Test
    @DisplayName("测试6：空文件处理")
    void testEmptyFileHandling() throws IOException {
        logger.info("\n========== 测试6：空文件处理 ==========");
        
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(IOException.class, () -> {
            documentIngestionService.ingestFile(emptyFile);
        }, "空文件应该抛出异常");

        logger.info("✓ 空文件处理测试通过");
    }

    @Test
    @DisplayName("测试7：不支持的文件格式")
    void testUnsupportedFormat() throws IOException {
        logger.info("\n========== 测试7：不支持的文件格式 ==========");
        
        MultipartFile unsupportedFile = new MockMultipartFile(
                "file",
                "test.mp4",
                "video/mp4",
                new byte[]{0x00, 0x01, 0x02}
        );

        assertThrows(UnsupportedOperationException.class, () -> {
            documentIngestionService.ingestFile(unsupportedFile);
        }, "不支持的格式应该抛出异常");

        logger.info("✓ 不支持文件格式测试通过");
    }

    @Test
    @DisplayName("测试8：文档ID生成规则")
    void testDocIdGeneration() throws IOException {
        logger.info("\n========== 测试8：文档ID生成规则 ==========");
        
        String textContent = "测试文档内容";
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "外汇管理政策.pdf",
                "application/pdf",
                textContent.getBytes("UTF-8")
        );

        DocumentIngestionService.IngestionResult result = documentIngestionService.ingestFile(mockFile);

        String docId = result.getDocId();
        assertNotNull(docId, "文档ID不应为空");
        assertTrue(docId.startsWith("doc_"), "文档ID应以doc_开头");
        assertTrue(docId.contains("外汇管理政策"), "文档ID应包含文件名");
        logger.info("生成的文档ID: {}", docId);

        logger.info("✓ 文档ID生成测试通过");
    }
}