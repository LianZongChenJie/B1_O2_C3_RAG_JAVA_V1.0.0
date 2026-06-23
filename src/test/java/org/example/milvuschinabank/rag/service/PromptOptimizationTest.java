//package org.example.milvuschinabank.rag.service;
//
//import org.junit.jupiter.api.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 4.2 提示词优化策略 - 单元测试
// *
// * 测试系统提示词优化策略，验证：
// * - 角色定义：明确 AI 身份和能力范围
// * - 输出格式约束：规范回答结构和长度
// * - 思维链引导：提升任务意图理解与回复质量
// * - 查询扩写提示词：多角度的查询优化
// * - 重排序提示词：相关性评估与 JSON 输出
// * - 提示词结构完整性：各部分组合正确
// * - 边界条件处理：空输入、长输入等
// *
// * @author RAG Team
// * @since 2024
// */
//public class PromptOptimizationTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(PromptOptimizationTest.class);
//
//    private PromptOptimizer promptOptimizer;
//
//    @BeforeEach
//    void setUp() {
//        promptOptimizer = new PromptOptimizer();
//    }
//
//    @Test
//    @DisplayName("测试1：角色定义 - 专业身份与能力")
//    void testRoleDefinition() {
//        logger.info("\n========== 测试1：角色定义 - 专业身份与能力 ==========");
//
//        String userQuery = "个人购汇的具体额度是多少？";
//        String context = "[段落 1]\n个人购汇的具体额度是每年5万美元。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证角色定义
//        assertTrue(prompt.contains("## 角色定义"), "应包含角色定义部分");
//        assertTrue(prompt.contains("中国银行交银外汇助手的专业顾问"), "应明确 AI 身份");
//        assertTrue(prompt.contains("精通外汇业务"), "应说明专业能力");
//        assertTrue(prompt.contains("国际结算"), "应说明业务领域");
//        assertTrue(prompt.contains("贸易融资"), "应说明业务范围");
//        assertTrue(prompt.contains("准确理解用户问题"), "应说明理解能力");
//        assertTrue(prompt.contains("回答专业、准确、简洁"), "应说明回答标准");
//
//        logger.info("✓ 角色定义测试通过");
//    }
//
//    @Test
//    @DisplayName("测试2：输出格式约束 - 回答规范")
//    void testOutputConstraints() {
//        logger.info("\n========== 测试2：输出格式约束 - 回答规范 ==========");
//
//        String userQuery = "外汇业务的办理流程是什么？";
//        String context = "[段落 1]\n外汇业务办理流程包括：申请、审核、办理。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证输出格式约束
//        assertTrue(prompt.contains("## 输出要求"), "应包含输出要求部分");
//        assertTrue(prompt.contains("直接回答问题，不要复述问题"), "应要求直接回答");
//        assertTrue(prompt.contains("使用清晰的段落结构"), "应要求清晰结构");
//        assertTrue(prompt.contains("涉及数字、利率、汇率等关键信息时必须准确引用"), "应要求准确引用");
//        assertTrue(prompt.contains("如果答案涉及多个方面，请分点说明"), "应要求分点说明");
//        assertTrue(prompt.contains("回答长度控制在 200-500 字之间"), "应限制回答长度");
//        assertTrue(prompt.contains("使用专业但易懂的语言"), "应要求语言风格");
//        assertTrue(prompt.contains("如果参考资料不足以回答问题，请明确说明"), "应要求说明不足");
//
//        logger.info("✓ 输出格式约束测试通过");
//    }
//
//    @Test
//    @DisplayName("测试3：思维链引导 - 内部推理步骤")
//    void testChainOfThoughtGuide() {
//        logger.info("\n========== 测试3：思维链引导 - 内部推理步骤 ==========");
//
//        String userQuery = "跨境贸易结算的流程和注意事项有哪些？";
//        String context = "[段落 1]\n跨境贸易结算流程包括：合同签订、信用证开立、货物发运、单据审核、付款结算。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证思维链引导
//        assertTrue(prompt.contains("## 思考步骤（内部推理，不要输出）"), "应包含思考步骤");
//        assertTrue(prompt.contains("理解用户问题的核心意图"), "应引导理解意图");
//        assertTrue(prompt.contains("在参考资料中定位与问题相关的段落"), "应引导定位资料");
//        assertTrue(prompt.contains("提取关键信息并验证准确性"), "应引导提取验证");
//        assertTrue(prompt.contains("组织答案结构：核心答案 → 补充说明 → 注意事项"), "应引导组织结构");
//        assertTrue(prompt.contains("检查答案是否完整回答了用户问题"), "应引导检查完整性");
//        assertTrue(prompt.contains("确保语言专业、准确、符合银行业务规范"), "应引导语言规范");
//        assertTrue(prompt.contains("## 回答"), "应包含回答部分");
//
//        logger.info("✓ 思维链引导测试通过");
//    }
//
//    @Test
//    @DisplayName("测试4：参考资料部分 - 上下文组织")
//    void testContextSection() {
//        logger.info("\n========== 测试4：参考资料部分 - 上下文组织 ==========");
//
//        String userQuery = "存款利率是多少？";
//        String context = "[段落 1]\n一年期存款利率为1.5%。\n\n[段落 2]\n两年期存款利率为2.0%。\n\n[段落 3]\n三年期存款利率为2.5%。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证参考资料部分
//        assertTrue(prompt.contains("## 参考资料"), "应包含参考资料部分");
//        assertTrue(prompt.contains("以下是与用户问题相关的参考资料"), "应说明参考资料用途");
//        assertTrue(prompt.contains("请基于这些资料回答问题"), "应要求基于资料回答");
//        assertTrue(prompt.contains("一年期存款利率为1.5%"), "应包含第1段内容");
//        assertTrue(prompt.contains("两年期存款利率为2.0%"), "应包含第2段内容");
//        assertTrue(prompt.contains("三年期存款利率为2.5%"), "应包含第3段内容");
//
//        logger.info("✓ 参考资料部分测试通过");
//    }
//
//    @Test
//    @DisplayName("测试5：用户问题部分 - 问题嵌入")
//    void testQuestionSection() {
//        logger.info("\n========== 测试5：用户问题部分 - 问题嵌入 ==========");
//
//        String userQuery = "个人购汇的具体额度是多少？超出额度怎么办？";
//        String context = "[段落 1]\n个人购汇额度为每年5万美元。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证用户问题部分
//        assertTrue(prompt.contains("## 用户问题"), "应包含用户问题部分");
//        assertTrue(prompt.contains("个人购汇的具体额度是多少？超出额度怎么办？"), "应完整嵌入用户问题");
//
//        logger.info("✓ 用户问题部分测试通过");
//    }
//
//    @Test
//    @DisplayName("测试6：提示词结构完整性 - 各部分组合")
//    void testPromptStructureCompleteness() {
//        logger.info("\n========== 测试6：提示词结构完整性 - 各部分组合 ==========");
//
//        String userQuery = "外汇业务如何办理？";
//        String context = "[段落 1]\n外汇业务办理流程：携带身份证到银行网点办理。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证结构完整性
//        String[] requiredSections = {
//            "## 角色定义",
//            "## 参考资料",
//            "## 用户问题",
//            "## 输出要求",
//            "## 思考步骤（内部推理，不要输出）",
//            "## 回答"
//        };
//
//        for (String section : requiredSections) {
//            assertTrue(prompt.contains(section), "应包含部分: " + section);
//            logger.info("✓ 包含部分: {}", section);
//        }
//
//        // 验证部分顺序
//        int roleIndex = prompt.indexOf("## 角色定义");
//        int contextIndex = prompt.indexOf("## 参考资料");
//        int questionIndex = prompt.indexOf("## 用户问题");
//        int outputIndex = prompt.indexOf("## 输出要求");
//        int thoughtIndex = prompt.indexOf("## 思考步骤");
//        int answerIndex = prompt.indexOf("## 回答");
//
//        assertTrue(roleIndex < contextIndex, "角色定义应在参考资料之前");
//        assertTrue(contextIndex < questionIndex, "参考资料应在用户问题之前");
//        assertTrue(questionIndex < outputIndex, "用户问题应在输出要求之前");
//        assertTrue(outputIndex < thoughtIndex, "输出要求应在思考步骤之前");
//        assertTrue(thoughtIndex < answerIndex, "思考步骤应在回答之前");
//
//        logger.info("✓ 提示词结构完整性测试通过");
//    }
//
//    @Test
//    @DisplayName("测试7：查询扩写提示词 - 多角度查询优化")
//    void testQueryExpansionPrompt() {
//        logger.info("\n========== 测试7：查询扩写提示词 - 多角度查询优化 ==========");
//
//        String originalQuery = "个人购汇的具体额度是多少？";
//
//        String prompt = promptOptimizer.buildQueryExpansionPrompt(originalQuery);
//
//        logger.info("原始查询: {}", originalQuery);
//        logger.info("生成的扩写提示词:\n{}", prompt);
//
//        // 验证扩写提示词
//        assertTrue(prompt.contains("## 角色"), "应包含角色部分");
//        assertTrue(prompt.contains("你是专业的查询优化助手"), "应说明 AI 身份");
//        assertTrue(prompt.contains("## 任务"), "应包含任务部分");
//        assertTrue(prompt.contains("请将以下用户问题扩写为多个不同角度的查询语句"), "应说明扩写任务");
//        assertTrue(prompt.contains("以提高检索召回率"), "应说明扩写目的");
//        assertTrue(prompt.contains("## 原始问题"), "应包含原始问题部分");
//        assertTrue(prompt.contains("个人购汇的具体额度是多少？"), "应嵌入原始问题");
//        assertTrue(prompt.contains("## 要求"), "应包含要求部分");
//        assertTrue(prompt.contains("生成 3-5 个不同角度的查询语句"), "应要求多角度");
//        assertTrue(prompt.contains("包含同义词、近义词、相关业务术语"), "应要求包含术语");
//        assertTrue(prompt.contains("保持简洁，每个查询不超过 30 字"), "应限制长度");
//        assertTrue(prompt.contains("输出格式：每行一个查询，用 | 分隔"), "应说明输出格式");
//        assertTrue(prompt.contains("## 扩写结果"), "应包含结果部分");
//
//        logger.info("✓ 查询扩写提示词测试通过");
//    }
//
//    @Test
//    @DisplayName("测试8：重排序提示词 - 相关性评估")
//    void testRerankPrompt() {
//        logger.info("\n========== 测试8：重排序提示词 - 相关性评估 ==========");
//
//        String userQuery = "外汇业务的利率是多少？";
//        int chunkCount = 5;
//
//        String prompt = promptOptimizer.buildRerankPrompt(userQuery, chunkCount);
//
//        logger.info("用户查询: {}", userQuery);
//        logger.info("切片数量: {}", chunkCount);
//        logger.info("生成的重排序提示词:\n{}", prompt);
//
//        // 验证重排序提示词
//        assertTrue(prompt.contains("## 角色"), "应包含角色部分");
//        assertTrue(prompt.contains("你是专业的文档相关性评估助手"), "应说明 AI 身份");
//        assertTrue(prompt.contains("## 任务"), "应包含任务部分");
//        assertTrue(prompt.contains("请评估以下 5 个文档片段与用户问题的相关性"), "应说明评估任务");
//        assertTrue(prompt.contains("并给出 0-100 的相关性分数"), "应说明评分范围");
//        assertTrue(prompt.contains("## 用户问题"), "应包含用户问题部分");
//        assertTrue(prompt.contains("外汇业务的利率是多少？"), "应嵌入用户问题");
//        assertTrue(prompt.contains("## 输出格式"), "应包含输出格式部分");
//        assertTrue(prompt.contains("请输出 JSON 数组"), "应要求 JSON 输出");
//        assertTrue(prompt.contains("\"chunk_index\": 片段序号"), "应包含 chunk_index 字段");
//        assertTrue(prompt.contains("\"relevance_score\": 相关性分数(0-100)"), "应包含 relevance_score 字段");
//        assertTrue(prompt.contains("\"reason\": \"简要说明相关性原因\""), "应包含 reason 字段");
//        assertTrue(prompt.contains("## 评估结果"), "应包含结果部分");
//
//        logger.info("✓ 重排序提示词测试通过");
//    }
//
//    @Test
//    @DisplayName("测试9：边界条件 - 空上下文")
//    void testEmptyContext() {
//        logger.info("\n========== 测试9：边界条件 - 空上下文 ==========");
//
//        String userQuery = "个人购汇的具体额度是多少？";
//        String context = "";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证即使空上下文也能生成完整提示词
//        assertNotNull(prompt, "提示词不应为 null");
//        assertTrue(prompt.contains("## 角色定义"), "应包含角色定义");
//        assertTrue(prompt.contains("## 参考资料"), "应包含参考资料部分");
//        assertTrue(prompt.contains("## 用户问题"), "应包含用户问题部分");
//        assertTrue(prompt.contains("个人购汇的具体额度是多少？"), "应包含用户问题");
//
//        logger.info("✓ 空上下文边界条件测试通过");
//    }
//
//    @Test
//    @DisplayName("测试10：边界条件 - 长上下文")
//    void testLongContext() {
//        logger.info("\n========== 测试10：边界条件 - 长上下文 ==========");
//
//        String userQuery = "外汇业务的完整流程是什么？";
//        StringBuilder contextBuilder = new StringBuilder();
//
//        // 生成长上下文
//        for (int i = 1; i <= 20; i++) {
//            contextBuilder.append("[段落 ").append(i).append("]\n");
//            contextBuilder.append("这是第").append(i).append("段内容，描述外汇业务的某个流程细节。");
//            contextBuilder.append("包括申请条件、所需材料、办理步骤、注意事项等。\n\n");
//        }
//
//        String context = contextBuilder.toString();
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("上下文长度: {}", context.length());
//        logger.info("生成的提示词长度: {}", prompt.length());
//
//        // 验证长上下文处理
//        assertNotNull(prompt, "提示词不应为 null");
//        assertTrue(prompt.contains("## 角色定义"), "应包含角色定义");
//        assertTrue(prompt.contains("## 参考资料"), "应包含参考资料部分");
//        assertTrue(prompt.contains("[段落 1]"), "应包含第1段");
//        assertTrue(prompt.contains("[段落 20]"), "应包含第20段");
//        assertTrue(prompt.contains("## 用户问题"), "应包含用户问题部分");
//
//        logger.info("✓ 长上下文边界条件测试通过");
//    }
//
//    @Test
//    @DisplayName("测试11：边界条件 - 空查询")
//    void testEmptyQuery() {
//        logger.info("\n========== 测试11：边界条件 - 空查询 ==========");
//
//        String userQuery = "";
//        String context = "[段落 1]\n外汇业务相关内容。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的提示词:\n{}", prompt);
//
//        // 验证空查询处理
//        assertNotNull(prompt, "提示词不应为 null");
//        assertTrue(prompt.contains("## 用户问题"), "应包含用户问题部分");
//
//        logger.info("✓ 空查询边界条件测试通过");
//    }
//
//    @Test
//    @DisplayName("测试12：提示词长度统计")
//    void testPromptLengthStatistics() {
//        logger.info("\n========== 测试12：提示词长度统计 ==========");
//
//        String userQuery = "个人购汇的具体额度是多少？超出额度怎么办？需要什么材料？";
//        String context = "[段落 1]\n个人购汇的具体额度是每年5万美元。\n\n" +
//                        "[段落 2]\n超出额度需要向外汇局申请审批。\n\n" +
//                        "[段落 3]\n审批材料包括：身份证、购汇申请书、相关证明材料。\n\n";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("用户问题长度: {} 字符", userQuery.length());
//        logger.info("上下文长度: {} 字符", context.length());
//        logger.info("生成的提示词长度: {} 字符", prompt.length());
//
//        // 统计各部分长度
//        int roleLength = promptOptimizer.buildQaPrompt("", "").indexOf("## 参考资料");
//        int outputLength = prompt.indexOf("## 思考步骤") - prompt.indexOf("## 输出要求");
//        int thoughtLength = prompt.indexOf("## 回答") - prompt.indexOf("## 思考步骤");
//
//        logger.info("角色定义部分长度: {} 字符", roleLength);
//        logger.info("输出要求部分长度: {} 字符", outputLength);
//        logger.info("思考步骤部分长度: {} 字符", thoughtLength);
//
//        // 验证提示词长度合理
//        assertTrue(prompt.length() > 500, "提示词长度应大于500字符");
//        assertTrue(prompt.length() < 5000, "提示词长度应小于5000字符");
//
//        logger.info("✓ 提示词长度统计测试通过");
//    }
//
//    @Test
//    @DisplayName("测试13：多种业务场景提示词生成")
//    void testMultipleBusinessScenarios() {
//        logger.info("\n========== 测试13：多种业务场景提示词生成 ==========");
//
//        String[][] scenarios = {
//            {"个人购汇", "个人购汇的具体额度是多少？", "[段落 1]\n个人购汇额度为每年5万美元。\n\n"},
//            {"跨境结算", "跨境贸易结算的流程是什么？", "[段落 1]\n跨境结算流程包括：合同签订、信用证开立、货物发运、付款结算。\n\n"},
//            {"存款利率", "当前存款利率是多少？", "[段落 1]\n一年期存款利率1.5%，两年期2.0%，三年期2.5%。\n\n"},
//            {"外汇审批", "超出购汇额度如何申请审批？", "[段落 1]\n超出额度需向外汇局申请，提交相关材料。\n\n"}
//        };
//
//        for (String[] scenario : scenarios) {
//            String scenarioName = scenario[0];
//            String query = scenario[1];
//            String context = scenario[2];
//
//            String prompt = promptOptimizer.buildQaPrompt(query, context);
//
//            logger.info("\n--- {} 场景 ---", scenarioName);
//            logger.info("用户问题: {}", query);
//            logger.info("提示词长度: {} 字符", prompt.length());
//
//            // 验证每个场景的提示词完整性
//            assertNotNull(prompt, scenarioName + "场景提示词不应为 null");
//            assertTrue(prompt.contains("## 角色定义"), scenarioName + "场景应包含角色定义");
//            assertTrue(prompt.contains("## 输出要求"), scenarioName + "场景应包含输出要求");
//            assertTrue(prompt.contains("## 思考步骤"), scenarioName + "场景应包含思考步骤");
//            assertTrue(prompt.contains(query), scenarioName + "场景应包含用户问题");
//        }
//
//        logger.info("\n✓ 多种业务场景提示词生成测试通过");
//    }
//
//    @Test
//    @DisplayName("测试14：提示词优化效果验证 - 结构对比")
//    void testPromptOptimizationEffect() {
//        logger.info("\n========== 测试14：提示词优化效果验证 - 结构对比 ==========");
//
//        String userQuery = "外汇业务如何办理？";
//        String context = "[段落 1]\n外汇业务办理流程：携带身份证到银行网点办理。\n\n";
//
//        // 优化后的提示词
//        String optimizedPrompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("优化后的提示词:\n{}", optimizedPrompt);
//
//        // 验证优化效果
//        logger.info("\n--- 优化效果验证 ---");
//
//        // 1. 角色定义
//        boolean hasRole = optimizedPrompt.contains("你是中国银行交银外汇助手的专业顾问");
//        logger.info("✓ 角色定义: {}", hasRole ? "明确" : "缺失");
//        assertTrue(hasRole, "应明确角色定义");
//
//        // 2. 输出格式约束
//        boolean hasConstraints = optimizedPrompt.contains("回答长度控制在 200-500 字之间");
//        logger.info("✓ 输出格式约束: {}", hasConstraints ? "明确" : "缺失");
//        assertTrue(hasConstraints, "应明确输出格式约束");
//
//        // 3. 思维链引导
//        boolean hasChainOfThought = optimizedPrompt.contains("理解用户问题的核心意图");
//        logger.info("✓ 思维链引导: {}", hasChainOfThought ? "明确" : "缺失");
//        assertTrue(hasChainOfThought, "应包含思维链引导");
//
//        // 4. 结构完整性
//        boolean hasCompleteStructure =
//            optimizedPrompt.contains("## 角色定义") &&
//            optimizedPrompt.contains("## 参考资料") &&
//            optimizedPrompt.contains("## 用户问题") &&
//            optimizedPrompt.contains("## 输出要求") &&
//            optimizedPrompt.contains("## 思考步骤（内部推理，不要输出）") &&
//            optimizedPrompt.contains("## 回答");
//
//        logger.info("✓ 结构完整性: {}", hasCompleteStructure ? "完整" : "不完整");
//        assertTrue(hasCompleteStructure, "提示词结构应完整");
//
//        logger.info("\n✓ 提示词优化效果验证测试通过");
//    }
//}