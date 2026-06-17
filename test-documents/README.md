# 测试文档使用说明

## 已生成的测试文档

所有测试文档位于 `test-documents/` 目录：

| 文件名 | 类型 | 说明 |
|--------|------|------|
| [外汇管理政策.txt](file:///E:/code/chinabank/milvuschinabank/test-documents/外汇管理政策.txt) | 纯文本 | 包含5章外汇管理政策内容 |
| [外汇管理政策.docx](file:///E:/code/chinabank/milvuschinabank/test-documents/外汇管理政策.docx) | Word文档 | 带标题层级的Word文档 |
| [银行利率表.xlsx](file:///E:/code/chinabank/milvuschinabank/test-documents/银行利率表.xlsx) | Excel表格 | 包含存款利率表和贷款利率表 |
| [外汇管理政策.png](file:///E:/code/chinabank/milvuschinabank/test-documents/外汇管理政策.png) | 图片 | 包含文字的图片（可用于OCR测试） |

## 如何测试上传

### 1. 启动应用

```bash
cd E:\code\chinabank\milvuschinabank
mvn spring-boot:run
```

### 2. 使用curl测试

**测试文本文件上传**:
```bash
curl -X POST http://localhost:8080/api/rag/upload ^
  -F "file=@test-documents/外汇管理政策.txt" ^
  -F "chunkSize=500"
```

**测试Word文件上传**:
```bash
curl -X POST http://localhost:8080/api/rag/upload ^
  -F "file=@test-documents/外汇管理政策.docx" ^
  -F "chunkSize=500"
```

**测试Excel文件上传**:
```bash
curl -X POST http://localhost:8080/api/rag/upload ^
  -F "file=@test-documents/银行利率表.xlsx" ^
  -F "chunkSize=300"
```

**测试图片上传**:
```bash
curl -X POST http://localhost:8080/api/rag/upload ^
  -F "file=@test-documents/外汇管理政策.png" ^
  -F "chunkSize=200"
```

**批量上传**:
```bash
curl -X POST http://localhost:8080/api/rag/upload/batch ^
  -F "files=@test-documents/外汇管理政策.txt" ^
  -F "files=@test-documents/外汇管理政策.docx" ^
  -F "files=@test-documents/银行利率表.xlsx"
```

### 3. 使用浏览器测试

访问: http://localhost:8080/upload.html

选择文件并点击"上传并入库"按钮。

## 预期响应

成功的响应示例:
```json
{
  "success": true,
  "docId": "doc_外汇管理政策_1718600000000_a1b2c3d4",
  "fileName": "外汇管理政策.txt",
  "chunkCount": 15,
  "textLength": 7500,
  "processingTime": 1234,
  "message": "文档入库成功"
}
```

## 测试文档内容

### 外汇管理政策.txt / .docx

包含以下5章内容：
1. **第一章 外汇管理政策概述** - 外汇管理基本概念
2. **第二章 结售汇业务管理** - 结汇、售汇制度
3. **第三章 跨境贸易人民币结算** - 跨境结算政策
4. **第四章 存款利率管理** - 各类存款利率
5. **第五章 贷款业务管理** - 贷款利率和审批

### 银行利率表.xlsx

包含两个工作表：
- **存款利率表**: 活期、定期、大额存单、外币存款利率
- **贷款利率表**: 个人住房、商业用房、流动资金等贷款利率

### 外汇管理政策.png

包含文字的图片，可用于测试OCR识别功能（需要配置Tesseract）。

## 重新生成测试文档

如果需要重新生成测试文档，运行：

```bash
python generate_test_docs.py
```

或者运行Maven测试：

```bash
mvn test -Dtest=SimpleTestDocumentGenerator
```