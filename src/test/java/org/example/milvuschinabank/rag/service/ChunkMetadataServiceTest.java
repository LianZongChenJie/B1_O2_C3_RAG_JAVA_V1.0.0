package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.model.SemanticBoundaryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2.1 切片元数据模型设计 - 单元测试
 * 
 * 测试切片的元数据生成和存储服务，验证：
 * - 文档切分和元数据生成的正确性
 * - 语义割裂检测的准确性
 * - 标签提取的完整性
 * - 语义割裂判定标准的合理性
 * - 边界条件处理（空文档、短文档）
 * 
 * @author RAG Team
 * @since 2024
 */
@SpringBootTest
public class ChunkMetadataServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMetadataServiceTest.class);

    @Autowired
    private ChunkMetadataService chunkMetadataService;

    private String testDocument;
    private String testDocId;

    @BeforeEach
    void setUp() {
        testDocId = "test_doc_001";
        
        testDocument = 
            "第一章 外汇管理政策概述\n" +
            "外汇管理是指国家对外汇收支、买卖、借贷、转移以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。\n" +
            "我国实行以市场供求为基础的、单一的、有管理的浮动汇率制度。中国人民银行根据银行间外汇市场形成的价格，公布人民币对主要外币的汇率。\n\n" +
            
            "第二章 结售汇业务管理\n" +
            "境内机构的外汇收入可以调回境内或者存放境外。实行银行结汇、售汇制度，境内机构的经常项目外汇收入必须结汇给外汇指定银行。\n" +
            "个人年度购汇额度为等值5万美元。超过年度总额的，需提供相关证明材料。\n\n" +
            
            "第三章 跨境贸易结算\n" +
            "跨境贸易人民币结算是指经国家允许结算的贸易，以人民币报关并且以人民币结算的贸易结算。\n" +
            "企业开展跨境贸易人民币结算业务，应当选择具有相关资质的银行作为结算银行。\n" +
            "跨境贸易人民币结算不纳入外汇核销管理范围。\n\n" +
            
            "第四章 存款利率政策\n" +
            "活期存款年利率为0.35%。一年期定期存款基准利率为1.50%。\n" +
            "各商业银行可以在基准利率基础上浮动定价。2024-01-15起执行新利率标准。\n" +
            "大额存单起存金额为20万元，利率可上浮至基准利率的1.45倍。";
    }

    @Test
    @DisplayName("测试文档切分和元数据生成")
    void testDocumentChunking() {
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument(testDocId, testDocument, 300);

        assertNotNull(chunks);
        assertTrue(chunks.size() > 0, "应该生成至少一个切片");
        logger.info("生成切片数量: {}", chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);

            logger.info("\n========== 切片 {} ==========", i);
            logger.info("segId: {}", chunk.getSegId());
            logger.info("docId: {}", chunk.getDocId());
            logger.info("pos: {}", chunk.getPos());
            logger.info("preSegId: {}", chunk.getPreSegId());
            logger.info("nextSegId: {}", chunk.getNextSegId());
            logger.info("内容: {}...", chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())));
            logger.info("标签: {}", chunk.getTags());
            logger.info("语义连贯度: {}", chunk.getSemanticCohesion());
            logger.info("是否边界: {}", chunk.getSemanticBoundary());
            logger.info("边界类型: {}", chunk.getBoundaryType());

            assertNotNull(chunk.getSegId(), "segId不应为空");
            assertEquals(testDocId, chunk.getDocId(), "docId应匹配");
            assertEquals(i, chunk.getPos(), "pos应等于索引");
            assertTrue(chunk.getContent().length() > 0, "内容不应为空");

            if (i == 0) {
                assertNull(chunk.getPreSegId(), "首切片的preSegId应为null");
            } else {
                assertNotNull(chunk.getPreSegId(), "非首切片的preSegId不应为null");
                assertEquals(chunks.get(i - 1).getSegId(), chunk.getPreSegId(), "preSegId应指向前一个切片");
            }

            if (i == chunks.size() - 1) {
                assertNull(chunk.getNextSegId(), "尾切片的nextSegId应为null");
            } else {
                assertNotNull(chunk.getNextSegId(), "非尾切片的nextSegId不应为null");
            }
        }
    }

    @Test
    @DisplayName("测试语义割裂检测")
    void testSemanticBoundaryDetection() {
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument(testDocId, testDocument, 300);

        logger.info("\n========== 语义割裂分析 ==========");
        
        int noneCount = 0;
        int paragraphCount = 0;
        int chapterCount = 0;
        int topicSwitchCount = 0;
        int documentCount = 0;

        for (DocumentChunk chunk : chunks) {
            SemanticBoundaryType boundaryType = chunk.getBoundaryType();
            
            switch (boundaryType) {
                case NONE:
                    noneCount++;
                    break;
                case PARAGRAPH:
                    paragraphCount++;
                    break;
                case CHAPTER:
                    chapterCount++;
                    break;
                case TOPIC_SWITCH:
                    topicSwitchCount++;
                    break;
                case DOCUMENT:
                    documentCount++;
                    break;
            }

            logger.info("切片{} [连贯度={:.3f}] 类型: {}", 
                    chunk.getPos(), chunk.getSemanticCohesion(), 
                    boundaryType != null ? boundaryType.getDescription() : "未知");
        }

        logger.info("\n========== 割裂类型统计 ==========");
        logger.info("无割裂: {}", noneCount);
        logger.info("段落边界: {}", paragraphCount);
        logger.info("章节边界: {}", chapterCount);
        logger.info("主题切换: {}", topicSwitchCount);
        logger.info("文档边界: {}", documentCount);

        for (DocumentChunk chunk : chunks) {
            assertNotNull(chunk.getSemanticCohesion(), "连贯度分数不应为空");
            assertTrue(chunk.getSemanticCohesion() >= 0.0 && chunk.getSemanticCohesion() <= 1.0, 
                    "连贯度分数应在0-1之间");
        }
    }

    @Test
    @DisplayName("测试标签提取")
    void testTagExtraction() {
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument(testDocId, testDocument, 300);

        logger.info("\n========== 标签提取分析 ==========");
        
        for (DocumentChunk chunk : chunks) {
            logger.info("\n切片 {} 的标签:", chunk.getPos());
            List<String> tags = chunk.getTags();
            assertNotNull(tags, "标签列表不应为空");

            logger.info("  业务标签: {}", tags.stream().filter(t -> t.startsWith("biz:")).collect(Collectors.toList()));
            logger.info("  实体标签: {}", tags.stream().filter(t -> t.startsWith("entity:")).collect(Collectors.toList()));
            logger.info("  主题标签: {}", tags.stream().filter(t -> t.startsWith("topic:")).collect(Collectors.toList()));
        }

        boolean hasForexBiz = chunks.stream()
                .flatMap(c -> c.getTags().stream())
                .anyMatch(t -> t.equals("biz:外汇"));
        assertTrue(hasForexBiz, "应包含外汇业务标签");

        boolean hasAnyEntity = chunks.stream()
                .flatMap(c -> c.getTags().stream())
                .anyMatch(t -> t.startsWith("entity:"));
        assertTrue(hasAnyEntity, "应包含至少一个实体标签");
    }

    @Test
    @DisplayName("测试语义割裂判定标准")
    void testSemanticBoundaryThresholds() {
        assertEquals(SemanticBoundaryType.NONE, SemanticBoundaryType.fromCohesionScore(0.85));
        assertEquals(SemanticBoundaryType.NONE, SemanticBoundaryType.fromCohesionScore(1.0));
        assertEquals(SemanticBoundaryType.PARAGRAPH, SemanticBoundaryType.fromCohesionScore(0.7));
        assertEquals(SemanticBoundaryType.CHAPTER, SemanticBoundaryType.fromCohesionScore(0.5));
        assertEquals(SemanticBoundaryType.TOPIC_SWITCH, SemanticBoundaryType.fromCohesionScore(0.35));
        assertEquals(SemanticBoundaryType.DOCUMENT, SemanticBoundaryType.fromCohesionScore(0.2));
        assertEquals(SemanticBoundaryType.DOCUMENT, SemanticBoundaryType.fromCohesionScore(0.0));
    }

    @Test
    @DisplayName("测试空文档处理")
    void testEmptyDocument() {
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument("empty_doc", "", 300);
        assertTrue(chunks.isEmpty(), "空文档应返回空列表");
    }

    @Test
    @DisplayName("测试短文档处理")
    void testShortDocument() {
        String shortDoc = "这是一段简短的测试文本。";
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument("short_doc", shortDoc, 300);
        
        assertEquals(1, chunks.size(), "短文档应只生成一个切片");
        assertEquals(shortDoc, chunks.get(0).getContent(), "内容应匹配原文");
        assertNull(chunks.get(0).getPreSegId(), "单一切片的preSegId应为null");
        assertNull(chunks.get(0).getNextSegId(), "单一切片的nextSegId应为null");
    }
}