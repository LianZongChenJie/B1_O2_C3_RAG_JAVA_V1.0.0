package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.model.DocumentChunk;
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

/**
 * 2.2 入库阶段元数据自动生成 - 单元测试
 * 
 * 测试入库阶段元数据自动生成的完整流程，验证：
 * - 全局 pos 顺序号分配的准确性
 * - 前后邻接关系绑定的正确性（首/尾切片置 null）
 * - 业务标签提取（biz: 前缀）
 * - 实体标签提取（entity: 前缀）
 * - 主题标签提取（topic: 前缀）
 * - 完整入库流程（多文档切片统一维护）
 * - 空列表和 null 处理的健壮性
 * - 标签前缀规范验证
 * - 语义连贯度分数计算
 * - 语义边界类型判定
 * - 多文档独立入库
 * 
 * @author RAG Team
 * @since 2024
 */
@SpringBootTest
public class MetadataAutoGenerationTest {

    private static final Logger logger = LoggerFactory.getLogger(MetadataAutoGenerationTest.class);

    @Autowired
    private ChunkMetadataService chunkMetadataService;

    @Autowired
    private TagExtractor tagExtractor;

    private String multiDocContent;

    @BeforeEach
    void setUp() {
        multiDocContent = 
            "第一章 外汇管理政策\n" +
            "外汇管理是指国家对外汇收支、买卖、借贷、转移以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。\n" +
            "我国实行以市场供求为基础的、单一的、有管理的浮动汇率制度。中国人民银行根据银行间外汇市场形成的价格，公布人民币对主要外币的汇率。\n" +
            "境内机构的外汇收入可以调回境内或者存放境外。\n\n" +
            
            "第二章 结售汇业务\n" +
            "实行银行结汇、售汇制度，境内机构的经常项目外汇收入必须结汇给外汇指定银行。\n" +
            "个人年度购汇额度为等值5万美元。超过年度总额的，需提供相关证明材料。\n" +
            "结汇是指外汇收入所有者将其持有的外汇卖给外汇指定银行，按一定汇率取得等值本币的行为。\n\n" +
            
            "第三章 跨境贸易人民币结算\n" +
            "跨境贸易人民币结算是指经国家允许结算的贸易，以人民币报关并且以人民币结算的贸易结算。\n" +
            "企业开展跨境贸易人民币结算业务，应当选择具有相关资质的银行作为结算银行。\n" +
            "跨境贸易人民币结算不纳入外汇核销管理范围。\n" +
            "2024-01-15起执行新政策，简化跨境人民币结算流程。\n\n" +
            
            "第四章 存款利率管理\n" +
            "活期存款年利率为0.35%。一年期定期存款基准利率为1.50%。\n" +
            "各商业银行可以在基准利率基础上浮动定价。\n" +
            "大额存单起存金额为20万元，利率可上浮至基准利率的1.45倍。\n" +
            "USD存款利率为2.5%，EUR存款利率为1.8%。";
    }

    @Test
    @DisplayName("测试1：全局pos顺序号分配")
    void testGlobalPosAssignment() {
        logger.info("\n========== 测试1：全局pos顺序号分配 ==========");
        
        List<String> rawChunks = Arrays.asList(
            "切片内容1：外汇管理政策概述",
            "切片内容2：结售汇业务管理",
            "切片内容3：跨境贸易结算",
            "切片内容4：存款利率政策",
            "切片内容5：其他业务"
        );

        List<DocumentChunk> chunks = chunkMetadataService.processChunks(rawChunks, "doc_test_001");

        logger.info("切片数量: {}", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            logger.info("切片{}: pos={}, 预期pos={}, 匹配={}", 
                    i, chunk.getPos(), i, chunk.getPos() == i);
            
            assertEquals(i, chunk.getPos(), "pos应该从0开始顺序递增");
        }

        for (int i = 0; i < chunks.size() - 1; i++) {
            assertEquals(1, chunks.get(i + 1).getPos() - chunks.get(i).getPos(), 
                    "相邻切片的pos差值应为1");
        }

        logger.info("✓ 全局pos分配测试通过");
    }

    @Test
    @DisplayName("测试2：前后邻接关系绑定（首/尾置null）")
    void testAdjacentLinkageBinding() {
        logger.info("\n========== 测试2：前后邻接关系绑定 ==========");
        
        List<String> rawChunks = Arrays.asList(
            "第一个切片内容",
            "第二个切片内容",
            "第三个切片内容",
            "第四个切片内容"
        );

        List<DocumentChunk> chunks = chunkMetadataService.processChunks(rawChunks, "doc_test_002");

        logger.info("切片总数: {}", chunks.size());
        
        DocumentChunk firstChunk = chunks.get(0);
        logger.info("\n首切片 (pos=0):");
        logger.info("  segId: {}", firstChunk.getSegId());
        logger.info("  preSegId: {} (预期: null)", firstChunk.getPreSegId());
        logger.info("  nextSegId: {} (预期: 不为null)", firstChunk.getNextSegId());
        
        assertNull(firstChunk.getPreSegId(), "首切片的preSegId应该为null");
        assertNotNull(firstChunk.getNextSegId(), "首切片的nextSegId不应该为null");
        assertTrue(firstChunk.getNextSegId().startsWith("doc_test_002_seg_1_"), 
                "首切片的nextSegId应该指向第二个切片");

        for (int i = 1; i < chunks.size() - 1; i++) {
            DocumentChunk chunk = chunks.get(i);
            logger.info("\n中间切片 (pos={}):", i);
            logger.info("  segId: {}", chunk.getSegId());
            logger.info("  preSegId: {}", chunk.getPreSegId());
            logger.info("  nextSegId: {}", chunk.getNextSegId());
            
            assertNotNull(chunk.getPreSegId(), "中间切片的preSegId不应该为null");
            assertNotNull(chunk.getNextSegId(), "中间切片的nextSegId不应该为null");
            String expectedPrePrefix = "doc_test_002_seg_" + (i - 1) + "_";
            assertTrue(chunk.getPreSegId().startsWith(expectedPrePrefix), 
                    "preSegId应该指向前一个切片");
            String expectedNextPrefix = "doc_test_002_seg_" + (i + 1) + "_";
            assertTrue(chunk.getNextSegId().startsWith(expectedNextPrefix), 
                    "nextSegId应该指向后一个切片");
        }

        DocumentChunk lastChunk = chunks.get(chunks.size() - 1);
        logger.info("\n尾切片 (pos={}):", chunks.size() - 1);
        logger.info("  segId: {}", lastChunk.getSegId());
        logger.info("  preSegId: {} (预期: 不为null)", lastChunk.getPreSegId());
        logger.info("  nextSegId: {} (预期: null)", lastChunk.getNextSegId());
        
        assertNotNull(lastChunk.getPreSegId(), "尾切片的preSegId不应该为null");
        assertNull(lastChunk.getNextSegId(), "尾切片的nextSegId应该为null");
        assertEquals(chunks.get(chunks.size() - 2).getSegId(), lastChunk.getPreSegId(), 
                "尾切片的preSegId应该指向前一个切片");

        logger.info("\n✓ 前后邻接关系绑定测试通过");
    }

    @Test
    @DisplayName("测试3：单一切片的前后邻接（都为null）")
    void testSingleChunkAdjacentLinkage() {
        logger.info("\n========== 测试3：单一切片的前后邻接 ==========");
        
        List<String> rawChunks = Arrays.asList("唯一的切片内容");

        List<DocumentChunk> chunks = chunkMetadataService.processChunks(rawChunks, "doc_test_003");

        assertEquals(1, chunks.size(), "应该只有一个切片");
        
        DocumentChunk chunk = chunks.get(0);
        logger.info("单一切片:");
        logger.info("  preSegId: {} (预期: null)", chunk.getPreSegId());
        logger.info("  nextSegId: {} (预期: null)", chunk.getNextSegId());
        
        assertNull(chunk.getPreSegId(), "单一切片的preSegId应该为null");
        assertNull(chunk.getNextSegId(), "单一切片的nextSegId应该为null");

        logger.info("✓ 单一切片测试通过");
    }

    @Test
    @DisplayName("测试4：业务标签提取（biz:前缀）")
    void testBusinessTagExtraction() {
        logger.info("\n========== 测试4：业务标签提取 ==========");
        
        String content = "外汇管理、汇率制度、结售汇业务、跨境贸易结算、信用证、贸易融资";
        List<String> tags = tagExtractor.extractTags(content, "doc", 0);

        logger.info("输入内容: {}", content);
        logger.info("提取的标签: {}", tags);
        
        List<String> businessTags = new ArrayList<>();
        for (String tag : tags) {
            if (tag.startsWith("biz:")) {
                businessTags.add(tag);
                logger.info("  业务标签: {}", tag);
            }
        }

        assertTrue(businessTags.contains("biz:外汇"), "应包含biz:外汇标签");
        assertTrue(businessTags.contains("biz:汇率"), "应包含biz:汇率标签");
        assertTrue(businessTags.contains("biz:跨境"), "应包含biz:跨境标签");
        assertTrue(businessTags.contains("biz:结算"), "应包含biz:结算标签");
        assertTrue(businessTags.contains("biz:信用证"), "应包含biz:信用证标签");

        logger.info("✓ 业务标签提取测试通过");
    }

    @Test
    @DisplayName("测试5：实体标签提取（entity:前缀）")
    void testEntityTagExtraction() {
        logger.info("\n========== 测试5：实体标签提取 ==========");
        
        String content = "2024-01-15起，USD存款利率为2.5%，EUR存款利率为1.8%，活期利率0.35%";
        List<String> tags = tagExtractor.extractTags(content, "doc", 0);

        logger.info("输入内容: {}", content);
        logger.info("提取的标签: {}", tags);
        
        List<String> entityTags = new ArrayList<>();
        for (String tag : tags) {
            if (tag.startsWith("entity:")) {
                entityTags.add(tag);
                logger.info("  实体标签: {}", tag);
            }
        }

        boolean hasUSD = entityTags.stream().anyMatch(t -> t.contains("USD"));
        boolean hasEUR = entityTags.stream().anyMatch(t -> t.contains("EUR"));
        assertTrue(hasUSD, "应包含USD货币实体");
        assertTrue(hasEUR, "应包含EUR货币实体");

        boolean hasRate = entityTags.stream().anyMatch(t -> t.startsWith("entity:rate:"));
        assertTrue(hasRate, "应包含利率实体");

        boolean hasDate = entityTags.stream().anyMatch(t -> t.startsWith("entity:date:"));
        assertTrue(hasDate, "应包含日期实体");

        logger.info("✓ 实体标签提取测试通过");
    }

    @Test
    @DisplayName("测试6：主题标签提取（topic:前缀）")
    void testTopicTagExtraction() {
        logger.info("\n========== 测试6：主题标签提取 ==========");
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("这是一段很长的文本内容，需要超过500个字符才能触发long_text标签。");
        }
        String longContent = sb.toString();
        logger.info("长文本长度: {}", longContent.length());
        List<String> longTags = tagExtractor.extractTags(longContent, "doc", 0);
        logger.info("长文本标签: {}", longTags);
        assertTrue(longContent.length() > 500, "测试文本长度应大于500");
        assertTrue(longTags.contains("topic:long_text"), "长文本应包含topic:long_text标签");

        String dataContent = "2024年存款金额为100万元，贷款金额为200万元";
        List<String> dataTags = tagExtractor.extractTags(dataContent, "doc", 0);
        logger.info("数据文本标签: {}", dataTags);
        assertTrue(dataTags.contains("topic:data"), "包含数字的文本应包含topic:data标签");

        String faqContent = "如何办理外汇业务？需要准备哪些材料？";
        List<String> faqTags = tagExtractor.extractTags(faqContent, "doc", 0);
        logger.info("FAQ文本标签: {}", faqTags);
        assertTrue(faqTags.contains("topic:faq"), "包含问句的文本应包含topic:faq标签");

        String chapterContent = "第一章外汇管理政策概述";
        List<String> chapterTags = tagExtractor.extractTags(chapterContent, "doc", 0);
        logger.info("章节文本标签: {}", chapterTags);
        
        String chapterContent2 = "1、外汇管理政策概述";
        List<String> chapterTags2 = tagExtractor.extractTags(chapterContent2, "doc", 0);
        logger.info("章节文本标签2: {}", chapterTags2);
        assertTrue(chapterTags2.contains("topic:chapter"), "章节标题应包含topic:chapter标签");

        logger.info("✓ 主题标签提取测试通过");
    }

    @Test
    @DisplayName("测试7：完整入库流程（多文档切片统一维护）")
    void testCompleteIngestionWorkflow() {
        logger.info("\n========== 测试7：完整入库流程 ==========");
        
        List<DocumentChunk> chunks = chunkMetadataService.ingestDocument(
                "doc_bank_001", multiDocContent, 200);

        logger.info("文档ID: doc_bank_001");
        logger.info("原始文档长度: {} 字符", multiDocContent.length());
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

            assertNotNull(chunk.getSegId(), "segId不能为空");
            assertEquals("doc_bank_001", chunk.getDocId(), "docId应匹配");
            assertEquals(i, chunk.getPos(), "pos应顺序递增");
            assertNotNull(chunk.getTags(), "tags不能为空");
            assertNotNull(chunk.getSemanticCohesion(), "semanticCohesion不能为空");
            assertNotNull(chunk.getSemanticBoundary(), "semanticBoundary不能为空");
            assertNotNull(chunk.getBoundaryType(), "boundaryType不能为空");

            boolean hasBusinessTag = chunk.getTags().stream().anyMatch(t -> t.startsWith("biz:"));
            boolean hasEntityTag = chunk.getTags().stream().anyMatch(t -> t.startsWith("entity:"));
            boolean hasTopicTag = chunk.getTags().stream().anyMatch(t -> t.startsWith("topic:"));
            
            logger.info("  包含业务标签: {}", hasBusinessTag);
            logger.info("  包含实体标签: {}", hasEntityTag);
            logger.info("  包含主题标签: {}", hasTopicTag);
        }

        logger.info("========== 验证前后链接完整性 ==========");
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            
            if (i == 0) {
                assertNull(chunk.getPreSegId(), "首切片preSegId应为null");
            } else {
                String expectedPrePrefix = "doc_bank_001_seg_" + (i - 1) + "_";
                assertTrue(chunk.getPreSegId().startsWith(expectedPrePrefix), 
                        "切片" + i + "的preSegId应指向切片" + (i - 1));
            }
            
            if (i == chunks.size() - 1) {
                assertNull(chunk.getNextSegId(), "尾切片nextSegId应为null");
            } else {
                String expectedNextPrefix = "doc_bank_001_seg_" + (i + 1) + "_";
                assertTrue(chunk.getNextSegId().startsWith(expectedNextPrefix), 
                        "切片" + i + "的nextSegId应指向切片" + (i + 1));
            }
        }

        logger.info("✓ 完整入库流程测试通过");
    }

    @Test
    @DisplayName("测试8：空列表和null处理")
    void testEmptyAndNullHandling() {
        logger.info("\n========== 测试8：空列表和null处理 ==========");
        
        List<DocumentChunk> emptyResult = chunkMetadataService.processChunks(
                new ArrayList<>(), "doc_empty");
        assertTrue(emptyResult.isEmpty(), "空列表应返回空结果");
        logger.info("空列表处理: ✓");

        List<DocumentChunk> nullResult = chunkMetadataService.processChunks(null, "doc_null");
        assertTrue(nullResult.isEmpty(), "null列表应返回空结果");
        logger.info("null列表处理: ✓");

        List<DocumentChunk> emptyContentResult = chunkMetadataService.ingestDocument(
                "doc_empty_content", "", 200);
        assertTrue(emptyContentResult.isEmpty(), "空字符串应返回空结果");
        logger.info("空字符串处理: ✓");

        List<DocumentChunk> nullContentResult = chunkMetadataService.ingestDocument(
                "doc_null_content", null, 200);
        assertTrue(nullContentResult.isEmpty(), "null内容应返回空结果");
        logger.info("null内容处理: ✓");

        logger.info("✓ 空列表和null处理测试通过");
    }

    @Test
    @DisplayName("测试9：标签前缀规范验证")
    void testTagPrefixConvention() {
        logger.info("\n========== 测试9：标签前缀规范验证 ==========");
        
        List<String> rawChunks = Arrays.asList(
            "外汇管理政策，USD汇率为7.2，2024-01-15执行新政策",
            "存款利率1.5%，EUR存款利率2.0%，如何办理业务？"
        );

        List<DocumentChunk> chunks = chunkMetadataService.processChunks(rawChunks, "doc_test");

        for (DocumentChunk chunk : chunks) {
            logger.info("\n切片 {} 的标签:", chunk.getPos());
            for (String tag : chunk.getTags()) {
                assertTrue(
                    tag.startsWith("biz:") || tag.startsWith("entity:") || tag.startsWith("topic:"),
                    "标签 '" + tag + "' 必须符合前缀规范 (biz:/entity:/topic:)"
                );
                logger.info("  {} - 符合规范 ✓", tag);
            }
        }

        logger.info("✓ 标签前缀规范验证通过");
    }

    @Test
    @DisplayName("测试10：语义连贯度分数计算")
    void testSemanticCohesionCalculation() {
        logger.info("\n========== 测试10：语义连贯度分数计算 ==========");
        
        List<String> rawChunks = Arrays.asList(
            "外汇管理政策概述，汇率制度管理",
            "外汇管理政策概述，汇率制度管理",
            "结售汇业务管理，跨境贸易结算",
            "存款利率管理，USD EUR 存款利率"
        );

        List<DocumentChunk> chunks = chunkMetadataService.processChunks(rawChunks, "doc_test");

        for (DocumentChunk chunk : chunks) {
            Double cohesion = chunk.getSemanticCohesion();
            logger.info("切片{}: 连贯度={:.3f}", chunk.getPos(), cohesion);
            
            assertNotNull(cohesion, "连贯度分数不应为空");
            assertTrue(cohesion >= 0.0 && cohesion <= 1.0, "连贯度分数应在0-1之间");
        }

        logger.info("✓ 语义连贯度分数计算测试通过");
    }

    @Test
    @DisplayName("测试11：语义边界类型判定")
    void testSemanticBoundaryTypeDetection() {
        logger.info("\n========== 测试11：语义边界类型判定 ==========");
        
        List<String> rawChunks = Arrays.asList(
            "第一章 外汇管理政策概述",
            "外汇管理相关内容",
            "第二章 结售汇业务",
            "结售汇相关内容",
            "第三章 存款利率",
            "存款利率相关内容"
        );

        List<DocumentChunk> chunks = chunkMetadataService.processChunks(rawChunks, "doc_test");

        for (DocumentChunk chunk : chunks) {
            logger.info("切片{}: 连贯度={:.3f}, 边界={}, 边界类型={}", 
                    chunk.getPos(), 
                    chunk.getSemanticCohesion(),
                    chunk.getSemanticBoundary(),
                    chunk.getBoundaryType().getDescription());
            
            assertNotNull(chunk.getBoundaryType(), "边界类型不应为空");
        }

        logger.info("✓ 语义边界类型判定测试通过");
    }

    @Test
    @DisplayName("测试12：多文档独立入库")
    void testMultipleDocumentsIngestion() {
        logger.info("\n========== 测试12：多文档独立入库 ==========");
        
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