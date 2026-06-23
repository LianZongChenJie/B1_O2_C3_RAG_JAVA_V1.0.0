package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.model.RetrievalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG 通用模块封装
 * 提供统一的配置模板，支持迁移到新对话场景
 */
@Service
public class RagPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(RagPipelineService.class);

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private QueryExpansionService queryExpansionService;

    @Autowired
    private ReActRetrievalService reActRetrievalService;

    @Autowired
    private QaIntegrationService qaIntegrationService;

    @Autowired
    private PromptOptimizer promptOptimizer;

    @Autowired
    private ABTestService abTestService;

    /**
     * 场景配置缓存（支持多场景）
     */
    private final Map<String, SceneConfig> sceneConfigs = new ConcurrentHashMap<>();

    /**
     * 初始化默认场景配置
     */
    public void initDefaultScene() {
        SceneConfig defaultScene = new SceneConfig();
        defaultScene.setSceneName("default");
        defaultScene.setEnableQueryExpansion(true);
        defaultScene.setEnableReAct(true);
        defaultScene.setEnableRerank(true);
        defaultScene.setEnableABTest(false);
        defaultScene.setMaxRound(5);
        defaultScene.setMaxAddSegPerRound(3);
        defaultScene.setMaxContextLength(8000);

        sceneConfigs.put("default", defaultScene);
    }

    /**
     * 注册新场景
     */
    public void registerScene(SceneConfig sceneConfig) {
        sceneConfigs.put(sceneConfig.getSceneName(), sceneConfig);
        logger.info("注册新场景: {}", sceneConfig.getSceneName());
    }

    /**
     * 执行完整的 RAG 问答流程
     * @param sceneName 场景名称
     * @param userQuery 用户问题
     * @param queryVector 查询向量
     * @return 问答响应
     */
    public String executeRagPipeline(String sceneName, String userQuery, List<Float> queryVector) {
        SceneConfig sceneConfig = sceneConfigs.getOrDefault(sceneName, sceneConfigs.get("default"));

        logger.info("执行 RAG 流程，场景: {}, 查询: {}", sceneName, userQuery);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询扩写
            String expandedQuery = userQuery;
            if (sceneConfig.isEnableQueryExpansion()) {
                List<String> expandedQueries = queryExpansionService.expandQuery(userQuery);
                expandedQuery = expandedQueries.get(0); // 使用第一个扩写查询
                logger.info("查询扩写: {} -> {}", userQuery, expandedQuery);
            }

            // 2. ReAct 多轮召回
            List<DocumentChunk> retrievedChunks;
            if (sceneConfig.isEnableReAct()) {
                retrievedChunks = reActRetrievalService.executeReActRetrieval(
                        expandedQuery, queryVector);
            } else {
                // 简单向量检索（不使用 ReAct 多轮循环，仅单轮召回）
                retrievedChunks = reActRetrievalService.executeSimpleRetrieval(
                        expandedQuery, queryVector);
            }

            logger.info("召回完成，切片数: {}", retrievedChunks.size());

            // 3. 问答衔接
            String answer = qaIntegrationService.answerQuestion(expandedQuery, retrievedChunks);

            long responseTime = System.currentTimeMillis() - startTime;

            // 4. A/B 测试记录
            if (sceneConfig.isEnableABTest()) {
                recordABTest(sceneName, userQuery, retrievedChunks, answer, responseTime);
            }

            return answer;

        } catch (Exception e) {
            logger.error("RAG 流程执行失败", e);
            return "抱歉，系统暂时无法处理您的请求，请稍后重试。";
        }
    }

    /**
     * 记录 A/B 测试数据
     */
    private void recordABTest(String sceneName, String userQuery,
                              List<DocumentChunk> chunks, String answer, long responseTime) {
        // TODO: 实现 A/B 测试记录逻辑
        logger.info("记录 A/B 测试数据: 场景={}, 查询={}", sceneName, userQuery);
    }

    /**
     * 场景配置
     */
    public static class SceneConfig {
        private String sceneName;
        private boolean enableQueryExpansion;
        private boolean enableReAct;
        private boolean enableRerank;
        private boolean enableABTest;
        private int maxRound;
        private int maxAddSegPerRound;
        private int maxContextLength;

        // Getters and Setters

        public String getSceneName() {
            return sceneName;
        }

        public void setSceneName(String sceneName) {
            this.sceneName = sceneName;
        }

        public boolean isEnableQueryExpansion() {
            return enableQueryExpansion;
        }

        public void setEnableQueryExpansion(boolean enableQueryExpansion) {
            this.enableQueryExpansion = enableQueryExpansion;
        }

        public boolean isEnableReAct() {
            return enableReAct;
        }

        public void setEnableReAct(boolean enableReAct) {
            this.enableReAct = enableReAct;
        }

        public boolean isEnableRerank() {
            return enableRerank;
        }

        public void setEnableRerank(boolean enableRerank) {
            this.enableRerank = enableRerank;
        }

        public boolean isEnableABTest() {
            return enableABTest;
        }

        public void setEnableABTest(boolean enableABTest) {
            this.enableABTest = enableABTest;
        }

        public int getMaxRound() {
            return maxRound;
        }

        public void setMaxRound(int maxRound) {
            this.maxRound = maxRound;
        }

        public int getMaxAddSegPerRound() {
            return maxAddSegPerRound;
        }

        public void setMaxAddSegPerRound(int maxAddSegPerRound) {
            this.maxAddSegPerRound = maxAddSegPerRound;
        }

        public int getMaxContextLength() {
            return maxContextLength;
        }

        public void setMaxContextLength(int maxContextLength) {
            this.maxContextLength = maxContextLength;
        }
    }
}