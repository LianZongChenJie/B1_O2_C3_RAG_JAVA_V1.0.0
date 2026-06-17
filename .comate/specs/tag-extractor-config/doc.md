# 标签提取配置外部化

## 需求背景

将 `TagExtractor.java` 中硬编码的标签提取规则（业务关键词、实体正则、主题模式）迁移到 `application.yml` 配置文件，实现配置与代码分离，便于维护和扩展。

## 技术方案

### 1. 配置结构设计

在 `application.yml` 中新增 `tag-extractor` 配置节点：

```yaml
tag-extractor:
  # 业务标签关键词（金融领域）
  business-keywords:
    - 外汇
    - 汇率
    - 结汇
    - 购汇
    - 跨境
    - 结算
    - 信用证
    - 贸易融资
    - 国际结算
    - 外币
    - USD
    - EUR
    - GBP
    - JPY
    - 存款
    - 贷款
    - 利率
    - 理财
    - 投资
    - 基金
    - 保险
    - 信用卡
    - 借记卡
    - 转账
    - 汇款
    - SWIFT
    - 清算
  
  # 实体标签正则配置
  entity-patterns:
    currency: "[A-Z]{3}"
    rate: "\\d+\\.?\\d*%"
    date: "\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}"
  
  # 主题标签配置
  topic-patterns:
    chapter-prefixes:
      - "^[第\\d]+[章节].*"
      - "^\\d+[、.].*"
    min-length-for-long-text: 500
```

### 2. 配置文件类设计

创建 `TagExtractorConfig.java` 配置类：

```java
@Configuration
@ConfigurationProperties(prefix = "tag-extractor")
public class TagExtractorConfig {
    private List<String> businessKeywords;
    private EntityPatterns entityPatterns;
    private TopicPatterns topicPatterns;
    
    // getters and setters
}
```

### 3. 修改 TagExtractor.java

- 移除硬编码常量
- 通过构造函数注入配置
- 使用配置中的值进行标签提取

## 涉及文件

| 文件 | 操作 |
|------|------|
| `src/main/resources/application.yml` | 新增 tag-extractor 配置节点 |
| `src/main/java/.../config/TagExtractorConfig.java` | 新建配置类 |
| `src/main/java/.../service/TagExtractor.java` | 修改，依赖注入配置 |

## 边界条件

- 配置为空时使用空列表，不抛异常
- 正则表达式格式错误时记录日志并跳过
- 关键词匹配仍使用 indexOf 优化性能
