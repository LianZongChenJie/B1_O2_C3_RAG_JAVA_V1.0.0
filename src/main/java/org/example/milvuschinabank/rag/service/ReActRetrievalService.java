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
            // 补全查询用于指导检索策略和后续处理，但向量检索仍使用原始 queryVector
            // 原因：原始 queryVector 保持了用户查询的原始语义，避免补全查询引入的语义漂移
            String refinedQuery = userQuery;
            if (state.getCurrentRound() > 1) {
                refinedQuery = contextCompleter.generateRefinedQuery(
                        userQuery, state.getCurrentContext(), state.getCurrentRound());
                logger.info("补全查询: {}", refinedQuery);
                // TODO: 当接入 Embedding 服务后，可基于 refinedQuery 生成新的 queryVector
                // 用于更精确的向量检索。当前使用原始 queryVector 保证语义一致性。
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
        logger.info("向量召回返回 {} 个切片", vectorResults.size());

        // 2. 过滤已召回的切片（全局去重）
        Set<String> existingSegIds = new HashSet<>(state.getTotalSegIds());
        List<DocumentChunk> newResults = vectorResults.stream()
                .filter(chunk -> chunk != null && chunk.getSegId() != null)
                .filter(chunk -> !existingSegIds.contains(chunk.getSegId()))
                .limit(ragConfig.getMaxAddSegPerRound())
                .collect(Collectors.toList());
        logger.info("去重后新增向量召回切片 {} 个", newResults.size());

        // 3. 拉取相邻切片修复语序截断
        List<DocumentChunk> adjacentChunks = fetchAdjacentChunks(newResults);
        logger.info("拉取相邻切片 {} 个", adjacentChunks.size());

        // 4. 合并本轮结果（向量召回 + 相邻切片）
        Set<String> addedSegIds = new HashSet<>();

        // 4.1 添加向量召回结果
        for (DocumentChunk chunk : newResults) {
            results.add(new RetrievalResult(
                    chunk.getSegId(), 1.0, state.getCurrentRound(), "vector"));
            addedSegIds.add(chunk.getSegId());
        }

        // 4.2 添加相邻切片（去重）
        int adjacentAddedCount = 0;
        for (DocumentChunk chunk : adjacentChunks) {
            if (chunk != null && chunk.getSegId() != null
                    && !existingSegIds.contains(chunk.getSegId())
                    && !addedSegIds.contains(chunk.getSegId())) {
                results.add(new RetrievalResult(
                        chunk.getSegId(), 0.8, state.getCurrentRound(), "adjacent"));
                addedSegIds.add(chunk.getSegId());
                adjacentAddedCount++;
            }
        }
        logger.info("新增相邻切片 {} 个（去重后）", adjacentAddedCount);

        // 5. 更新全局去重集合
        state.getTotalSegIds().addAll(addedSegIds);
        logger.info("本轮共新增 {} 个切片ID到全局集合，当前总计 {} 个",
                addedSegIds.size(), state.getTotalSegIds().size());

        return results;
    }

    /**
     * 向量检索
     * 调用 MilvusChunkRepository 执行真正的向量检索
     */
    private List<DocumentChunk> vectorSearch(List<Float> queryVector, int topK) {
        logger.info("执行向量检索，TopK: {}", topK);

        if (queryVector == null || queryVector.isEmpty()) {
            logger.warn("查询向量为空，返回空结果");
            return new ArrayList<>();
        }

        try {
            // 调用 MilvusChunkRepository 进行向量检索
            List<MilvusChunkRepository.SearchResult> searchResults =
                    chunkRepository.searchByVector(queryVector, topK, null);

            // 将 SearchResult 转换为 DocumentChunk
            List<DocumentChunk> chunks = searchResults.stream()
                    .map(MilvusChunkRepository.SearchResult::getChunk)
                    .filter(chunk -> chunk != null && chunk.getSegId() != null)
                    .collect(Collectors.toList());

            logger.info("向量检索完成，返回 {} 个切片", chunks.size());
            return chunks;

        } catch (Exception e) {
            logger.error("向量检索失败: {}", e.getMessage(), e);
            // 降级返回空列表
            return new ArrayList<>();
        }
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
        List<String> segIds = state.getTotalSegIds();
        if (segIds == null || segIds.isEmpty()) {
            logger.warn("全局切片ID集合为空，返回空列表");
            return new ArrayList<>();
        }

        List<DocumentChunk> allChunks = chunkRepository.queryBySegIds(segIds);
        if (allChunks == null || allChunks.isEmpty()) {
            logger.warn("查询切片返回空，返回空列表");
            return new ArrayList<>();
        }

        // 2. 按 pos 升序强制顺序规整
        allChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));

        return allChunks;
    }

    /**
     * 构建上下文文本
     */
    private String buildContext(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();

        // 使用连续序号而非 pos+1，避免编号跳号
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            context.append("[段落 ").append(i + 1).append("]\n");
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
        List<String> segIds = state.getTotalSegIds();
        if (segIds == null || segIds.isEmpty()) {
            logger.warn("全局切片ID集合为空，返回空列表");
            return new ArrayList<>();
        }

        List<DocumentChunk> finalChunks = chunkRepository.queryBySegIds(segIds);
        if (finalChunks == null || finalChunks.isEmpty()) {
            logger.warn("查询最终切片返回空，返回空列表");
            return new ArrayList<>();
        }

        // 2. 按 pos 升序排序
        finalChunks.sort(Comparator.comparingInt(DocumentChunk::getPos));

        // 3. 合并相邻重复语句/冗余
        finalChunks = mergeDuplicateContent(finalChunks);

        // 4. 剔除空文本与乱码
        finalChunks = finalChunks.stream()
                .filter(chunk -> chunk != null && isValidContent(chunk.getContent()))
                .collect(Collectors.toList());

        // 注：合并重复内容后 pos 顺序已保持不变，无需再次排序

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

        // 检查尾部/头部重叠（使用配置化的重叠阈值）
        int overlapLength = ragConfig.getOverlapDetectionThreshold();
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
        int overlapLength = ragConfig.getOverlapDetectionThreshold();

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