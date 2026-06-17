package org.example.milvuschinabank.rag.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 测试文档生成器 - 单元测试版本
 * 生成用于测试的纯文本文件
 * 
 * @author RAG Team
 * @since 2024
 */
public class SimpleTestDocumentGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SimpleTestDocumentGenerator.class);
    private static final String OUTPUT_DIR = "test-documents";

    @Test
    @DisplayName("生成测试文档")
    public void generateTestDocuments() throws IOException {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        logger.info("开始生成测试文档...");

        generatePlainTextFile();
        logger.info("✓ 纯文本文件生成完成: {}/外汇管理政策.txt", dir.getAbsolutePath());

        logger.info("\n所有测试文档已生成到目录: {}", dir.getAbsolutePath());
    }

    private void generatePlainTextFile() throws IOException {
        String content = 
            "第一章 外汇管理政策概述\n\n" +
            "外汇管理是指国家对外汇收支、买卖、借贷、转移以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。\n" +
            "我国实行以市场供求为基础的、单一的、有管理的浮动汇率制度。中国人民银行根据银行间外汇市场形成的价格，公布人民币对主要外币的汇率。\n" +
            "国家对外汇实行集中管理、统一经营的方针。\n\n" +
            
            "第二章 结售汇业务管理\n\n" +
            "实行银行结汇、售汇制度，境内机构的经常项目外汇收入必须结汇给外汇指定银行。\n" +
            "个人年度购汇额度为等值5万美元。超过年度总额的，需提供相关证明材料。\n" +
            "结汇是指外汇收入所有者将其持有的外汇卖给外汇指定银行，按一定汇率取得等值本币的行为。\n" +
            "售汇是指外汇指定银行向外汇使用者出售外汇，收取等值本币的行为。\n\n" +
            
            "第三章 跨境贸易人民币结算\n\n" +
            "跨境贸易人民币结算是指经国家允许结算的贸易，以人民币报关并且以人民币结算的贸易结算。\n" +
            "企业开展跨境贸易人民币结算业务，应当选择具有相关资质的银行作为结算银行。\n" +
            "跨境贸易人民币结算不纳入外汇核销管理范围。\n" +
            "2024-01-15起执行新政策，简化跨境人民币结算流程。\n\n" +
            
            "第四章 存款利率管理\n\n" +
            "活期存款年利率为0.35%。一年期定期存款基准利率为1.50%。\n" +
            "各商业银行可以在基准利率基础上浮动定价。\n" +
            "大额存单起存金额为20万元，利率可上浮至基准利率的1.45倍。\n" +
            "USD存款利率为2.5%，EUR存款利率为1.8%。\n" +
            "GBP存款利率为1.5%，JPY存款利率为0.01%。\n\n" +
            
            "第五章 贷款业务管理\n\n" +
            "个人住房贷款基准利率为4.9%。商业用房贷款基准利率为5.9%。\n" +
            "企业流动资金贷款基准利率为4.35%。中长期贷款基准利率为4.75%。\n" +
            "贷款审批需要提供财务报表、担保材料等相关文件。\n" +
            "逾期贷款将按日计收罚息，罚息利率为合同利率的1.5倍。";

        File file = new File(OUTPUT_DIR, "外汇管理政策.txt");
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content);
        }
    }
}