package org.example.milvuschinabank.rag.parser;

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

import java.io.IOException;
import java.io.InputStream;

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

    public TikaDocumentParser() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
        logger.info("Apache Tika文档解析器初始化完成");
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

        try (InputStream inputStream = file.getInputStream()) {
            String content = parseWithTika(inputStream, fileName);
            
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
     * @param inputStream 文件输入流
     * @param fileName 文件名
     * @return 解析后的文本
     */
    private String parseWithTika(InputStream inputStream, String fileName) 
            throws IOException, TikaException, SAXException {
        
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