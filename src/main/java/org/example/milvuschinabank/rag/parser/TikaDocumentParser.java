package org.example.milvuschinabank.rag.parser;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于Apache Tika的统一文档解析器
 * 支持格式：PDF、Word、Excel、PPT、TXT、HTML、XML、图片(OCR)等
 * 
 * @author RAG Team
 * @since 2024
 */
@Component
public class TikaDocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(TikaDocumentParser.class);
    
    private final Tika tika;
    private final AutoDetectParser parser;
    private final Tesseract tesseract;

    public TikaDocumentParser() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
        this.tesseract = new Tesseract();
        
        // 设置中文语言包路径（兼容 JAR 包部署）
        try {
            ClassPathResource resource = new ClassPathResource("tessdata/chi_sim.traineddata");
            
            // 将语言包从 JAR 包中解压到临时目录
            Path tempDir = Files.createTempDirectory("tessdata");
            File tempFile = new File(tempDir.toFile(), "chi_sim.traineddata");
            
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, tempFile.toPath());
            }
            
            // 设置 datapath 为临时目录（不包含文件名）
            tesseract.setDatapath(tempDir.toAbsolutePath().toString());
            tesseract.setLanguage("chi_sim");
            
            logger.info("OCR语言包已解压到: {}", tempDir.toAbsolutePath());
        } catch (Exception e) {
            logger.error("OCR语言包加载失败，请确保 src/main/resources/tessdata/chi_sim.traineddata 存在", e);
        }
        
        logger.info("Apache Tika文档解析器初始化完成（支持中文OCR）");
    }

    /**
     * 解析文件内容
     * 
     * @param file 上传的文件
     * @return 解析后的文本内容
     * @throws IOException 解析异常
     */
    public String parse(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        logger.info("开始解析文件: {}, 类型: {}", fileName, contentType);

        // 读取文件到字节数组，以便多次使用
        byte[] fileBytes = file.getBytes();
        
        try {
            String content = parseWithTika(fileBytes, fileName);
            
            if (content == null || content.trim().isEmpty()) {
                logger.warn("文件解析结果为空: {}", fileName);
                return "";
            }

            String cleanedContent = content.trim();
            logger.info("文件解析完成: {}, 文本长度: {} 字符", fileName, cleanedContent.length());
            
            return cleanedContent;
            
        } catch (TikaException e) {
            logger.error("Tika解析失败: {}", fileName, e);
            throw new IOException("文档解析失败: " + e.getMessage(), e);
        } catch (SAXException e) {
            logger.error("SAX解析异常: {}", fileName, e);
            throw new IOException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用Tika解析文件
     * 
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return 解析后的文本
     */
    private String parseWithTika(byte[] fileBytes, String fileName) 
            throws IOException, TikaException, SAXException {
        
        // 检测文件类型
        String detectedType = tika.detect(fileBytes, fileName);
        
        // 如果是图片文件，使用OCR识别
        if (detectedType != null && detectedType.startsWith("image/")) {
            logger.info("检测到图片文件，使用OCR识别: {}", fileName);
            return parseImageWithOCR(fileBytes, fileName);
        }
        
        // 非图片文件使用Tika解析
        try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(fileBytes)) {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            
            ParseContext context = new ParseContext();
            context.set(org.apache.tika.parser.Parser.class, parser);
            
            BodyContentHandler handler = new BodyContentHandler(-1);
            
            parser.parse(inputStream, handler, metadata, context);
            
            String content = handler.toString();
            
            logger.info("Tika解析元数据 - 内容类型: {}, 作者: {}, 创建时间: {}", 
                    metadata.get(Metadata.CONTENT_TYPE),
                    metadata.get(TikaCoreProperties.CREATOR),
                    metadata.get(TikaCoreProperties.CREATED));
            
            return content;
        }
    }

    /**
     * 使用OCR识别图片中的文字
     * 
     * @param fileBytes 图片字节数组
     * @param fileName 文件名
     * @return 识别出的文字
     */
    private String parseImageWithOCR(byte[] fileBytes, String fileName) throws IOException {
        try {
            try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(fileBytes)) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    logger.warn("无法读取图片: {}", fileName);
                    return "";
                }
                
                logger.info("开始OCR识别图片: {}", fileName);
                String result = tesseract.doOCR(image);
                
                if (result == null || result.trim().isEmpty()) {
                    logger.warn("OCR识别结果为空: {}", fileName);
                    return "";
                }
                
                logger.info("OCR识别完成: {}, 识别文字长度: {} 字符", fileName, result.length());
                return result;
            }
            
        } catch (TesseractException e) {
            logger.error("OCR识别失败: {}", fileName, e);
            throw new IOException("图片OCR识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检测文件类型
     * 
     * @param file 上传的文件
     * @return 检测到的MIME类型
     */
    public String detectFileType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            logger.error("文件类型检测失败: {}", file.getOriginalFilename(), e);
            return "application/octet-stream";
        }
    }
}