package org.example.milvuschinabank.rag.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class DocumentParserService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentParserService.class);

    @Autowired
    private TikaDocumentParser tikaParser;

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "pdf",
            "doc", "docx",
            "xls", "xlsx",
            "ppt", "pptx",
            "txt", "text",
            "html", "htm",
            "xml",
            "csv",
            "rtf",
            "png", "jpg", "jpeg", "bmp", "gif", "tiff", "tif",
            "md", "markdown"
    );

    public String parse(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(fileName).toLowerCase();
        logger.info("开始解析文件: {}, 类型: {}", fileName, extension);

        if (!isSupported(extension)) {
            throw new UnsupportedOperationException("不支持的文件类型: " + extension + 
                    "，支持的格式: " + String.join(", ", SUPPORTED_EXTENSIONS));
        }

        long startTime = System.currentTimeMillis();
        String content = tikaParser.parse(file);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("文件解析完成: {}, 内容长度: {} 字符, 耗时: {} ms", 
                fileName, content.length(), duration);

        return content;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    public boolean isSupported(String extension) {
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }
}