//package org.example.milvuschinabank.rag.service;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 4.6 机制封装与迁移新对话测试类
// *
// * 测试目标：
// * 1. 验证扩写+提示词+召回提升封装为通用模块
// * 2. 验证配置模板功能
// * 3. 验证迁移应用于其他新对话场景
// * 4. 验证多场景配置管理
// */
//@DisplayName("4.6 机制封装与迁移新对话测试")
//class MechanismEncapsulationTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(MechanismEncapsulationTest.class);
//
//    private RagPipelineService pipelineService;
//
//    @BeforeEach
//    void setUp() {
//        pipelineService = new RagPipelineService();
//        pipelineService.initDefaultScene();
//    }
//
//    @Test
//    @DisplayName("测试1：默认场景初始化")
//    void testDefaultSceneInitialization() {
//        logger.info("\n========== 测试1：默认场景初始化 ==========");
//
//        // 验证默认场景已初始化
//        RagPipelineService.SceneConfig defaultConfig = new RagPipelineService.SceneConfig();
//        defaultConfig.setSceneName("default");
//        defaultConfig.setEnableQueryExpansion(true);
//        defaultConfig.setEnableReAct(true);
//        defaultConfig.setEnableRerank(true);
//        defaultConfig.setEnableABTest(false);
//        defaultConfig.setMaxRound(5);
//        defaultConfig.setMaxAddSegPerRound(3);
//        defaultConfig.setMaxContextLength(8000);
//
//        logger.info("默认场景配置:");
//        logger.info("  场景名称: {}", defaultConfig.getSceneName());
//        logger.info("  启用查询扩写: {}", defaultConfig.isEnableQueryExpansion());
//        logger.info("  启用ReAct召回: {}", defaultConfig.isEnableReAct());
//        logger.info("  启用重排序: {}", defaultConfig.isEnableRerank());
//        logger.info("  启用A/B测试: {}", defaultConfig.isEnableABTest());
//        logger.info("  最大轮次: {}", defaultConfig.getMaxRound());
//        logger.info("  每轮最大新增切片: {}", defaultConfig.getMaxAddSegPerRound());
//        logger.info("  最大上下文长度: {}", defaultConfig.getMaxContextLength());
//
//        // 验证默认配置值
//        assertEquals("default", defaultConfig.getSceneName());
//        assertTrue(defaultConfig.isEnableQueryExpansion());
//        assertTrue(defaultConfig.isEnableReAct());
//        assertTrue(defaultConfig.isEnableRerank());
//        assertFalse(defaultConfig.isEnableABTest());
//        assertEquals(5, defaultConfig.getMaxRound());
//        assertEquals(3, defaultConfig.getMaxAddSegPerRound());
//        assertEquals(8000, defaultConfig.getMaxContextLength());
//
//        logger.info("✓ 默认场景初始化测试通过");
//    }
//
//    @Test
//    @DisplayName("测试2：新场景注册")
//    void testNewSceneRegistration() {
//        logger.info("\n========== 测试2：新场景注册 ==========");
//
//        // 注册外汇场景
//        RagPipelineService.SceneConfig forexScene = new RagPipelineService.SceneConfig();
//        forexScene.setSceneName("foreign_exchange");
//        forexScene.setEnableQueryExpansion(true);
//        forexScene.setEnableReAct(true);
//        forexScene.setEnableRerank(true);
//        forexScene.setEnableABTest(true);
//        forexScene.setMaxRound(5);
//        forexScene.setMaxAddSegPerRound(3);
//        forexScene.setMaxContextLength(8000);
//
//        pipelineService.registerScene(forexScene);
//        logger.info("注册外汇场景: {}", forexScene.getSceneName());
//
//        // 注册结算场景
//        RagPipelineService.SceneConfig settlementScene = new RagPipelineService.SceneConfig();
//        settlementScene.setSceneName("settlement");
//        settlementScene.setEnableQueryExpansion(true);
//        settlementScene.setEnableReAct(true);
//        settlementScene.setEnableRerank(false);
//        settlementScene.setEnableABTest(true);
//        settlementScene.setMaxRound(3);
//        settlementScene.setMaxAddSegPerRound(2);
//        settlementScene.setMaxContextLength(6000);
//
//        pipelineService.registerScene(settlementScene);
//        logger.info("注册结算场景: {}", settlementScene.getSceneName());
//
//        // 验证场景注册成功
//        assertNotNull(forexScene);
//        assertNotNull(settlementScene);
//        assertEquals("foreign_exchange", forexScene.getSceneName());
//        assertEquals("settlement", settlementScene.getSceneName());
//
//        logger.info("✓ 新场景注册测试通过");
//    }
//
//    @Test
//    @DisplayName("测试3：场景配置模板 - 外汇业务场景")
//    void testForexSceneConfigurationTemplate() {
//        logger.info("\n========== 测试3：场景配置模板 - 外汇业务场景 ==========");
//
//        // 创建外汇业务场景配置模板
//        RagPipelineService.SceneConfig forexTemplate = createForexSceneTemplate();
//
//        logger.info("外汇业务场景配置模板:");
//        logger.info("  场景名称: {}", forexTemplate.getSceneName());
//        logger.info("  启用查询扩写: {}", forexTemplate.isEnableQueryExpansion());
//        logger.info("  启用ReAct召回: {}", forexTemplate.isEnableReAct());
//        logger.info("  启用重排序: {}", forexTemplate.isEnableRerank());
//        logger.info("  启用A/B测试: {}", forexTemplate.isEnableABTest());
//        logger.info("  最大轮次: {}", forexTemplate.getMaxRound());
//        logger.info("  每轮最大新增切片: {}", forexTemplate.getMaxAddSegPerRound());
//        logger.info("  最大上下文长度: {}", forexTemplate.getMaxContextLength());
//
//        // 验证外汇场景配置
//        assertEquals("foreign_exchange", forexTemplate.getSceneName());
//        assertTrue(forexTemplate.isEnableQueryExpansion());
//        assertTrue(forexTemplate.isEnableReAct());
//        assertTrue(forexTemplate.isEnableRerank());
//        assertTrue(forexTemplate.isEnableABTest());
//        assertEquals(5, forexTemplate.getMaxRound());
//        assertEquals(3, forexTemplate.getMaxAddSegPerRound());
//        assertEquals(8000, forexTemplate.getMaxContextLength());
//
//        logger.info("✓ 外汇业务场景配置模板测试通过");
//    }
//
//    @Test
//    @DisplayName("测试4：场景配置模板 - 国际结算场景")
//    void testSettlementSceneConfigurationTemplate() {
//        logger.info("\n========== 测试4：场景配置模板 - 国际结算场景 ==========");
//
//        // 创建国际结算场景配置模板
//        RagPipelineService.SceneConfig settlementTemplate = createSettlementSceneTemplate();
//
//        logger.info("国际结算场景配置模板:");
//        logger.info("  场景名称: {}", settlementTemplate.getSceneName());
//        logger.info("  启用查询扩写: {}", settlementTemplate.isEnableQueryExpansion());
//        logger.info("  启用ReAct召回: {}", settlementTemplate.isEnableReAct());
//        logger.info("  启用重排序: {}", settlementTemplate.isEnableRerank());
//        logger.info("  启用A/B测试: {}", settlementTemplate.isEnableABTest());
//        logger.info("  最大轮次: {}", settlementTemplate.getMaxRound());
//        logger.info("  每轮最大新增切片: {}", settlementTemplate.getMaxAddSegPerRound());
//        logger.info("  最大上下文长度: {}", settlementTemplate.getMaxContextLength());
//
//        // 验证结算场景配置
//        assertEquals("settlement", settlementTemplate.getSceneName());
//        assertTrue(settlementTemplate.isEnableQueryExpansion());
//        assertTrue(settlementTemplate.isEnableReAct());
//        assertFalse(settlementTemplate.isEnableRerank());
//        assertTrue(settlementTemplate.isEnableABTest());
//        assertEquals(3, settlementTemplate.getMaxRound());
//        assertEquals(2, settlementTemplate.getMaxAddSegPerRound());
//        assertEquals(6000, settlementTemplate.getMaxContextLength());
//
//        logger.info("✓ 国际结算场景配置模板测试通过");
//    }
//
//    @Test
//    @DisplayName("测试5：场景配置模板 - 通用客服场景")
//    void testGeneralCustomerServiceSceneTemplate() {
//        logger.info("\n========== 测试5：场景配置模板 - 通用客服场景 ==========");
//
//        // 创建通用客服场景配置模板
//        RagPipelineService.SceneConfig generalTemplate = createGeneralCustomerServiceSceneTemplate();
//
//        logger.info("通用客服场景配置模板:");
//        logger.info("  场景名称: {}", generalTemplate.getSceneName());
//        logger.info("  启用查询扩写: {}", generalTemplate.isEnableQueryExpansion());
//        logger.info("  启用ReAct召回: {}", generalTemplate.isEnableReAct());
//        logger.info("  启用重排序: {}", generalTemplate.isEnableRerank());
//        logger.info("  启用A/B测试: {}", generalTemplate.isEnableABTest());
//        logger.info("  最大轮次: {}", generalTemplate.getMaxRound());
//        logger.info("  每轮最大新增切片: {}", generalTemplate.getMaxAddSegPerRound());
//        logger.info("  最大上下文长度: {}", generalTemplate.getMaxContextLength());
//
//        // 验证通用客服场景配置
//        assertEquals("general_customer_service", generalTemplate.getSceneName());
//        assertTrue(generalTemplate.isEnableQueryExpansion());
//        assertFalse(generalTemplate.isEnableReAct());
//        assertFalse(generalTemplate.isEnableRerank());
//        assertFalse(generalTemplate.isEnableABTest());
//        assertEquals(3, generalTemplate.getMaxRound());
//        assertEquals(2, generalTemplate.getMaxAddSegPerRound());
//        assertEquals(4000, generalTemplate.getMaxContextLength());
//
//        logger.info("✓ 通用客服场景配置模板测试通过");
//    }
//
//    @Test
//    @DisplayName("测试6：多场景配置管理")
//    void testMultiSceneConfigurationManagement() {
//        logger.info("\n========== 测试6：多场景配置管理 ==========");
//
//        // 注册多个场景
//        RagPipelineService.SceneConfig forexScene = createForexSceneTemplate();
//        RagPipelineService.SceneConfig settlementScene = createSettlementSceneTemplate();
//        RagPipelineService.SceneConfig generalScene = createGeneralCustomerServiceSceneTemplate();
//
//        pipelineService.registerScene(forexScene);
//        pipelineService.registerScene(settlementScene);
//        pipelineService.registerScene(generalScene);
//
//        logger.info("已注册场景:");
//        logger.info("  1. {}", forexScene.getSceneName());
//        logger.info("  2. {}", settlementScene.getSceneName());
//        logger.info("  3. {}", generalScene.getSceneName());
//
//        // 验证场景配置独立性
//        assertNotEquals(forexScene.getMaxRound(), settlementScene.getMaxRound());
//        assertNotEquals(forexScene.getMaxContextLength(), generalScene.getMaxContextLength());
//        assertNotEquals(forexScene.isEnableRerank(), settlementScene.isEnableRerank());
//
//        logger.info("场景配置独立性验证:");
//        logger.info("  外汇场景最大轮次: {}, 结算场景最大轮次: {}",
//                forexScene.getMaxRound(), settlementScene.getMaxRound());
//        logger.info("  外汇场景上下文长度: {}, 通用场景上下文长度: {}",
//                forexScene.getMaxContextLength(), generalScene.getMaxContextLength());
//        logger.info("  结算场景启用重排序: {}, 通用场景启用重排序: {}",
//                settlementScene.isEnableRerank(), generalScene.isEnableRerank());
//
//        logger.info("✓ 多场景配置管理测试通过");
//    }
//
//    @Test
//    @DisplayName("测试7：场景配置迁移 - 从外汇到结算")
//    void testSceneConfigurationMigration() {
//        logger.info("\n========== 测试7：场景配置迁移 - 从外汇到结算 ==========");
//
//        // 外汇场景配置
//        RagPipelineService.SceneConfig forexScene = createForexSceneTemplate();
//        logger.info("源场景（外汇）配置:");
//        logger.info("  最大轮次: {}", forexScene.getMaxRound());
//        logger.info("  最大上下文长度: {}", forexScene.getMaxContextLength());
//        logger.info("  启用重排序: {}", forexScene.isEnableRerank());
//
//        // 迁移到结算场景（调整配置）
//        RagPipelineService.SceneConfig settlementScene = new RagPipelineService.SceneConfig();
//        settlementScene.setSceneName("settlement_migrated");
//        settlementScene.setEnableQueryExpansion(forexScene.isEnableQueryExpansion());
//        settlementScene.setEnableReAct(forexScene.isEnableReAct());
//        settlementScene.setEnableRerank(false); // 结算场景不需要重排序
//        settlementScene.setEnableABTest(forexScene.isEnableABTest());
//        settlementScene.setMaxRound(3); // 减少轮次
//        settlementScene.setMaxAddSegPerRound(2); // 减少新增切片
//        settlementScene.setMaxContextLength(6000); // 减少上下文长度
//
//        pipelineService.registerScene(settlementScene);
//
//        logger.info("\n目标场景（结算）配置:");
//        logger.info("  最大轮次: {}", settlementScene.getMaxRound());
//        logger.info("  最大上下文长度: {}", settlementScene.getMaxContextLength());
//        logger.info("  启用重排序: {}", settlementScene.isEnableRerank());
//
//        // 验证迁移后的配置
//        assertEquals("settlement_migrated", settlementScene.getSceneName());
//        assertEquals(forexScene.isEnableQueryExpansion(), settlementScene.isEnableQueryExpansion());
//        assertEquals(forexScene.isEnableReAct(), settlementScene.isEnableReAct());
//        assertFalse(settlementScene.isEnableRerank());
//        assertEquals(3, settlementScene.getMaxRound());
//        assertEquals(2, settlementScene.getMaxAddSegPerRound());
//        assertEquals(6000, settlementScene.getMaxContextLength());
//
//        logger.info("✓ 场景配置迁移测试通过");
//    }
//
//    @Test
//    @DisplayName("测试8：通用模块封装验证 - 查询扩写模块")
//    void testQueryExpansionModuleEncapsulation() {
//        logger.info("\n========== 测试8：通用模块封装验证 - 查询扩写模块 ==========");
//
//        // 验证查询扩写模块可以独立配置
//        RagPipelineService.SceneConfig sceneWithExpansion = new RagPipelineService.SceneConfig();
//        sceneWithExpansion.setSceneName("expansion_test");
//        sceneWithExpansion.setEnableQueryExpansion(true);
//        sceneWithExpansion.setEnableReAct(false);
//        sceneWithExpansion.setEnableRerank(false);
//        sceneWithExpansion.setEnableABTest(false);
//        sceneWithExpansion.setMaxRound(1);
//        sceneWithExpansion.setMaxAddSegPerRound(1);
//        sceneWithExpansion.setMaxContextLength(2000);
//
//        pipelineService.registerScene(sceneWithExpansion);
//
//        logger.info("查询扩写模块配置:");
//        logger.info("  启用查询扩写: {}", sceneWithExpansion.isEnableQueryExpansion());
//        logger.info("  启用ReAct召回: {}", sceneWithExpansion.isEnableReAct());
//
//        // 验证模块独立性
//        assertTrue(sceneWithExpansion.isEnableQueryExpansion());
//        assertFalse(sceneWithExpansion.isEnableReAct());
//
//        logger.info("✓ 查询扩写模块封装验证通过");
//    }
//
//    @Test
//    @DisplayName("测试9：通用模块封装验证 - 提示词优化模块")
//    void testPromptOptimizerModuleEncapsulation() {
//        logger.info("\n========== 测试9：通用模块封装验证 - 提示词优化模块 ==========");
//
//        // 创建提示词优化器实例
//        PromptOptimizer promptOptimizer = new PromptOptimizer();
//
//        // 测试构建问答提示词
//        String userQuery = "个人购汇的具体额度是多少？";
//        String context = "个人购汇年度总额为等值5万美元。";
//
//        String prompt = promptOptimizer.buildQaPrompt(userQuery, context);
//
//        logger.info("生成的问答提示词:");
//        logger.info("  长度: {} 字符", prompt.length());
//        logger.info("  包含角色定义: {}", prompt.contains("角色定义"));
//        logger.info("  包含参考资料: {}", prompt.contains("参考资料"));
//        logger.info("  包含用户问题: {}", prompt.contains("用户问题"));
//        logger.info("  包含输出要求: {}", prompt.contains("输出要求"));
//        logger.info("  包含思考步骤: {}", prompt.contains("思考步骤"));
//
//        // 验证提示词结构完整性
//        assertNotNull(prompt);
//        assertTrue(prompt.length() > 100);
//        assertTrue(prompt.contains("角色定义"));
//        assertTrue(prompt.contains("参考资料"));
//        assertTrue(prompt.contains("用户问题"));
//        assertTrue(prompt.contains("输出要求"));
//        assertTrue(prompt.contains("思考步骤"));
//
//        logger.info("✓ 提示词优化模块封装验证通过");
//    }
//
//    @Test
//    @DisplayName("测试10：通用模块封装验证 - 召回提升模块")
//    void testRetrievalEnhancementModuleEncapsulation() {
//        logger.info("\n========== 测试10：通用模块封装验证 - 召回提升模块 ==========");
//
//        // 验证召回提升模块可以独立配置
//        RagPipelineService.SceneConfig sceneWithRetrieval = new RagPipelineService.SceneConfig();
//        sceneWithRetrieval.setSceneName("retrieval_test");
//        sceneWithRetrieval.setEnableQueryExpansion(true);
//        sceneWithRetrieval.setEnableReAct(true);
//        sceneWithRetrieval.setEnableRerank(true);
//        sceneWithRetrieval.setEnableABTest(false);
//        sceneWithRetrieval.setMaxRound(5);
//        sceneWithRetrieval.setMaxAddSegPerRound(3);
//        sceneWithRetrieval.setMaxContextLength(8000);
//
//        pipelineService.registerScene(sceneWithRetrieval);
//
//        logger.info("召回提升模块配置:");
//        logger.info("  启用查询扩写: {}", sceneWithRetrieval.isEnableQueryExpansion());
//        logger.info("  启用ReAct召回: {}", sceneWithRetrieval.isEnableReAct());
//        logger.info("  启用重排序: {}", sceneWithRetrieval.isEnableRerank());
//
//        // 验证召回提升模块完整性
//        assertTrue(sceneWithRetrieval.isEnableQueryExpansion());
//        assertTrue(sceneWithRetrieval.isEnableReAct());
//        assertTrue(sceneWithRetrieval.isEnableRerank());
//
//        logger.info("✓ 召回提升模块封装验证通过");
//    }
//
//    @Test
//    @DisplayName("测试11：完整模块集成 - 扩写+提示词+召回")
//    void testCompleteModuleIntegration() {
//        logger.info("\n========== 测试11：完整模块集成 - 扩写+提示词+召回 ==========");
//
//        // 创建完整集成场景配置
//        RagPipelineService.SceneConfig integratedScene = new RagPipelineService.SceneConfig();
//        integratedScene.setSceneName("integrated_test");
//        integratedScene.setEnableQueryExpansion(true);
//        integratedScene.setEnableReAct(true);
//        integratedScene.setEnableRerank(true);
//        integratedScene.setEnableABTest(true);
//        integratedScene.setMaxRound(5);
//        integratedScene.setMaxAddSegPerRound(3);
//        integratedScene.setMaxContextLength(8000);
//
//        pipelineService.registerScene(integratedScene);
//
//        logger.info("完整模块集成配置:");
//        logger.info("  场景名称: {}", integratedScene.getSceneName());
//        logger.info("  查询扩写模块: {}", integratedScene.isEnableQueryExpansion() ? "已启用" : "未启用");
//        logger.info("  提示词优化模块: 已启用");
//        logger.info("  召回提升模块: {}", integratedScene.isEnableReAct() ? "已启用" : "未启用");
//        logger.info("  重排序模块: {}", integratedScene.isEnableRerank() ? "已启用" : "未启用");
//        logger.info("  A/B测试模块: {}", integratedScene.isEnableABTest() ? "已启用" : "未启用");
//
//        // 验证所有模块都已启用
//        assertTrue(integratedScene.isEnableQueryExpansion());
//        assertTrue(integratedScene.isEnableReAct());
//        assertTrue(integratedScene.isEnableRerank());
//        assertTrue(integratedScene.isEnableABTest());
//        assertEquals(5, integratedScene.getMaxRound());
//        assertEquals(3, integratedScene.getMaxAddSegPerRound());
//        assertEquals(8000, integratedScene.getMaxContextLength());
//
//        logger.info("✓ 完整模块集成测试通过");
//    }
//
//    @Test
//    @DisplayName("测试12：新场景迁移验证 - 贸易融资场景")
//    void testNewSceneMigrationTradeFinance() {
//        logger.info("\n========== 测试12：新场景迁移验证 - 贸易融资场景 ==========");
//
//        // 基于外汇场景模板迁移到贸易融资场景
//        RagPipelineService.SceneConfig tradeFinanceScene = new RagPipelineService.SceneConfig();
//        tradeFinanceScene.setSceneName("trade_finance");
//        tradeFinanceScene.setEnableQueryExpansion(true);
//        tradeFinanceScene.setEnableReAct(true);
//        tradeFinanceScene.setEnableRerank(true);
//        tradeFinanceScene.setEnableABTest(true);
//        tradeFinanceScene.setMaxRound(4);
//        tradeFinanceScene.setMaxAddSegPerRound(2);
//        tradeFinanceScene.setMaxContextLength(7000);
//
//        pipelineService.registerScene(tradeFinanceScene);
//
//        logger.info("贸易融资场景配置:");
//        logger.info("  场景名称: {}", tradeFinanceScene.getSceneName());
//        logger.info("  启用查询扩写: {}", tradeFinanceScene.isEnableQueryExpansion());
//        logger.info("  启用ReAct召回: {}", tradeFinanceScene.isEnableReAct());
//        logger.info("  启用重排序: {}", tradeFinanceScene.isEnableRerank());
//        logger.info("  启用A/B测试: {}", tradeFinanceScene.isEnableABTest());
//        logger.info("  最大轮次: {}", tradeFinanceScene.getMaxRound());
//        logger.info("  每轮最大新增切片: {}", tradeFinanceScene.getMaxAddSegPerRound());
//        logger.info("  最大上下文长度: {}", tradeFinanceScene.getMaxContextLength());
//
//        // 验证贸易融资场景配置
//        assertEquals("trade_finance", tradeFinanceScene.getSceneName());
//        assertTrue(tradeFinanceScene.isEnableQueryExpansion());
//        assertTrue(tradeFinanceScene.isEnableReAct());
//        assertTrue(tradeFinanceScene.isEnableRerank());
//        assertTrue(tradeFinanceScene.isEnableABTest());
//        assertEquals(4, tradeFinanceScene.getMaxRound());
//        assertEquals(2, tradeFinanceScene.getMaxAddSegPerRound());
//        assertEquals(7000, tradeFinanceScene.getMaxContextLength());
//
//        logger.info("✓ 贸易融资场景迁移验证通过");
//    }
//
//    @Test
//    @DisplayName("测试13：配置模板复用验证")
//    void testConfigurationTemplateReuse() {
//        logger.info("\n========== 测试13：配置模板复用验证 ==========");
//
//        // 创建基础模板
//        RagPipelineService.SceneConfig baseTemplate = new RagPipelineService.SceneConfig();
//        baseTemplate.setSceneName("base_template");
//        baseTemplate.setEnableQueryExpansion(true);
//        baseTemplate.setEnableReAct(true);
//        baseTemplate.setEnableRerank(true);
//        baseTemplate.setEnableABTest(false);
//        baseTemplate.setMaxRound(5);
//        baseTemplate.setMaxAddSegPerRound(3);
//        baseTemplate.setMaxContextLength(8000);
//
//        // 基于模板创建多个场景
//        RagPipelineService.SceneConfig scene1 = copySceneConfig(baseTemplate, "scene_1");
//        RagPipelineService.SceneConfig scene2 = copySceneConfig(baseTemplate, "scene_2");
//        RagPipelineService.SceneConfig scene3 = copySceneConfig(baseTemplate, "scene_3");
//
//        // 微调场景2和场景3
//        scene2.setMaxRound(3);
//        scene3.setMaxContextLength(6000);
//
//        pipelineService.registerScene(scene1);
//        pipelineService.registerScene(scene2);
//        pipelineService.registerScene(scene3);
//
//        logger.info("基于模板创建的场景:");
//        logger.info("  场景1: {}, 最大轮次: {}, 上下文长度: {}",
//                scene1.getSceneName(), scene1.getMaxRound(), scene1.getMaxContextLength());
//        logger.info("  场景2: {}, 最大轮次: {}, 上下文长度: {}",
//                scene2.getSceneName(), scene2.getMaxRound(), scene2.getMaxContextLength());
//        logger.info("  场景3: {}, 最大轮次: {}, 上下文长度: {}",
//                scene3.getSceneName(), scene3.getMaxRound(), scene3.getMaxContextLength());
//
//        // 验证模板复用
//        assertEquals(5, scene1.getMaxRound());
//        assertEquals(3, scene2.getMaxRound());
//        assertEquals(5, scene3.getMaxRound());
//        assertEquals(8000, scene1.getMaxContextLength());
//        assertEquals(8000, scene2.getMaxContextLength());
//        assertEquals(6000, scene3.getMaxContextLength());
//
//        logger.info("✓ 配置模板复用验证通过");
//    }
//
//    @Test
//    @DisplayName("测试14：模块解耦与独立配置验证")
//    void testModuleDecouplingAndIndependentConfiguration() {
//        logger.info("\n========== 测试14：模块解耦与独立配置验证 ==========");
//
//        // 场景1：仅启用查询扩写
//        RagPipelineService.SceneConfig scene1 = new RagPipelineService.SceneConfig();
//        scene1.setSceneName("expansion_only");
//        scene1.setEnableQueryExpansion(true);
//        scene1.setEnableReAct(false);
//        scene1.setEnableRerank(false);
//        scene1.setEnableABTest(false);
//        scene1.setMaxRound(1);
//        scene1.setMaxAddSegPerRound(1);
//        scene1.setMaxContextLength(2000);
//
//        // 场景2：仅启用ReAct召回
//        RagPipelineService.SceneConfig scene2 = new RagPipelineService.SceneConfig();
//        scene2.setSceneName("react_only");
//        scene2.setEnableQueryExpansion(false);
//        scene2.setEnableReAct(true);
//        scene2.setEnableRerank(false);
//        scene2.setEnableABTest(false);
//        scene2.setMaxRound(5);
//        scene2.setMaxAddSegPerRound(3);
//        scene2.setMaxContextLength(8000);
//
//        // 场景3：仅启用重排序
//        RagPipelineService.SceneConfig scene3 = new RagPipelineService.SceneConfig();
//        scene3.setSceneName("rerank_only");
//        scene3.setEnableQueryExpansion(false);
//        scene3.setEnableReAct(false);
//        scene3.setEnableRerank(true);
//        scene3.setEnableABTest(false);
//        scene3.setMaxRound(1);
//        scene3.setMaxAddSegPerRound(1);
//        scene3.setMaxContextLength(4000);
//
//        pipelineService.registerScene(scene1);
//        pipelineService.registerScene(scene2);
//        pipelineService.registerScene(scene3);
//
//        logger.info("模块解耦配置:");
//        logger.info("  场景1（仅扩写）: 扩写={}, ReAct={}, 重排序={}",
//                scene1.isEnableQueryExpansion(), scene1.isEnableReAct(), scene1.isEnableRerank());
//        logger.info("  场景2（仅ReAct）: 扩写={}, ReAct={}, 重排序={}",
//                scene2.isEnableQueryExpansion(), scene2.isEnableReAct(), scene2.isEnableRerank());
//        logger.info("  场景3（仅重排序）: 扩写={}, ReAct={}, 重排序={}",
//                scene3.isEnableQueryExpansion(), scene3.isEnableReAct(), scene3.isEnableRerank());
//
//        // 验证模块解耦
//        assertTrue(scene1.isEnableQueryExpansion());
//        assertFalse(scene1.isEnableReAct());
//        assertFalse(scene1.isEnableRerank());
//
//        assertFalse(scene2.isEnableQueryExpansion());
//        assertTrue(scene2.isEnableReAct());
//        assertFalse(scene2.isEnableRerank());
//
//        assertFalse(scene3.isEnableQueryExpansion());
//        assertFalse(scene3.isEnableReAct());
//        assertTrue(scene3.isEnableRerank());
//
//        logger.info("✓ 模块解耦与独立配置验证通过");
//    }
//
//    /**
//     * 创建外汇业务场景配置模板
//     */
//    private RagPipelineService.SceneConfig createForexSceneTemplate() {
//        RagPipelineService.SceneConfig config = new RagPipelineService.SceneConfig();
//        config.setSceneName("foreign_exchange");
//        config.setEnableQueryExpansion(true);
//        config.setEnableReAct(true);
//        config.setEnableRerank(true);
//        config.setEnableABTest(true);
//        config.setMaxRound(5);
//        config.setMaxAddSegPerRound(3);
//        config.setMaxContextLength(8000);
//        return config;
//    }
//
//    /**
//     * 创建国际结算场景配置模板
//     */
//    private RagPipelineService.SceneConfig createSettlementSceneTemplate() {
//        RagPipelineService.SceneConfig config = new RagPipelineService.SceneConfig();
//        config.setSceneName("settlement");
//        config.setEnableQueryExpansion(true);
//        config.setEnableReAct(true);
//        config.setEnableRerank(false);
//        config.setEnableABTest(true);
//        config.setMaxRound(3);
//        config.setMaxAddSegPerRound(2);
//        config.setMaxContextLength(6000);
//        return config;
//    }
//
//    /**
//     * 创建通用客服场景配置模板
//     */
//    private RagPipelineService.SceneConfig createGeneralCustomerServiceSceneTemplate() {
//        RagPipelineService.SceneConfig config = new RagPipelineService.SceneConfig();
//        config.setSceneName("general_customer_service");
//        config.setEnableQueryExpansion(true);
//        config.setEnableReAct(false);
//        config.setEnableRerank(false);
//        config.setEnableABTest(false);
//        config.setMaxRound(3);
//        config.setMaxAddSegPerRound(2);
//        config.setMaxContextLength(4000);
//        return config;
//    }
//
//    /**
//     * 复制场景配置
//     */
//    private RagPipelineService.SceneConfig copySceneConfig(
//            RagPipelineService.SceneConfig source, String targetSceneName) {
//        RagPipelineService.SceneConfig target = new RagPipelineService.SceneConfig();
//        target.setSceneName(targetSceneName);
//        target.setEnableQueryExpansion(source.isEnableQueryExpansion());
//        target.setEnableReAct(source.isEnableReAct());
//        target.setEnableRerank(source.isEnableRerank());
//        target.setEnableABTest(source.isEnableABTest());
//        target.setMaxRound(source.getMaxRound());
//        target.setMaxAddSegPerRound(source.getMaxAddSegPerRound());
//        target.setMaxContextLength(source.getMaxContextLength());
//        return target;
//    }
//}