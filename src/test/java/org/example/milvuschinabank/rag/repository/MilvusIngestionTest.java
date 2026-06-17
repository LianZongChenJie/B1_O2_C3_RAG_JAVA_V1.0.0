package org.example.milvuschinabank.rag.repository;

import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.service.ChunkMetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MilvusIngestionTest {

    private static final Logger logger = LoggerFactory.getLogger(MilvusIngestionTest.class);

    @Autowired
    private MilvusChunkRepository milvusRepository;

    @Autowired
    private ChunkMetadataService chunkMetadataService;

    private String testDocument;
    private String testDocId;

    @BeforeEach
    void setUp() {
        testDocId = "test_milvus_doc_001";
        
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
    @DisplayName("测试1：完整入库流程（生成元数据 + 入库到Milvus）")
    void testCompleteIngestionWorkflow() {
        logger.info("\n========== 测试1：完整入库流程 ==========");
        
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument(
                testDocId, testDocument, 300);

        assertNotNull(chunks);
        assertTrue(chunks.size() > 0, "应该生成至少一个切片");
        
        logger.info("文档ID: {}", testDocId);
        logger.info("生成切片数量: {}", chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            
            logger.info("========== 切片 {} ==========", i);
            logger.info("segId: {}", chunk.getSegId());
            logger.info("docId: {}", chunk.getDocId());
            logger.info("pos: {}", chunk.getPos());
            logger.info("preSegId: {}", chunk.getPreSegId());
            logger.info("nextSegId: {}", chunk.getNextSegId());
            logger.info("内容长度: {} 字符", chunk.getContent().length());
            logger.info("内容预览: {}...", chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())));
            logger.info("标签数量: {}", chunk.getTags().size());
            logger.info("标签列表: {}", chunk.getTags());
            logger.info("语义连贯度: {:.3f}", chunk.getSemanticCohesion());
            logger.info("是否边界: {}", chunk.getSemanticBoundary());
            logger.info("边界类型: {}", chunk.getBoundaryType());
            logger.info("向量维度: {}", chunk.getVector() != null ? chunk.getVector().size() : "null");

            assertNotNull(chunk.getSegId(), "segId不能为空");
            assertEquals(testDocId, chunk.getDocId(), "docId应匹配");
            assertEquals(i, chunk.getPos(), "pos应顺序递增");
            assertNotNull(chunk.getTags(), "tags不能为空");
            assertNotNull(chunk.getSemanticCohesion(), "semanticCohesion不能为空");
            assertNotNull(chunk.getSemanticBoundary(), "semanticBoundary不能为空");
            assertNotNull(chunk.getBoundaryType(), "boundaryType不能为空");
        }

        logger.info("✓ 完整入库流程测试通过");
        logger.info("注意：如果 Milvus 未启动，数据会模拟插入（只打印日志）");
    }

    @Test
    @DisplayName("测试2：批量插入性能测试")
    void testBatchInsertPerformance() {
        logger.info("\n========== 测试2：批量插入性能测试 ==========");
        
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setSegId("perf_test_seg_" + i);
            chunk.setDocId("perf_test_doc");
            chunk.setPos(i);
            chunk.setPreSegId(i > 0 ? "perf_test_seg_" + (i - 1) : null);
            chunk.setNextSegId(i < 99 ? "perf_test_seg_" + (i + 1) : null);
            chunk.setContent("这是性能测试切片内容 " + i + "。外汇管理、汇率、结售汇业务。");
            chunk.setTags(Arrays.asList("biz:外汇", "biz:汇率", "topic:test"));
            chunk.setSemanticCohesion(0.8);
            chunk.setSemanticBoundary(false);
            chunks.add(chunk);
        }

        long startTime = System.currentTimeMillis();
        milvusRepository.insertChunks(chunks);
        long endTime = System.currentTimeMillis();

        logger.info("插入 {} 条切片数据", chunks.size());
        logger.info("耗时: {} ms", (endTime - startTime));
        logger.info("平均每条: {} ms", (endTime - startTime) / chunks.size());

        logger.info("✓ 批量插入性能测试通过");
    }

    @Test
    @DisplayName("测试3：空数据处理")
    void testEmptyDataHandling() {
        logger.info("\n========== 测试3：空数据处理 ==========");
        
        List<DocumentChunk> emptyList = new ArrayList<>();
        milvusRepository.insertChunks(emptyList);
        logger.info("空列表插入: ✓");

        milvusRepository.insertChunks(null);
        logger.info("null插入: ✓");

        logger.info("✓ 空数据处理测试通过");
    }

    @Test
    @DisplayName("测试4：查询功能测试（如果Milvus已连接）")
    void testQueryFunctions() {
        logger.info("\n========== 测试4：查询功能测试 ==========");
        
        List<String> segIds = Arrays.asList("seg_001", "seg_002");
        List<DocumentChunk> result1 = milvusRepository.queryBySegIds(segIds);
        logger.info("queryBySegIds 返回: {} 条", result1.size());

        List<DocumentChunk> result2 = milvusRepository.queryByDocId("test_doc");
        logger.info("queryByDocId 返回: {} 条", result2.size());

        List<String> tags = Arrays.asList("biz:外汇", "biz:汇率");
        List<DocumentChunk> result3 = milvusRepository.queryByTags(tags);
        logger.info("queryByTags 返回: {} 条", result3.size());

        List<DocumentChunk> result4 = milvusRepository.queryAdjacentChunks(segIds);
        logger.info("queryAdjacentChunks 返回: {} 条", result4.size());

        logger.info("✓ 查询功能测试通过（如果Milvus未连接，返回空列表）");
    }

    @Test
    @DisplayName("测试5：向量检索功能测试（如果Milvus已连接）")
    void testVectorSearch() {
        logger.info("\n========== 测试5：向量检索功能测试 ==========");
        
        List<Float> queryVector = new ArrayList<>();
        for (int i = 0; i < 768; i++) {
            queryVector.add(0.01f * i);
        }

        List<MilvusChunkRepository.SearchResult> results = 
                milvusRepository.searchByVector(queryVector, 10, null);
        
        logger.info("向量检索返回: {} 条结果", results.size());

        logger.info("✓ 向量检索功能测试通过（如果Milvus未连接，返回空列表）");
    }

    @Test
    @DisplayName("测试6：多文档独立入库")
    void testMultipleDocumentsIngestion() {
        logger.info("\n========== 测试6：多文档独立入库 ==========");
        
        String doc1Content = "第一章 外汇管理\n外汇业务相关内容";
        String doc2Content = "第二章 存款业务\n存款利率相关内容";
        String doc3Content = "第三章 贷款业务\n贷款利率相关内容";

        List<DocumentChunk> doc1Chunks = chunkMetadataService.ingestDocument(
                "doc_001", doc1Content, 100);
        List<DocumentChunk> doc2Chunks = chunkMetadataService.ingestDocument(
                "doc_002", doc2Content, 100);
        List<DocumentChunk> doc3Chunks = chunkMetadataService.ingestDocument(
                "doc_003", doc3Content, 100);

        logger.info("文档1入库: {} 个切片", doc1Chunks.size());
        logger.info("文档2入库: {} 个切片", doc2Chunks.size());
        logger.info("文档3入库: {} 个切片", doc3Chunks.size());

        for (DocumentChunk chunk : doc1Chunks) {
            assertEquals("doc_001", chunk.getDocId(), "文档1的切片docId应为doc_001");
            assertTrue(chunk.getSegId().startsWith("doc_001_"), 
                    "文档1的切片segId应以doc_001_开头");
        }

        for (DocumentChunk chunk : doc2Chunks) {
            assertEquals("doc_002", chunk.getDocId(), "文档2的切片docId应为doc_002");
            assertTrue(chunk.getSegId().startsWith("doc_002_"), 
                    "文档2的切片segId应以doc_002_开头");
        }

        for (DocumentChunk chunk : doc3Chunks) {
            assertEquals("doc_003", chunk.getDocId(), "文档3的切片docId应为doc_003");
            assertTrue(chunk.getSegId().startsWith("doc_003_"), 
                    "文档3的切片segId应以doc_003_开头");
        }

        logger.info("✓ 多文档独立入库测试通过");
    }
}