package org.example.milvuschinabank.rag.repository;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.QueryResults;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import org.example.milvuschinabank.rag.config.RagConfig;
import org.example.milvuschinabank.rag.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 2.3 标量过滤索引建设
 * 
 * Milvus 切片数据访问层，负责向量存储、标量过滤、索引管理。
 * 
 * 核心功能：
 * 1. Collection 管理：创建和管理 Milvus Collection，定义字段 Schema
 * 2. 标量索引建设：
 *    - pos 字段：STL_SORT 索引，支持位置范围查询
 *    - semantic_cohesion 字段：STL_SORT 索引，支持连贯度过滤
 *    - doc_id 字段：自动倒排索引，支持文档ID精确查询
 *    - pre_seg_id/next_seg_id 字段：自动倒排索引，支持邻接切片查询
 *    - tags 字段：自动倒排索引，支持标签过滤查询
 * 3. 向量索引：使用 AUTOINDEX 构建向量检索索引
 * 4. 数据操作：批量插入、按条件查询、向量检索
 * 5. 降级机制：Milvus 连接失败时自动降级到内存模拟模式
 * 
 * 索引策略：
 * - 数值类型字段（pos, semantic_cohesion）：显式创建 STL_SORT 索引
 * - VarChar 类型字段（doc_id, pre_seg_id, next_seg_id, tags）：使用 Milvus 自动倒排索引
 * - 向量字段（vector）：使用 AUTOINDEX 自动索引
 * 
 * @author RAG Team
 * @since 2024
 */
@Repository
public class MilvusChunkRepository {

    private static final Logger logger = LoggerFactory.getLogger(MilvusChunkRepository.class);

    @Autowired
    private RagConfig ragConfig;

    // 模拟存储（内存中的 Map）- 用于 Milvus 连接失败时的降级
    private final Map<String, DocumentChunk> chunkStore = new ConcurrentHashMap<>();
    private final Map<String, List<String>> docIndex = new ConcurrentHashMap<>();
    private final Map<String, List<String>> tagIndex = new ConcurrentHashMap<>();

    private MilvusClient milvusClient;
    private boolean milvusConnected = false;

    /**
     * 初始化 Milvus 连接和 Collection
     */
    @PostConstruct
    public void init() {
        try {
            logger.info("尝试连接 Milvus: {}:{}", ragConfig.getMilvusHost(), ragConfig.getMilvusPort());
            
            // 构建连接参数
            ConnectParam.Builder connectBuilder = ConnectParam.newBuilder()
                    .withHost(ragConfig.getMilvusHost())
                    .withPort(ragConfig.getMilvusPort());

            // 如果启用认证，添加用户名和密码
            if (ragConfig.isMilvusUseAuth()) {
                connectBuilder.withAuthorization(ragConfig.getMilvusUsername(),
                        ragConfig.getMilvusPassword());
            }

            // 创建客户端
            milvusClient = new MilvusServiceClient(connectBuilder.build());
            
            // 测试连接 - 列出所有 collections
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(ragConfig.getCollectionName())
                            .build());
            
            if (hasCollection.getStatus() == R.Status.Success.getCode()) {
                logger.info("Milvus 连接成功！");
                milvusConnected = true;
                
                // 确保 Collection 存在
                ensureCollectionExists();
                
                logger.info("MilvusChunkRepository 初始化完成（真实模式）");
            } else {
                logger.warn("Milvus 连接测试失败: {}", hasCollection.getMessage());
                milvusConnected = false;
            }
        } catch (Exception e) {
            logger.warn("Milvus 连接失败: {}，使用模拟模式", e.getMessage());
            milvusConnected = false;
            milvusClient = null;
        }
    }

    /**
     * 确保 Collection 存在
     */
    private void ensureCollectionExists() {
        String collectionName = ragConfig.getCollectionName();
        
        try {
            // 检查 Collection 是否存在
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());

            if (hasCollection.getData()) {
                logger.info("Collection [{}] 已存在", collectionName);
                // 加载 Collection 到内存
                loadCollection(collectionName);
            } else {
                logger.info("Collection [{}] 不存在，开始创建", collectionName);
                createCollection(collectionName);
                createScalarIndexes(collectionName);
                createVectorIndex(collectionName);
            }
            
            // 确保 Collection 已加载
            loadCollection(collectionName);
            logger.info("Collection [{}] 准备完成", collectionName);
        } catch (Exception e) {
            logger.error("检查 Collection 失败: {}", e.getMessage());
        }
    }

    /**
     * 创建 Collection
     */
    private void createCollection(String collectionName) {
        List<FieldType> fieldsSchema = new ArrayList<>();

        // seg_id - 主键
        fieldsSchema.add(FieldType.newBuilder()
                .withName("seg_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .withPrimaryKey(true)
                .withAutoID(false)
                .build());

        // doc_id
        fieldsSchema.add(FieldType.newBuilder()
                .withName("doc_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .build());

        // pos
        fieldsSchema.add(FieldType.newBuilder()
                .withName("pos")
                .withDataType(DataType.Int64)
                .build());

        // pre_seg_id
        fieldsSchema.add(FieldType.newBuilder()
                .withName("pre_seg_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .build());

        // next_seg_id
        fieldsSchema.add(FieldType.newBuilder()
                .withName("next_seg_id")
                .withDataType(DataType.VarChar)
                .withMaxLength(128)
                .build());

        // tags
        fieldsSchema.add(FieldType.newBuilder()
                .withName("tags")
                .withDataType(DataType.VarChar)
                .withMaxLength(1024)
                .build());

        // content
        fieldsSchema.add(FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build());

        // semantic_cohesion
        fieldsSchema.add(FieldType.newBuilder()
                .withName("semantic_cohesion")
                .withDataType(DataType.Float)
                .build());

        // is_semantic_boundary
        fieldsSchema.add(FieldType.newBuilder()
                .withName("is_semantic_boundary")
                .withDataType(DataType.Bool)
                .build());

        // vector
        fieldsSchema.add(FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(ragConfig.getVectorDimension())
                .build());

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldTypes(fieldsSchema)
                .build();

        R<RpcStatus> response = milvusClient.createCollection(createParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("创建 Collection 失败: " + response.getMessage());
        }
    }

    /**
     * 创建标量索引
     */
    private void createScalarIndexes(String collectionName) {
        // 只为数值类型字段创建 STL_SORT 索引
        String[] numericFields = {"pos", "semantic_cohesion"};
        
        for (String field : numericFields) {
            try {
                CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFieldName(field)
                        .withIndexType(IndexType.STL_SORT)
                        .build();

                R<RpcStatus> response = milvusClient.createIndex(indexParam);
                if (response.getStatus() == R.Status.Success.getCode()) {
                    logger.info("字段 [{}] 数值索引创建成功", field);
                }
            } catch (Exception e) {
                logger.warn("字段 [{}] 索引创建失败: {}", field, e.getMessage());
            }
        }
        
        // VarChar 字段（doc_id, pre_seg_id, next_seg_id, tags）不需要显式创建索引
        // Milvus 会自动为 VarChar 字段创建倒排索引
        logger.info("VarChar 字段使用自动倒排索引");
    }

    /**
     * 创建向量索引
     */
    private void createVectorIndex(String collectionName) {
        try {
            // 使用 AUTOINDEX - Milvus 2.5.x 推荐的自动索引类型
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("vector")
                    .withIndexType(IndexType.AUTOINDEX)
                    .withMetricType(MetricType.IP) // 内积相似度
                    .build();

            R<RpcStatus> response = milvusClient.createIndex(indexParam);
            if (response.getStatus() == R.Status.Success.getCode()) {
                logger.info("向量索引创建成功");
            } else {
                logger.warn("向量索引创建失败: {}", response.getMessage());
            }
        } catch (Exception e) {
            logger.error("向量索引创建失败: {}", e.getMessage());
        }
    }

    /**
     * 加载 Collection 到内存
     */
    private void loadCollection(String collectionName) {
        try {
            // 先检查是否已加载
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());
            
            if (!hasCollection.getData()) {
                logger.warn("Collection [{}] 不存在，无法加载", collectionName);
                return;
            }
            
            R<RpcStatus> response = milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());

            if (response.getStatus() == R.Status.Success.getCode()) {
                logger.info("Collection [{}] 加载到内存成功", collectionName);
            } else {
                logger.warn("Collection [{}] 加载失败: {}，尝试重新创建", collectionName, response.getMessage());
                // 如果加载失败，可能是 Collection 被删除了，尝试重新创建
                milvusClient.dropCollection(
                        DropCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .build());
                createCollection(collectionName);
                createScalarIndexes(collectionName);
                createVectorIndex(collectionName);
                
                // 重新加载
                response = milvusClient.loadCollection(
                        LoadCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .build());
                
                if (response.getStatus() == R.Status.Success.getCode()) {
                    logger.info("Collection [{}] 重新加载成功", collectionName);
                }
            }
        } catch (Exception e) {
            logger.error("加载 Collection 异常: {}", e.getMessage());
        }
    }

    /**
     * 批量插入切片数据
     */
    public void insertChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        // 如果 Milvus 已连接，使用真实插入
        if (milvusConnected && milvusClient != null) {
            insertToMilvus(chunks);
        } else {
            // 降级到模拟模式
            insertToMock(chunks);
        }
    }

    /**
     * 插入到真实 Milvus
     */
    private void insertToMilvus(List<DocumentChunk> chunks) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建插入字段
            List<InsertParam.Field> fields = new ArrayList<>();

            List<String> segIds = new ArrayList<>();
            List<String> docIds = new ArrayList<>();
            List<Long> positions = new ArrayList<>();
            List<String> preSegIds = new ArrayList<>();
            List<String> nextSegIds = new ArrayList<>();
            List<String> tagsList = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<Float> semanticCohesions = new ArrayList<>();
            List<Boolean> isSemanticBoundaries = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();

            for (DocumentChunk chunk : chunks) {
                segIds.add(chunk.getSegId());
                docIds.add(chunk.getDocId());
                positions.add(chunk.getPos() != null ? chunk.getPos().longValue() : 0L);
                preSegIds.add(chunk.getPreSegId() != null ? chunk.getPreSegId() : "");
                nextSegIds.add(chunk.getNextSegId() != null ? chunk.getNextSegId() : "");
                tagsList.add(chunk.getTags() != null ? String.join(",", chunk.getTags()) : "");
                contents.add(chunk.getContent() != null ? chunk.getContent() : "");
                semanticCohesions.add(chunk.getSemanticCohesion() != null ? 
                        chunk.getSemanticCohesion().floatValue() : 0.0f);
                isSemanticBoundaries.add(chunk.getSemanticBoundary() != null ? 
                        chunk.getSemanticBoundary() : false);
                
                // 如果向量未生成，使用零向量占位
                if (chunk.getVector() != null && !chunk.getVector().isEmpty()) {
                    vectors.add(chunk.getVector());
                } else {
                    vectors.add(generateZeroVector(ragConfig.getVectorDimension()));
                }
            }

            fields.add(new InsertParam.Field("seg_id", segIds));
            fields.add(new InsertParam.Field("doc_id", docIds));
            fields.add(new InsertParam.Field("pos", positions));
            fields.add(new InsertParam.Field("pre_seg_id", preSegIds));
            fields.add(new InsertParam.Field("next_seg_id", nextSegIds));
            fields.add(new InsertParam.Field("tags", tagsList));
            fields.add(new InsertParam.Field("content", contents));
            fields.add(new InsertParam.Field("semantic_cohesion", semanticCohesions));
            fields.add(new InsertParam.Field("is_semantic_boundary", isSemanticBoundaries));
            fields.add(new InsertParam.Field("vector", vectors));

            // 执行插入
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(ragConfig.getCollectionName())
                    .withFields(fields)
                    .build();

            R<MutationResult> response = milvusClient.insert(insertParam);
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                long endTime = System.currentTimeMillis();
                logger.info("成功插入 {} 条切片数据到 Milvus，耗时: {} ms", 
                        chunks.size(), (endTime - startTime));
            } else {
                logger.error("插入数据失败: {}", response.getMessage());
                throw new RuntimeException("插入数据失败: " + response.getMessage());
            }
        } catch (Exception e) {
            logger.error("插入到 Milvus 失败: {}，降级到模拟模式", e.getMessage());
            insertToMock(chunks);
        }
    }

    /**
     * 插入到模拟存储
     */
    private void insertToMock(List<DocumentChunk> chunks) {
        long startTime = System.currentTimeMillis();
        int successCount = 0;

        for (DocumentChunk chunk : chunks) {
            try {
                chunkStore.put(chunk.getSegId(), chunk);
                docIndex.computeIfAbsent(chunk.getDocId(), k -> new ArrayList<>())
                        .add(chunk.getSegId());

                if (chunk.getTags() != null) {
                    for (String tag : chunk.getTags()) {
                        tagIndex.computeIfAbsent(tag, k -> new ArrayList<>())
                                .add(chunk.getSegId());
                    }
                }

                successCount++;
            } catch (Exception e) {
                logger.error("插入切片失败: segId={}", chunk.getSegId(), e);
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("成功插入 {}/{} 条切片数据到模拟存储，耗时: {} ms", 
                successCount, chunks.size(), (endTime - startTime));
    }

    /**
     * 根据 seg_id 列表查询切片
     */
    public List<DocumentChunk> queryBySegIds(List<String> segIds) {
        if (segIds == null || segIds.isEmpty()) {
            return new ArrayList<>();
        }

        if (milvusConnected && milvusClient != null) {
            return queryFromMilvus(segIds);
        } else {
            return segIds.stream()
                    .map(chunkStore::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 从 Milvus 查询
     */
    private List<DocumentChunk> queryFromMilvus(List<String> segIds) {
        try {
            String filter = buildInFilter("seg_id", segIds);
            return executeQuery(filter);
        } catch (Exception e) {
            logger.error("从 Milvus 查询失败: {}，返回空列表", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 根据 doc_id 查询所有切片
     */
    public List<DocumentChunk> queryByDocId(String docId) {
        if (docId == null || docId.isEmpty()) {
            return new ArrayList<>();
        }

        if (milvusConnected && milvusClient != null) {
            try {
                String filter = "doc_id == \"" + docId + "\"";
                return executeQuery(filter);
            } catch (Exception e) {
                logger.error("从 Milvus 查询失败: {}，降级到模拟模式", e.getMessage());
            }
        }
        
        List<String> segIds = docIndex.get(docId);
        if (segIds == null || segIds.isEmpty()) {
            return new ArrayList<>();
        }

        return segIds.stream()
                .map(chunkStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据标签过滤查询
     */
    public List<DocumentChunk> queryByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }

        if (milvusConnected && milvusClient != null) {
            try {
                // 构建 OR 过滤条件：tags 字段是逗号分隔的字符串，使用 LIKE 匹配
                // 例如：tags LIKE "%biz:外汇%" OR tags LIKE "%biz:汇率%"
                List<String> conditions = new ArrayList<>();
                for (String tag : tags) {
                    conditions.add("tags LIKE \"%" + tag + "%\"");
                }
                String filter = String.join(" or ", conditions);
                return executeQuery(filter);
            } catch (Exception e) {
                logger.error("从 Milvus 查询标签失败: {}，降级到模拟模式", e.getMessage());
            }
        }
        
        // 降级到模拟模式
        Set<String> matchedSegIds = new HashSet<>();
        for (String tag : tags) {
            List<String> segIds = tagIndex.get(tag);
            if (segIds != null) {
                matchedSegIds.addAll(segIds);
            }
        }

        return matchedSegIds.stream()
                .map(chunkStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据位置范围查询
     */
    public List<DocumentChunk> queryByPosRange(String docId, int startPos, int endPos) {
        if (milvusConnected && milvusClient != null) {
            try {
                String filter = "doc_id == \"" + docId + "\" and pos >= " + startPos + " and pos <= " + endPos;
                return executeQuery(filter);
            } catch (Exception e) {
                logger.error("从 Milvus 查询位置范围失败: {}，降级到模拟模式", e.getMessage());
            }
        }
        
        // 降级到模拟模式
        return chunkStore.values().stream()
                .filter(chunk -> chunk.getDocId().equals(docId))
                .filter(chunk -> chunk.getPos() >= startPos && chunk.getPos() <= endPos)
                .collect(Collectors.toList());
    }

    /**
     * 查询相邻切片（根据 pre_seg_id 或 next_seg_id）
     */
    public List<DocumentChunk> queryAdjacentChunks(List<String> segIds) {
        if (segIds == null || segIds.isEmpty()) {
            return new ArrayList<>();
        }

        if (milvusConnected && milvusClient != null) {
            try {
                // 查询 pre_seg_id 或 next_seg_id 在 segIds 列表中的切片
                String segIdsStr = segIds.stream()
                        .map(id -> "\"" + id + "\"")
                        .collect(Collectors.joining(", "));
                String filter = "pre_seg_id in [" + segIdsStr + "] or next_seg_id in [" + segIdsStr + "]";
                return executeQuery(filter);
            } catch (Exception e) {
                logger.error("从 Milvus 查询相邻切片失败: {}，降级到模拟模式", e.getMessage());
            }
        }
        
        // 降级到模拟模式
        Set<String> adjacentSegIds = new HashSet<>();
        for (String segId : segIds) {
            DocumentChunk chunk = chunkStore.get(segId);
            if (chunk != null) {
                if (chunk.getPreSegId() != null) {
                    adjacentSegIds.add(chunk.getPreSegId());
                }
                if (chunk.getNextSegId() != null) {
                    adjacentSegIds.add(chunk.getNextSegId());
                }
            }
        }

        return adjacentSegIds.stream()
                .map(chunkStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 向量检索
     */
    public List<SearchResult> searchByVector(List<Float> queryVector, int topK, String filter) {
        if (milvusConnected && milvusClient != null && queryVector != null) {
            try {
                return searchFromMilvus(queryVector, topK, filter);
            } catch (Exception e) {
                logger.error("Milvus 向量检索失败: {}，降级到模拟模式", e.getMessage());
            }
        }
        
        // 降级到模拟模式
        logger.info("向量检索（模拟模式）");
        return searchFromMock(queryVector, topK);
    }

    /**
     * 从 Milvus 进行向量检索
     */
    private List<SearchResult> searchFromMilvus(List<Float> queryVector, int topK, String filter) {
        long startTime = System.currentTimeMillis();
        
        // 构建搜索参数
        SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                .withCollectionName(ragConfig.getCollectionName())
                .withVectors(Collections.singletonList(queryVector))
                .withVectorFieldName("vector")
                .withTopK(topK)
                .withMetricType(MetricType.IP)
                .withOutFields(Arrays.asList(
                        "seg_id", "doc_id", "pos", "pre_seg_id", "next_seg_id",
                        "tags", "content", "semantic_cohesion", "is_semantic_boundary", "vector"));

        // 如果有过滤条件，添加过滤
        if (filter != null && !filter.isEmpty()) {
            searchBuilder.withExpr(filter);
        }

        SearchParam searchParam = searchBuilder.build();

        // 执行搜索
        R<io.milvus.grpc.SearchResults> response = milvusClient.search(searchParam);

        if (response.getStatus() != R.Status.Success.getCode()) {
            logger.error("向量检索失败: {}", response.getMessage());
            return new ArrayList<>();
        }

        // 解析搜索结果
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResult> results = new ArrayList<>();

        // 获取搜索结果记录
        List<QueryResultsWrapper.RowRecord> records = wrapper.getRowRecords();
        logger.info("Milvus 向量检索返回 {} 条结果，耗时: {} ms", 
                records.size(), System.currentTimeMillis() - startTime);

        // 解析每个结果
        for (int i = 0; i < records.size(); i++) {
            QueryResultsWrapper.RowRecord record = records.get(i);
            SearchResult result = new SearchResult();
            
            // 获取分数
            List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);
            double score = 0.0;
            if (idScores != null && i < idScores.size()) {
                score = idScores.get(i).getScore();
            }
            result.setScore(score);
            result.setDistance(score);

            // 构建 DocumentChunk
            DocumentChunk chunk = new DocumentChunk();
            try {
                Map<String, Object> fieldValues = record.getFieldValues();
                
                chunk.setSegId(fieldValues.get("seg_id").toString());
                result.setSegId(chunk.getSegId());
                
                chunk.setDocId(fieldValues.get("doc_id").toString());
                chunk.setPos(((Number) fieldValues.get("pos")).intValue());
                
                String preSegId = fieldValues.get("pre_seg_id").toString();
                chunk.setPreSegId(preSegId.isEmpty() ? null : preSegId);
                
                String nextSegId = fieldValues.get("next_seg_id").toString();
                chunk.setNextSegId(nextSegId.isEmpty() ? null : nextSegId);
                
                String tagsStr = fieldValues.get("tags").toString();
                if (!tagsStr.isEmpty()) {
                    chunk.setTags(Arrays.asList(tagsStr.split(",")));
                }
                
                chunk.setContent(fieldValues.get("content").toString());
                chunk.setSemanticCohesion(((Number) fieldValues.get("semantic_cohesion")).doubleValue());
                chunk.setSemanticBoundary((Boolean) fieldValues.get("is_semantic_boundary"));
                
                @SuppressWarnings("unchecked")
                List<Float> vector = (List<Float>) fieldValues.get("vector");
                chunk.setVector(vector);
            } catch (Exception e) {
                logger.warn("解析向量检索结果失败: {}", e.getMessage());
            }

            result.setChunk(chunk);
            results.add(result);
        }

        return results;
    }

    /**
     * 模拟向量检索
     */
    private List<SearchResult> searchFromMock(List<Float> queryVector, int topK) {
        if (chunkStore.isEmpty() || queryVector == null) {
            return new ArrayList<>();
        }

        List<SearchResult> results = new ArrayList<>();
        List<DocumentChunk> allChunks = new ArrayList<>(chunkStore.values());
        
        // 随机返回结果（模拟）
        Collections.shuffle(allChunks);
        int count = Math.min(topK, allChunks.size());
        
        for (int i = 0; i < count; i++) {
            DocumentChunk chunk = allChunks.get(i);
            SearchResult result = new SearchResult();
            result.setSegId(chunk.getSegId());
            result.setScore(0.8 - (i * 0.05));
            result.setDistance(result.getScore());
            result.setChunk(chunk);
            results.add(result);
        }

        logger.info("向量检索（模拟模式）返回 {} 条结果", results.size());
        return results;
    }

    /**
     * 执行查询
     */
    private List<DocumentChunk> executeQuery(String filter) {
        List<String> outputFields = Arrays.asList(
                "seg_id", "doc_id", "pos", "pre_seg_id", "next_seg_id",
                "tags", "content", "semantic_cohesion", "is_semantic_boundary", "vector");

        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(ragConfig.getCollectionName())
                .withExpr(filter)
                .withOutFields(outputFields)
                .build();

        R<QueryResults> response = milvusClient.query(queryParam);

        if (response.getStatus() != R.Status.Success.getCode()) {
            logger.error("查询失败: {}", response.getMessage());
            return new ArrayList<>();
        }

        // 将 QueryResults 包装为 QueryResultsWrapper
        QueryResultsWrapper wrapper = new QueryResultsWrapper(response.getData());
        return parseQueryResults(wrapper);
    }

    /**
     * 解析查询结果
     */
    private List<DocumentChunk> parseQueryResults(QueryResultsWrapper wrapper) {
        List<DocumentChunk> chunks = new ArrayList<>();

        try {
            List<QueryResultsWrapper.RowRecord> records = wrapper.getRowRecords();
            
            for (QueryResultsWrapper.RowRecord record : records) {
                DocumentChunk chunk = new DocumentChunk();
                Map<String, Object> fieldValues = record.getFieldValues();
                
                chunk.setSegId(fieldValues.get("seg_id").toString());
                chunk.setDocId(fieldValues.get("doc_id").toString());
                chunk.setPos(((Number) fieldValues.get("pos")).intValue());
                
                String preSegId = fieldValues.get("pre_seg_id").toString();
                chunk.setPreSegId(preSegId.isEmpty() ? null : preSegId);
                
                String nextSegId = fieldValues.get("next_seg_id").toString();
                chunk.setNextSegId(nextSegId.isEmpty() ? null : nextSegId);
                
                String tagsStr = fieldValues.get("tags").toString();
                if (!tagsStr.isEmpty()) {
                    chunk.setTags(Arrays.asList(tagsStr.split(",")));
                }
                
                chunk.setContent(fieldValues.get("content").toString());
                chunk.setSemanticCohesion(
                        ((Number) fieldValues.get("semantic_cohesion")).doubleValue());
                chunk.setSemanticBoundary(
                        (Boolean) fieldValues.get("is_semantic_boundary"));
                
                @SuppressWarnings("unchecked")
                List<Float> vector = (List<Float>) fieldValues.get("vector");
                chunk.setVector(vector);

                chunks.add(chunk);
            }
        } catch (Exception e) {
            logger.error("解析查询结果失败", e);
        }

        return chunks;
    }

    /**
     * 构建 IN 过滤条件
     */
    private String buildInFilter(String fieldName, List<String> values) {
        String valuesStr = values.stream()
                .map(v -> "\"" + v + "\"")
                .collect(Collectors.joining(", "));
        return fieldName + " in [" + valuesStr + "]";
    }

    /**
     * 生成零向量
     */
    private List<Float> generateZeroVector(int dimension) {
        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add(0.0f);
        }
        return vector;
    }

    /**
     * 获取存储统计信息
     */
    public Map<String, Object> getStorageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("milvusConnected", milvusConnected);
        stats.put("milvusClient", milvusClient != null ? "已初始化" : "未初始化");
        
        if (milvusConnected && milvusClient != null) {
            stats.put("mode", "真实模式");
            stats.put("collectionName", ragConfig.getCollectionName());
        } else {
            stats.put("totalChunks", chunkStore.size());
            stats.put("totalDocuments", docIndex.size());
            stats.put("totalTags", tagIndex.size());
            stats.put("mode", "模拟模式");
        }
        
        return stats;
    }

    /**
     * 清空所有数据（用于测试）
     */
    public void clearAll() {
        chunkStore.clear();
        docIndex.clear();
        tagIndex.clear();
        logger.info("已清空所有模拟存储数据");
    }

    /**
     * 关闭连接
     */
    @PreDestroy
    public void close() {
        if (milvusClient != null) {
            try {
                milvusClient.close();
                logger.info("Milvus 连接已关闭");
            } catch (Exception e) {
                logger.error("关闭 Milvus 连接失败", e);
            }
        }
        chunkStore.clear();
        docIndex.clear();
        tagIndex.clear();
    }

    /**
     * 向量检索结果
     */
    public static class SearchResult {
        private String segId;
        private double score;
        private double distance;
        private DocumentChunk chunk;

        public String getSegId() {
            return segId;
        }

        public void setSegId(String segId) {
            this.segId = segId;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public DocumentChunk getChunk() {
            return chunk;
        }

        public void setChunk(DocumentChunk chunk) {
            this.chunk = chunk;
        }
    }
}