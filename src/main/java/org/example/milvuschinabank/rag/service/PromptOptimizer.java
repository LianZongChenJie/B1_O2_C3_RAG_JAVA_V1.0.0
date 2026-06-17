package org.example.milvuschinabank.rag.service;

import org.springframework.stereotype.Service;

/**
 * 提示词优化器
 * 优化系统提示词结构：角色定义、输出格式约束、思维链引导
 */
@Service
public class PromptOptimizer {

    /**
     * 构建问答提示词
     * @param userQuery 用户问题
     * @param context 召回的上下文
     * @return 优化后的完整提示词
     */
    public String buildQaPrompt(String userQuery, String context) {
        return buildSystemPrompt() + "\n\n" +
               buildContextSection(context) + "\n\n" +
               buildQuestionSection(userQuery) + "\n\n" +
               buildOutputConstraints() + "\n\n" +
               buildChainOfThoughtGuide();
    }

    /**
     * 系统提示词 - 角色定义
     */
    private String buildSystemPrompt() {
        return "## 角色定义\n" +
               "你是中国银行交银外汇助手的专业顾问，具备以下能力：\n" +
               "1. 精通外汇业务、国际结算、贸易融资等银行业务\n" +
               "2. 能够准确理解用户问题并从提供的参考资料中找到答案\n" +
               "3. 回答专业、准确、简洁，符合银行业务规范\n" +
               "4. 如果参考资料中没有答案，明确告知用户并建议咨询银行工作人员";
    }

    /**
     * 上下文部分
     */
    private String buildContextSection(String context) {
        return "## 参考资料\n" +
               "以下是与用户问题相关的参考资料，请基于这些资料回答问题：\n\n" +
               context;
    }

    /**
     * 问题部分
     */
    private String buildQuestionSection(String userQuery) {
        return "## 用户问题\n" + userQuery;
    }

    /**
     * 输出格式约束
     */
    private String buildOutputConstraints() {
        return "## 输出要求\n" +
               "1. 直接回答问题，不要复述问题\n" +
               "2. 使用清晰的段落结构，必要时使用列表\n" +
               "3. 涉及数字、利率、汇率等关键信息时必须准确引用\n" +
               "4. 如果答案涉及多个方面，请分点说明\n" +
               "5. 回答长度控制在 200-500 字之间\n" +
               "6. 使用专业但易懂的语言，避免过度使用术语\n" +
               "7. 如果参考资料不足以回答问题，请明确说明";
    }

    /**
     * 思维链引导
     */
    private String buildChainOfThoughtGuide() {
        return "## 思考步骤（内部推理，不要输出）\n" +
               "1. 理解用户问题的核心意图和关键信息需求\n" +
               "2. 在参考资料中定位与问题相关的段落\n" +
               "3. 提取关键信息并验证准确性\n" +
               "4. 组织答案结构：核心答案 → 补充说明 → 注意事项\n" +
               "5. 检查答案是否完整回答了用户问题\n" +
               "6. 确保语言专业、准确、符合银行业务规范\n\n" +
               "## 回答\n" +
               "请基于以上思考步骤，给出专业、准确的回答：";
    }

    /**
     * 构建查询扩写提示词
     * @param originalQuery 原始查询
     * @return 扩写提示词
     */
    public String buildQueryExpansionPrompt(String originalQuery) {
        return "## 角色\n" +
               "你是专业的查询优化助手，擅长将用户的自然语言问题转化为更适合检索的查询语句。\n\n" +
               "## 任务\n" +
               "请将以下用户问题扩写为多个不同角度的查询语句，以提高检索召回率。\n\n" +
               "## 原始问题\n" +
               originalQuery + "\n\n" +
               "## 要求\n" +
               "1. 生成 3-5 个不同角度的查询语句\n" +
               "2. 包含同义词、近义词、相关业务术语\n" +
               "3. 保持简洁，每个查询不超过 30 字\n" +
               "4. 输出格式：每行一个查询，用 | 分隔\n\n" +
               "## 扩写结果\n";
    }

    /**
     * 构建重排序提示词
     * @param userQuery 用户查询
     * @param chunks 待重排序的切片
     * @return 重排序提示词
     */
    public String buildRerankPrompt(String userQuery, int chunkCount) {
        return "## 角色\n" +
               "你是专业的文档相关性评估助手。\n\n" +
               "## 任务\n" +
               "请评估以下 " + chunkCount + " 个文档片段与用户问题的相关性，并给出 0-100 的相关性分数。\n\n" +
               "## 用户问题\n" +
               userQuery + "\n\n" +
               "## 输出格式\n" +
               "请输出 JSON 数组，每个元素包含：\n" +
               "{\n" +
               "  \"chunk_index\": 片段序号,\n" +
               "  \"relevance_score\": 相关性分数(0-100),\n" +
               "  \"reason\": \"简要说明相关性原因\"\n" +
               "}\n\n" +
               "## 评估结果\n";
    }
}