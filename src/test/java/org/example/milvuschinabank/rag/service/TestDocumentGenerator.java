//package org.example.milvuschinabank.rag.service;
//
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.PDPageContentStream;
//import org.apache.pdfbox.pdmodel.font.PDType1Font;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.apache.poi.xwpf.usermodel.XWPFDocument;
//import org.apache.poi.xwpf.usermodel.XWPFParagraph;
//import org.apache.poi.xwpf.usermodel.XWPFRun;
//
//import javax.imageio.ImageIO;
//import java.awt.Graphics2D;
//import java.awt.RenderingHints;
//import java.awt.Font;
//import java.awt.Color;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
///**
// * 测试文档生成器
// * 生成用于测试的PDF、Word、Excel、图片和文本文件
// *
// * @author RAG Team
// * @since 2024
// */
//public class TestDocumentGenerator {
//
//    private static final String OUTPUT_DIR = "test-documents";
//
//    public static void main(String[] args) {
//        try {
//            File dir = new File(OUTPUT_DIR);
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
//
//            System.out.println("开始生成测试文档...");
//
//            generatePlainTextFile();
//            System.out.println("✓ 纯文本文件生成完成");
//
//            generatePdfFile();
//            System.out.println("✓ PDF文件生成完成");
//
//            generateWordFile();
//            System.out.println("✓ Word文件生成完成");
//
//            generateExcelFile();
//            System.out.println("✓ Excel文件生成完成");
//
//            generateTestImage();
//            System.out.println("✓ 测试图片生成完成");
//
//            System.out.println("\n所有测试文档已生成到目录: " + new File(OUTPUT_DIR).getAbsolutePath());
//
//        } catch (Exception e) {
//            System.err.println("生成测试文档失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 生成纯文本测试文件
//     */
//    private static void generatePlainTextFile() throws IOException {
//        String content =
//            "第一章 外汇管理政策概述\n\n" +
//            "外汇管理是指国家对外汇收支、买卖、借贷、转移以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。\n" +
//            "我国实行以市场供求为基础的、单一的、有管理的浮动汇率制度。中国人民银行根据银行间外汇市场形成的价格，公布人民币对主要外币的汇率。\n" +
//            "国家对外汇实行集中管理、统一经营的方针。\n\n" +
//
//            "第二章 结售汇业务管理\n\n" +
//            "实行银行结汇、售汇制度，境内机构的经常项目外汇收入必须结汇给外汇指定银行。\n" +
//            "个人年度购汇额度为等值5万美元。超过年度总额的，需提供相关证明材料。\n" +
//            "结汇是指外汇收入所有者将其持有的外汇卖给外汇指定银行，按一定汇率取得等值本币的行为。\n" +
//            "售汇是指外汇指定银行向外汇使用者出售外汇，收取等值本币的行为。\n\n" +
//
//            "第三章 跨境贸易人民币结算\n\n" +
//            "跨境贸易人民币结算是指经国家允许结算的贸易，以人民币报关并且以人民币结算的贸易结算。\n" +
//            "企业开展跨境贸易人民币结算业务，应当选择具有相关资质的银行作为结算银行。\n" +
//            "跨境贸易人民币结算不纳入外汇核销管理范围。\n" +
//            "2024-01-15起执行新政策，简化跨境人民币结算流程。\n\n" +
//
//            "第四章 存款利率管理\n\n" +
//            "活期存款年利率为0.35%。一年期定期存款基准利率为1.50%。\n" +
//            "各商业银行可以在基准利率基础上浮动定价。\n" +
//            "大额存单起存金额为20万元，利率可上浮至基准利率的1.45倍。\n" +
//            "USD存款利率为2.5%，EUR存款利率为1.8%。\n" +
//            "GBP存款利率为1.5%，JPY存款利率为0.01%。\n\n" +
//
//            "第五章 贷款业务管理\n\n" +
//            "个人住房贷款基准利率为4.9%。商业用房贷款基准利率为5.9%。\n" +
//            "企业流动资金贷款基准利率为4.35%。中长期贷款基准利率为4.75%。\n" +
//            "贷款审批需要提供财务报表、担保材料等相关文件。\n" +
//            "逾期贷款将按日计收罚息，罚息利率为合同利率的1.5倍。";
//
//        File file = new File(OUTPUT_DIR, "外汇管理政策.txt");
//        java.nio.file.Files.write(file.toPath(), content.getBytes("UTF-8"));
//    }
//
//    /**
//     * 生成PDF测试文件
//     */
//    private static void generatePdfFile() throws IOException {
//        try (PDDocument document = new PDDocument()) {
//            PDPage page1 = new PDPage();
//            document.addPage(page1);
//
//            try (PDPageContentStream contentStream = new PDPageContentStream(document, page1)) {
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
//                contentStream.newLineAtOffset(50, 750);
//                contentStream.showText("Foreign Exchange Management Policy");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA, 12);
//                contentStream.newLineAtOffset(50, 720);
//                contentStream.showText("Chapter 1: Overview of Foreign Exchange Management");
//                contentStream.newLineAtOffset(0, -20);
//                contentStream.showText("Foreign exchange management refers to the management measures implemented by the state");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("on foreign exchange receipts and payments, trading, lending, transfers, and international");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("settlements, exchange rates, and foreign exchange markets.");
//                contentStream.newLineAtOffset(0, -20);
//                contentStream.showText("China implements a managed floating exchange rate system based on market supply and demand.");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("The People's Bank of China publishes the RMB exchange rates against major foreign currencies");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("based on the prices formed in the interbank foreign exchange market.");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
//                contentStream.newLineAtOffset(50, 580);
//                contentStream.showText("Chapter 2: Settlement and Sale of Foreign Exchange");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA, 12);
//                contentStream.newLineAtOffset(50, 550);
//                contentStream.showText("The system of bank settlement and sale of foreign exchange is implemented.");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("The annual foreign exchange purchase quota for individuals is USD 50,000 equivalent.");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("Amounts exceeding the annual quota require supporting documentation.");
//                contentStream.newLineAtOffset(0, -20);
//                contentStream.showText("Settlement refers to the act of selling foreign exchange to designated banks");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("at a certain exchange rate to obtain equivalent local currency.");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
//                contentStream.newLineAtOffset(50, 420);
//                contentStream.showText("Chapter 3: Cross-border Trade RMB Settlement");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA, 12);
//                contentStream.newLineAtOffset(50, 390);
//                contentStream.showText("Cross-border trade RMB settlement refers to trade that is declared in RMB");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("and settled in RMB as permitted by the state.");
//                contentStream.newLineAtOffset(0, -20);
//                contentStream.showText("Enterprises conducting cross-border trade RMB settlement business should select");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("banks with relevant qualifications as settlement banks.");
//                contentStream.newLineAtOffset(0, -20);
//                contentStream.showText("New policy effective from 2024-01-15: Simplified cross-border RMB settlement process.");
//                contentStream.endText();
//            }
//
//            PDPage page2 = new PDPage();
//            document.addPage(page2);
//
//            try (PDPageContentStream contentStream = new PDPageContentStream(document, page2)) {
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
//                contentStream.newLineAtOffset(50, 750);
//                contentStream.showText("Chapter 4: Deposit Interest Rate Management");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA, 12);
//                contentStream.newLineAtOffset(50, 720);
//                contentStream.showText("Current deposit annual interest rate: 0.35%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("One-year fixed deposit benchmark rate: 1.50%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("Commercial banks can float prices based on the benchmark rate.");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("Large-denomination certificates of deposit: minimum 200,000 RMB");
//                contentStream.newLineAtOffset(0, -20);
//                contentStream.showText("Foreign Currency Deposit Rates:");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("- USD: 2.5%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("- EUR: 1.8%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("- GBP: 1.5%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("- JPY: 0.01%");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
//                contentStream.newLineAtOffset(50, 520);
//                contentStream.showText("Chapter 5: Loan Business Management");
//                contentStream.endText();
//
//                contentStream.beginText();
//                contentStream.setFont(PDType1Font.HELVETICA, 12);
//                contentStream.newLineAtOffset(50, 490);
//                contentStream.showText("Personal housing loan benchmark rate: 4.9%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("Commercial property loan benchmark rate: 5.9%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("Corporate working capital loan benchmark rate: 4.35%");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("Medium and long-term loan benchmark rate: 4.75%");
//                contentStream.newLineAtOffset(0, -20);
//                contentStream.showText("Loan approval requires financial statements, guarantee materials, and related documents.");
//                contentStream.newLineAtOffset(0, -15);
//                contentStream.showText("Overdue loans will incur penalty interest at 1.5 times the contract rate.");
//                contentStream.endText();
//            }
//
//            document.save(new File(OUTPUT_DIR, "外汇管理政策.pdf"));
//        }
//    }
//
//    /**
//     * 生成Word测试文件
//     */
//    private static void generateWordFile() throws IOException {
//        try (XWPFDocument document = new XWPFDocument()) {
//            addHeading(document, "外汇管理政策", 24);
//            addParagraph(document, "第一章 外汇管理政策概述", 16, true);
//            addParagraph(document,
//                "外汇管理是指国家对外汇收支、买卖、借贷、转移以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。" +
//                "我国实行以市场供求为基础的、单一的、有管理的浮动汇率制度。" +
//                "中国人民银行根据银行间外汇市场形成的价格，公布人民币对主要外币的汇率。",
//                12, false);
//
//            addParagraph(document, "第二章 结售汇业务管理", 16, true);
//            addParagraph(document,
//                "实行银行结汇、售汇制度，境内机构的经常项目外汇收入必须结汇给外汇指定银行。" +
//                "个人年度购汇额度为等值5万美元。超过年度总额的，需提供相关证明材料。" +
//                "结汇是指外汇收入所有者将其持有的外汇卖给外汇指定银行，按一定汇率取得等值本币的行为。" +
//                "售汇是指外汇指定银行向外汇使用者出售外汇，收取等值本币的行为。",
//                12, false);
//
//            addParagraph(document, "第三章 跨境贸易人民币结算", 16, true);
//            addParagraph(document,
//                "跨境贸易人民币结算是指经国家允许结算的贸易，以人民币报关并且以人民币结算的贸易结算。" +
//                "企业开展跨境贸易人民币结算业务，应当选择具有相关资质的银行作为结算银行。" +
//                "跨境贸易人民币结算不纳入外汇核销管理范围。" +
//                "2024-01-15起执行新政策，简化跨境人民币结算流程。",
//                12, false);
//
//            addParagraph(document, "第四章 存款利率管理", 16, true);
//            addParagraph(document,
//                "活期存款年利率为0.35%。一年期定期存款基准利率为1.50%。" +
//                "各商业银行可以在基准利率基础上浮动定价。" +
//                "大额存单起存金额为20万元，利率可上浮至基准利率的1.45倍。" +
//                "USD存款利率为2.5%，EUR存款利率为1.8%。" +
//                "GBP存款利率为1.5%，JPY存款利率为0.01%。",
//                12, false);
//
//            addParagraph(document, "第五章 贷款业务管理", 16, true);
//            addParagraph(document,
//                "个人住房贷款基准利率为4.9%。商业用房贷款基准利率为5.9%。" +
//                "企业流动资金贷款基准利率为4.35%。中长期贷款基准利率为4.75%。" +
//                "贷款审批需要提供财务报表、担保材料等相关文件。" +
//                "逾期贷款将按日计收罚息，罚息利率为合同利率的1.5倍。",
//                12, false);
//
//            try (FileOutputStream out = new FileOutputStream(new File(OUTPUT_DIR, "外汇管理政策.docx"))) {
//                document.write(out);
//            }
//        }
//    }
//
//    private static void addHeading(XWPFDocument document, String text, int fontSize) {
//        XWPFParagraph paragraph = document.createParagraph();
//        XWPFRun run = paragraph.createRun();
//        run.setText(text);
//        run.setBold(true);
//        run.setFontSize(fontSize);
//        run.setFontFamily("宋体");
//    }
//
//    private static void addParagraph(XWPFDocument document, String text, int fontSize, boolean bold) {
//        XWPFParagraph paragraph = document.createParagraph();
//        XWPFRun run = paragraph.createRun();
//        run.setText(text);
//        run.setBold(bold);
//        run.setFontSize(fontSize);
//        run.setFontFamily("宋体");
//    }
//
//    /**
//     * 生成Excel测试文件
//     */
//    private static void generateExcelFile() throws IOException {
//        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
//            Sheet sheet1 = workbook.createSheet("存款利率表");
//
//            CellStyle headerStyle = workbook.createCellStyle();
//            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
//            headerFont.setBold(true);
//            headerFont.setFontHeightInPoints((short) 12);
//            headerStyle.setFont(headerFont);
//
//            Row headerRow = sheet1.createRow(0);
//            String[] headers = {"存款类型", "币种", "年利率(%)", "起存金额", "备注"};
//            for (int i = 0; i < headers.length; i++) {
//                Cell cell = headerRow.createCell(i);
//                cell.setCellValue(headers[i]);
//                cell.setCellStyle(headerStyle);
//            }
//
//            Object[][] data = {
//                {"活期存款", "CNY", 0.35, 0, "随时存取"},
//                {"一年定期", "CNY", 1.50, 50, "固定期限"},
//                {"三年定期", "CNY", 2.75, 50, "固定期限"},
//                {"大额存单", "CNY", 2.18, 200000, "可转让"},
//                {"活期存款", "USD", 2.50, 0, "外币存款"},
//                {"活期存款", "EUR", 1.80, 0, "外币存款"},
//                {"活期存款", "GBP", 1.50, 0, "外币存款"},
//                {"活期存款", "JPY", 0.01, 0, "外币存款"}
//            };
//
//            for (int i = 0; i < data.length; i++) {
//                Row row = sheet1.createRow(i + 1);
//                for (int j = 0; j < data[i].length; j++) {
//                    Cell cell = row.createCell(j);
//                    if (data[i][j] instanceof String) {
//                        cell.setCellValue((String) data[i][j]);
//                    } else if (data[i][j] instanceof Double) {
//                        cell.setCellValue((Double) data[i][j]);
//                    } else if (data[i][j] instanceof Integer) {
//                        cell.setCellValue((Integer) data[i][j]);
//                    }
//                }
//            }
//
//            Sheet sheet2 = workbook.createSheet("贷款利率表");
//
//            Row headerRow2 = sheet2.createRow(0);
//            String[] headers2 = {"贷款类型", "基准利率(%)", "期限", "担保要求", "审批时间"};
//            for (int i = 0; i < headers2.length; i++) {
//                Cell cell = headerRow2.createCell(i);
//                cell.setCellValue(headers2[i]);
//                cell.setCellStyle(headerStyle);
//            }
//
//            Object[][] data2 = {
//                {"个人住房贷款", 4.90, "30年", "房产抵押", "15个工作日"},
//                {"商业用房贷款", 5.90, "10年", "房产抵押", "10个工作日"},
//                {"流动资金贷款", 4.35, "1年", "信用/担保", "5个工作日"},
//                {"中长期贷款", 4.75, "5年", "抵押/质押", "20个工作日"},
//                {"个人消费贷款", 6.00, "3年", "信用", "3个工作日"}
//            };
//
//            for (int i = 0; i < data2.length; i++) {
//                Row row = sheet2.createRow(i + 1);
//                for (int j = 0; j < data2[i].length; j++) {
//                    Cell cell = row.createCell(j);
//                    if (data2[i][j] instanceof String) {
//                        cell.setCellValue((String) data2[i][j]);
//                    } else if (data2[i][j] instanceof Double) {
//                        cell.setCellValue((Double) data2[i][j]);
//                    }
//                }
//            }
//
//            try (FileOutputStream out = new FileOutputStream(new File(OUTPUT_DIR, "银行利率表.xlsx"))) {
//                workbook.write(out);
//            }
//        }
//    }
//
//    /**
//     * 生成测试图片（包含文字）
//     */
//    private static void generateTestImage() throws IOException {
//        int width = 800;
//        int height = 600;
//        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g2d = image.createGraphics();
//
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//        g2d.setColor(Color.WHITE);
//        g2d.fillRect(0, 0, width, height);
//
//        g2d.setColor(Color.BLACK);
//        g2d.setFont(new Font("SimSun", Font.BOLD, 32));
//        g2d.drawString("外汇管理政策", 250, 80);
//
//        g2d.setFont(new Font("SimSun", Font.PLAIN, 20));
//        g2d.drawString("第一章 外汇管理概述", 50, 140);
//        g2d.drawString("外汇管理是指国家对外汇收支、买卖、借贷、转移", 50, 180);
//        g2d.drawString("以及国际间的结算、外汇汇率和外汇市场等实行的管理措施。", 50, 210);
//
//        g2d.drawString("第二章 结售汇业务", 50, 270);
//        g2d.drawString("个人年度购汇额度为等值5万美元。", 50, 310);
//        g2d.drawString("超过年度总额的，需提供相关证明材料。", 50, 340);
//
//        g2d.drawString("第三章 存款利率", 50, 400);
//        g2d.drawString("活期存款年利率: 0.35%", 50, 440);
//        g2d.drawString("一年期定期存款: 1.50%", 50, 470);
//        g2d.drawString("USD存款利率: 2.5%", 50, 500);
//        g2d.drawString("EUR存款利率: 1.8%", 50, 530);
//
//        g2d.dispose();
//
//        ImageIO.write(image, "png", new File(OUTPUT_DIR, "外汇管理政策.png"));
//    }
//}