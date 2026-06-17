package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.example.milvuschinabank.rag.model.ReActState;
import org.example.milvuschinabank.rag.model.RetrievalResult;
import org.example.milvuschinabank.rag.repository.MilvusChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ReAct 多轮召回引擎
 * 实现推理判定→决策动作→执行召回→合并重排的单轮循环
 */
@Service
public class ReActRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(ReActRetrievalService.class);

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private MilvusChunkRepository chunkRepository;

    @Autowired
    private ContextCompleter contextCompleter;

    @Autowired
    private QualityEvaluator qualityEvaluator;

    /**
     * 执行 ReAct 多轮召回
     * @param userQuery 用户查询
     * @param queryVector 查询向量
     * @return 最终召回的有序切片列表
     */
    public List<DocumentChunk> executeReActRetrieval(String userQuery, List<Float> queryVector) {
        logger.info("开始 ReAct 多轮召回，查询: {}", userQuery);

        // 初始化 ReAct 状态
        ReActState state = new ReActState(
                ragConfig.getMaxRound(),
                ragConfig.getMaxContextLength(),
                ragConfig.getMaxAddSegPerRound()
        );

        state.setTotalSegIds(new ArrayList<>());
        state.setRoundResults(new ArrayList<>());

        // 多轮循环
        while (!state.isFinished()) {
            state.nextRound();

            // 1. 终止条件检查
            ReActState.TerminationCheckResult terminationCheck = state.checkTermination();
            if (terminationCheck.shouldTerminate()) {
                logger.info("终止召回: {}", terminationCheck.getReason());
                state.finish(terminationCheck.getReason());
                break;
            }

            logger.info("========== 第 {} 轮召回 ==========", state.getCurrentRound());

            // 2. 推理判定：评估当前上下文质量
            boolean needMoreContext = false;
            if (state.getCurrentRound() == 1) {
                // 第一轮：执行初始向量召回
                needMoreContext = true;
            } else {
                // 后续轮次：LLM 评估召回质量
                needMoreContext = qualityEvaluator.evaluateQuality(
                        userQuery, state.getCurrentContext(), state.getCurrentRound());
            }

            if (!needMoreContext) {
                logger.info("召回质量已满足要求，终止召回");
                state.finish("质量达标");
                break;
            }

            // 3. 决策动作：生成补全查询（第二轮及以后）
            String refinedQuery = userQuery;
            if (state.getCurrentRound() > 1) {
                refinedQuery = contextCompleter.generateRefinedQuery(
                        userQuery, state.getCurrentContext(), state.getCurrentRound());
                logger.info("补全查询: {}", refinedQuery);
            }

            // 4. 执行召回
            List<RetrievalResult> roundResults = executeSingleRoundRetrieval(
                    refinedQuery, queryVector, state);

            if (roundResults.isEmpty()) {
                logger.info("本轮无新结果，终止召回");
                state.finish("无新结果");
                break;
            }

            state.getRoundResults().addAll(roundResults);

            // 5. 合并重排：全局去重 + pos 顺序规整
            List<DocumentChunk> mergedChunks = mergeAndRerank(state);

            // 6. 拼接上下文
            String context = buildContext(mergedChunks);
            state.setCurrentContext(context);
            state.setCurrentContextLength(context.length());

            logger.info("当前上下文长度: {}, 切片数: {}",
                    state.getCurrentContextLength(), mergedChunks.size());
        }

        // 7. 收尾后处理
        List<DocumentChunk> finalChunks = postProcess(state);

        logger.info("ReAct 召回完成，最终切片数: {}, 总轮次: {}",
                finalChunks.size(), state.getCurrentRound());

        return finalChunks;
    }

    /**
     * 执行单轮召回
     */
    private List<RetrievalResult> executeSingleRoundRetrieval(
            String query, List<Float> queryVector, ReActState state) {

        List<RetrievalResult> results = new ArrayList<>();

        // 1. 向量召回（第一轮）或补全查询召回（后续轮次）
        List<DocumentChunk> vectorResults = vectorSearch(queryVector, ragConfig.getVectorTopK());

        // 2. 过滤已召回的切片（全局去重）
        Set<String> existingSegIds = new HashSet<>(state.getTotalSegIds());
        List<DocumentChunk> newResults = vectorResults.stream()
                .filter(chunk -> !existingSegIds.contains(chunk.getSegId()))
                .limit(ragConfig.getMaxAddSegPerRound())
                .collect(Collectors.toList());

        // 3. 拉取相邻切片修复语序截断
        List<DocumentChunk> adjacentChunks = fetchAdjacentChunks(newResults);

        // 4. 合并本轮结果
        Set<String> addedSegIds = new HashSet<>();
        for (DocumentChunk chunk : newResults) {
            results.add(new RetrievalResult(
                    chunk.getSegId(), 1.0, state.getCurrentRound(), "vector"));
            addedSegIds.add(chunk.getSegId());
        }

        for (DocumentChunk chunk : adjacentChunks) {
            if (!existingSegIds.contains(chunk.getSegId()) && !addedSegIds.contains(chunk.getSegId())) {
                results.add(new RetrievalResult(
                        chunk.getSegId(), 0.8, state.getCurrentRound(), "adjacent"));
                addedSegIds.add(chunk.getSegId());
            }
        }

        // 5. 更新全局去重集合
        state.getTotalSegIds().addAll(addedSegIds);

        return results;
    }

    /**
     * 向量检索
     */
    private List<DocumentChunk> vectorSearch(List<Float> queryVector, int topK) {
        // TODO: 实现 Milvus 向量检索
        // 这里需要根据实际的 Milvus SDK 版本实现
        logger.info("执行向量检索，TopK: {}", topK);
        return new ArrayList<>();
    }

    /**
     * 拉取相邻切片（根据 pre/next 关系）
     */
    private List<DocumentChunk> fetchAdjacentChunks(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> segIds = chunks.stream()
                .map(DocumentChunk::getSegId)
                .collect(Collectors.toList());

        return chunkRepository.queryAdjacentChunks(segIds);
    }

    /**
     * 合并重排：全局去重 + pos 顺序规整
     */
    private List<DocumentChunk> mergeAndRerank(ReActState state) {
        // 1. 根据 totalSegIds 查询所有切片
        List<DocumentChunk> allChunks = chunkRepository.queryBySegIds(state.getTotalSegIds());

        // 2. 按 pos 升序强制顺序规整
        allChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));

        return allChunks;
    }

    /**
     * 构建上下文文本
     */
    private String buildContext(List<DocumentChunk> chunks) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            context.append("[段落 ").append(chunk.getPos() + 1).append("]\n");
            context.append(chunk.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    /**
     * 收尾后处理
     */
    private List<DocumentChunk> postProcess(ReActState state) {
        // 1. 查询最终全集
        List<DocumentChunk> finalChunks = chunkRepository.queryBySegIds(state.getTotalSegIds());

        // 2. 按 pos 升序排序
        finalChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));

        // 3. 合并相邻重复语句/冗余
        finalChunks = mergeDuplicateContent(finalChunks);

        // 4. 剔除空文本与乱码
        finalChunks = finalChunks.stream()
                .filter(chunk -> isValidContent(chunk.getContent()))
                .collect(Collectors.toList());

        // 5. 再次按 pos 排序（确保顺序）
        finalChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));

        return finalChunks;
    }

    /**
     * 合并相邻重复内容
     */
    private List<DocumentChunk> mergeDuplicateContent(List<DocumentChunk> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<DocumentChunk> merged = new ArrayList<>();
        DocumentChunk previous = chunks.get(0);

        for (int i = 1; i < chunks.size(); i++) {
            DocumentChunk current = chunks.get(i);

            // 检查是否重复（简单实现：检查尾部/头部重叠）
            if (isDuplicateOrOverlap(previous.getContent(), current.getContent())) {
                // 合并内容
                String mergedContent = mergeOverlap(previous.getContent(), current.getContent());
                previous.setContent(mergedContent);
            } else {
                merged.add(previous);
                previous = current;
            }
        }

        merged.add(previous);
        return merged;
    }

    /**
     * 检查内容是否重复或重叠
     */
    private boolean isDuplicateOrOverlap(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return false;
        }

        // 检查完全重复
        if (content1.equals(content2)) {
            return true;
        }

        // 检查尾部/头部重叠（重叠超过 20 字符）
        int overlapLength = 20;
        if (content1.length() >= overlapLength && content2.length() >= overlapLength) {
            String tail = content1.substring(content1.length() - overlapLength);
            String head = content2.substring(0, overlapLength);
            return tail.equals(head);
        }

        return false;
    }

    /**
     * 合并重叠内容
     */
    private String mergeOverlap(String content1, String content2) {
        int overlapLength = 20;

        if (content1.length() >= overlapLength && content2.length() >= overlapLength) {
            String tail = content1.substring(content1.length() - overlapLength);
            String head = content2.substring(0, overlapLength);

            if (tail.equals(head)) {
                return content1 + content2.substring(overlapLength);
            }
        }

        return content1 + "\n" + content2;
    }

    /**
     * 验证内容是否有效（非空、非乱码）
     */
    private boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        // 检查是否包含足够的中文字符（排除乱码）
        long chineseCharCount = content.chars()
                .filter(c -> c >= 0x4E00 && c <= 0x9FFF)
                .count();

        return chineseCharCount > content.length() * 0.3; // 至少 30% 是中文字符
    }
}