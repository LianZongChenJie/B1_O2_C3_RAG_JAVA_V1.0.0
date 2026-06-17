package org.example.milvuschinabank.rag.service;

import org.example.milvuschinabank.rag.config.RagConfig;
import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 问答衔接服务
 * 负责收尾后处理，将用户问题+有序上下文送入主问答模型
 */
@Service
public class QaIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(QaIntegrationService.class);

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private PromptOptimizer promptOptimizer;

    /**
     * 执行完整问答流程
     * @param userQuery 用户问题
     * @param retrievedChunks ReAct 召回的有序切片列表
     * @return 问答模型响应
     */
    public String answerQuestion(String userQuery, List<DocumentChunk> retrievedChunks) {
        logger.info("开始问答衔接，召回切片数: {}", retrievedChunks.size());

        // 1. 合并相邻重复语句/冗余
        List<DocumentChunk> deduplicatedChunks = mergeAdjacentDuplicates(retrievedChunks);

        // 2. 剔除空文本与乱码
        List<DocumentChunk> validChunks = filterInvalidContent(deduplicatedChunks);

        // 3. 再次按 pos 排序（确保顺序）
        validChunks.sort((a, b) -> a.getPos().compareTo(b.getPos()));

        logger.info("后处理完成，有效切片数: {}", validChunks.size());

        // 4. 构建上下文文本
        String context = buildOrderedContext(validChunks);

        // 5. 优化提示词
        String optimizedPrompt = promptOptimizer.buildQaPrompt(userQuery, context);

        // 6. 调用问答模型
        String answer = llmClient.generateCompletion(optimizedPrompt);

        logger.info("问答完成");

        return answer;
    }

    /**
     * 合并相邻重复语句
     */
    private List<DocumentChunk> mergeAdjacentDuplicates(List<DocumentChunk> chunks) {
        if (chunks.size() <= 1) {
            return chunks;
        }

        List<DocumentChunk> merged = new ArrayList<>();
        DocumentChunk current = chunks.get(0);

        for (int i = 1; i < chunks.size(); i++) {
            DocumentChunk next = chunks.get(i);

            // 检查相邻切片是否有重复内容
            if (hasOverlap(current.getContent(), next.getContent())) {
                // 合并重复部分
                String mergedContent = mergeOverlapContent(current.getContent(), next.getContent());
                current.setContent(mergedContent);

                // 更新 next_seg_id
                current.setNextSegId(next.getNextSegId());
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    /**
     * 检查是否有重叠
     */
    private boolean hasOverlap(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return false;
        }

        // 检查尾部/头部重叠（至少 10 个字符）
        int minOverlap = 10;
        int maxCheck = Math.min(50, Math.min(content1.length(), content2.length()));

        for (int len = maxCheck; len >= minOverlap; len--) {
            if (content1.length() >= len && content2.length() >= len) {
                String tail = content1.substring(content1.length() - len);
                String head = content2.substring(0, len);
                if (tail.equals(head)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 合并重叠内容
     */
    private String mergeOverlapContent(String content1, String content2) {
        int minOverlap = 10;
        int maxCheck = Math.min(50, Math.min(content1.length(), content2.length()));

        for (int len = maxCheck; len >= minOverlap; len--) {
            if (content1.length() >= len && content2.length() >= len) {
                String tail = content1.substring(content1.length() - len);
                String head = content2.substring(0, len);
                if (tail.equals(head)) {
                    return content1 + content2.substring(len);
                }
            }
        }

        return content1 + "\n" + content2;
    }

    /**
     * 过滤无效内容（空文本、乱码）
     */
    private List<DocumentChunk> filterInvalidContent(List<DocumentChunk> chunks) {
        return chunks.stream()
                .filter(chunk -> chunk.getContent() != null && !chunk.getContent().trim().isEmpty())
                .filter(this::isNotGarbled)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否为乱码
     */
    private boolean isNotGarbled(DocumentChunk chunk) {
        String content = chunk.getContent();

        if (content == null || content.isEmpty()) {
            return false;
        }

        // 检查中文字符比例（至少 30%）
        long chineseCount = content.chars()
                .filter(c -> c >= 0x4E00 && c <= 0x9FFF)
                .count();

        double chineseRatio = (double) chineseCount / content.length();

        // 检查可打印字符比例（至少 80%）
        long printableCount = content.chars()
                .filter(c -> c >= 32 && c <= 126 || c >= 0x4E00)
                .count();

        double printableRatio = (double) printableCount / content.length();

        return chineseRatio >= 0.3 && printableRatio >= 0.8;
    }

    /**
     * 构建有序上下文
     */
    private String buildOrderedContext(List<DocumentChunk> chunks) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);

            context.append("【段落 ").append(chunk.getPos() + 1).append("】\n");

            if (chunk.getTags() != null && !chunk.getTags().isEmpty()) {
                context.append("标签: ").append(String.join(", ", chunk.getTags())).append("\n");
            }

            context.append(chunk.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }
}