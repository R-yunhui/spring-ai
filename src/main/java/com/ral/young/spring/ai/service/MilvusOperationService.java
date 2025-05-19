package com.ral.young.spring.ai.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.DescribeIndexResponse;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.GetIndexStateResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.grpc.ShowCollectionsResponse;
import io.milvus.grpc.ShowPartitionsResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DescribeCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.collection.ShowCollectionsParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.UpsertParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.param.index.DropIndexParam;
import io.milvus.param.index.GetIndexStateParam;
import io.milvus.param.partition.CreatePartitionParam;
import io.milvus.param.partition.DropPartitionParam;
import io.milvus.param.partition.HasPartitionParam;
import io.milvus.param.partition.LoadPartitionsParam;
import io.milvus.param.partition.ReleasePartitionsParam;
import io.milvus.param.partition.ShowPartitionsParam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * Milvus 数据库操作服务类
 * <p>
 * 封装了 Milvus 向量数据库的常用操作，包括连接、集合管理、分区管理、索引管理、数据插入、删除、更新、查询和搜索等。
 * </p>
 *
 * @author Gemini
 * @since 2024-07-29
 */
@Slf4j
@Service
public class MilvusOperationService {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    private MilvusServiceClient milvusServiceClient;

    private static final Random RANDOM = new Random();

    /**
     * 初始化 Milvus 服务连接
     * <p>
     * 在 Bean 实例化后执行，建立与 Milvus 服务器的连接。
     * </p>
     */
    // @PostConstruct
    public void init() {
        try {
            log.info("开始连接 Milvus 服务，主机：{}，端口：{}", host, port);
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();
            milvusServiceClient = new MilvusServiceClient(connectParam);
            log.info("成功连接 Milvus 服务。");
        } catch (Exception e) {
            log.error("连接 Milvus 服务失败：{}", e.getMessage(), e);
            throw new RuntimeException("Milvus 服务连接失败", e);
        }
    }

    /**
     * 关闭 Milvus 服务连接
     * <p>
     * 在 Bean 销毁前执行，释放与 Milvus 服务器的连接资源。
     * </p>
     */
    @PreDestroy
    public void destroy() {
        if (milvusServiceClient != null) {
            try {
                log.info("开始关闭 Milvus 服务连接...");
                milvusServiceClient.close();
                log.info("成功关闭 Milvus 服务连接。");
            } catch (Exception e) { // Milvus close() 方法声明了 InterruptedException
                log.error("关闭 Milvus 服务连接时发生异常：", e);
			}
        }
    }

    // --- 集合管理 (Collection Management) ---

    /**
     * 检查指定的集合是否存在。
     *
     * @param collectionName 要检查的集合名称
     * @return 如果集合存在则返回 {@code true}，否则返回 {@code false}
     */
    public boolean hasCollection(String collectionName) {
        log.debug("检查集合是否存在：{}", collectionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法检查集合 {}。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<Boolean> response = milvusServiceClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("检查集合 {} 是否存在失败: {}", collectionName, response.getMessage());
                return false;
            }
            boolean exists = response.getData();
            log.info("集合 {} 是否存在：{}", collectionName, exists);
            return exists;
        } catch (Exception e) {
            log.error("检查集合 {} 是否存在时出错: {}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建集合。
     *
     * @param collectionName  集合名称
     * @param description     集合描述
     * @param primaryFieldName 主键字段名称
     * @param vectorFieldName 向量字段名称
     * @param dimension       向量维度
     * @param otherFields     其他标量字段定义 (可选)
     * @return 如果创建成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean createCollection(String collectionName, String description,
                                    String primaryFieldName, String vectorFieldName, int dimension,
                                    List<FieldType> otherFields) {
        log.info("准备创建集合：{}，描述：{}，主键字段：{}，向量字段：{}，维度：{}",
                collectionName, description, primaryFieldName, vectorFieldName, dimension);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法创建集合 {}。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }

        FieldType primaryField = FieldType.newBuilder()
                .withName(primaryFieldName)
                .withDataType(DataType.Int64) // 通常主键使用 Int64, 根据实际情况调整
                .withPrimaryKey(true)
                .withAutoID(false) // 根据需求设置是否自动生成 ID
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName(vectorFieldName)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        CreateCollectionParam.Builder createCollectionParamBuilder = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription(description)
                .withShardsNum(2) // 分片数量，根据实际需求调整
                .addFieldType(primaryField)
                .addFieldType(vectorField);

        if (otherFields != null && !otherFields.isEmpty()) {
            otherFields.forEach(createCollectionParamBuilder::addFieldType);
        }

        try {
            R<RpcStatus> response = milvusServiceClient.createCollection(createCollectionParamBuilder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("创建集合 {} 失败: {}", collectionName, response.getMessage());
                return false;
            }
            log.info("集合 {} 创建成功。", collectionName);
            return true;
        } catch (Exception e) {
            log.error("创建集合 {} 失败：{}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取指定集合的描述信息（包括 Schema）。
     *
     * @param collectionName 集合名称
     * @return 集合的描述信息 {@link DescribeCollectionResponse}，失败则返回 null
     */
    public DescribeCollectionResponse describeCollection(String collectionName) {
        log.debug("获取集合 {} 的描述信息...", collectionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法获取集合 {} 的描述信息。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<DescribeCollectionResponse> response = milvusServiceClient.describeCollection(
                    DescribeCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("获取集合 {} 描述信息失败: {}", collectionName, response.getMessage());
                return null;
            }
            log.info("成功获取集合 {} 的描述信息。", collectionName);
            return response.getData();
        } catch (Exception e) {
            log.error("获取集合 {} 描述信息时出错: {}", collectionName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取指定集合的统计信息（例如行数）。
     *
     * @param collectionName 集合名称
     * @return 集合的统计信息 {@link GetCollectionStatisticsResponse}，失败则返回 null
     */
    public GetCollectionStatisticsResponse getCollectionStatistics(String collectionName) {
        log.debug("获取集合 {} 的统计信息...", collectionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法获取集合 {} 的统计信息。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            // 需要先加载集合才能获取统计信息
            if (!loadCollection(collectionName)) {
                log.warn("获取统计信息前，加载集合 {} 失败，统计结果可能不准确或失败。", collectionName);
            }
            R<GetCollectionStatisticsResponse> response = milvusServiceClient.getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("获取集合 {} 统计信息失败: {}", collectionName, response.getMessage());
                return null;
            }
            log.info("成功获取集合 {} 的统计信息。", collectionName);
            return response.getData();
        } catch (Exception e) {
            log.error("获取集合 {} 统计信息时出错: {}", collectionName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 列出 Milvus 实例中的所有集合。
     *
     * @return 包含所有集合名称的列表 {@link ShowCollectionsResponse}，失败则返回 null
     */
    public ShowCollectionsResponse showCollections() {
        log.debug("列出所有集合...");
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法列出所有集合。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<ShowCollectionsResponse> response = milvusServiceClient.showCollections(
                    ShowCollectionsParam.newBuilder().build() // 默认展示所有
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("列出所有集合失败: {}", response.getMessage());
                return null;
            }
            log.info("成功列出所有集合，数量：{}", response.getData().getCollectionNamesCount());
            return response.getData();
        } catch (Exception e) {
            log.error("列出所有集合时出错: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 加载集合到内存以进行搜索。
     *
     * @param collectionName 集合名称
     * @return 如果加载成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean loadCollection(String collectionName) {
        log.info("准备加载集合 {} 到内存...", collectionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法加载集合 {}。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<RpcStatus> response = milvusServiceClient.loadCollection(
                    LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("加载集合 {} 失败: {}", collectionName, response.getMessage());
                return false;
            }
            log.info("集合 {} 加载请求已提交。通常加载是异步的，请稍后检查状态。", collectionName);
            return true;
        } catch (Exception e) {
            log.error("加载集合 {} 失败：{}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从内存中释放集合。
     *
     * @param collectionName 集合名称
     * @return 如果释放成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean releaseCollection(String collectionName) {
        log.info("准备从内存中释放集合 {}...", collectionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法释放集合 {}。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<RpcStatus> response = milvusServiceClient.releaseCollection(
                    ReleaseCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("释放集合 {} 失败: {}", collectionName, response.getMessage());
                return false;
            }
            log.info("集合 {} 释放成功。", collectionName);
            return true;
        } catch (Exception e) {
            log.error("释放集合 {} 失败：{}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除指定的集合。
     * <p>
     * 警告：此操作将永久删除集合及其所有数据，请谨慎使用。
     * </p>
     *
     * @param collectionName 要删除的集合名称
     * @return 如果删除成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean dropCollection(String collectionName) {
        log.warn("警告：即将删除集合 {} 及其所有数据！", collectionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法删除集合 {}。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }

        try {
            R<RpcStatus> response = milvusServiceClient.dropCollection(
                    DropCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("删除集合 {} 失败: {}", collectionName, response.getMessage());
                return false;
            }
            log.info("集合 {} 已成功删除。", collectionName);
            return true;
        } catch (Exception e) {
            log.error("删除集合 {} 失败：{}", collectionName, e.getMessage(), e);
            return false;
        }
    }

    // --- 分区管理 (Partition Management) ---

    /**
     * 在指定集合中创建分区。
     *
     * @param collectionName 集合名称
     * @param partitionName  要创建的分区名称
     * @return 如果创建成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean createPartition(String collectionName, String partitionName) {
        log.info("准备在集合 {} 中创建分区 {}...", collectionName, partitionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法在集合 {} 中创建分区 {}。", collectionName, partitionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<RpcStatus> response = milvusServiceClient.createPartition(
                    CreatePartitionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withPartitionName(partitionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("在集合 {} 中创建分区 {} 失败: {}", collectionName, partitionName, response.getMessage());
                return false;
            }
            log.info("在集合 {} 中成功创建分区 {}。", collectionName, partitionName);
            return true;
        } catch (Exception e) {
            log.error("在集合 {} 中创建分区 {} 时出错: {}", collectionName, partitionName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查指定集合中的分区是否存在。
     *
     * @param collectionName 集合名称
     * @param partitionName  要检查的分区名称
     * @return 如果分区存在返回 {@code true}，否则返回 {@code false}
     */
    public boolean hasPartition(String collectionName, String partitionName) {
        log.debug("检查集合 {} 中是否存在分区 {}...", collectionName, partitionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法检查分区。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<Boolean> response = milvusServiceClient.hasPartition(
                    HasPartitionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withPartitionName(partitionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("检查集合 {} 中分区 {} 是否存在失败: {}", collectionName, partitionName, response.getMessage());
                return false;
            }
            boolean exists = response.getData();
            log.info("集合 {} 中分区 {} 是否存在: {}", collectionName, partitionName, exists);
            return exists;
        } catch (Exception e) {
            log.error("检查集合 {} 中分区 {} 是否存在时出错: {}", collectionName, partitionName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 列出指定集合中的所有分区。
     *
     * @param collectionName 集合名称
     * @return 包含所有分区名称的列表 {@link ShowPartitionsResponse}，失败则返回 null
     */
    public ShowPartitionsResponse showPartitions(String collectionName) {
        log.debug("列出集合 {} 中的所有分区...", collectionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法列出分区。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<ShowPartitionsResponse> response = milvusServiceClient.showPartitions(
                    ShowPartitionsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("列出集合 {} 中的分区失败: {}", collectionName, response.getMessage());
                return null;
            }
            log.info("成功列出集合 {} 中的分区，数量：{}", collectionName, response.getData().getPartitionNamesCount());
            return response.getData();
        } catch (Exception e) {
            log.error("列出集合 {} 中的分区时出错: {}", collectionName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 加载指定集合的若干分区到内存。
     *
     * @param collectionName 集合名称
     * @param partitionNames 要加载的分区名称列表
     * @return 如果加载请求提交成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean loadPartitions(String collectionName, List<String> partitionNames) {
        log.info("准备加载集合 {} 的分区 {} 到内存...", collectionName, partitionNames);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法加载分区。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        if (partitionNames == null || partitionNames.isEmpty()) {
            log.warn("分区列表为空，不执行加载操作。");
            return false;
        }
        try {
            R<RpcStatus> response = milvusServiceClient.loadPartitions(
                    LoadPartitionsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withPartitionNames(partitionNames)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("加载集合 {} 的分区 {} 失败: {}", collectionName, partitionNames, response.getMessage());
                return false;
            }
            log.info("集合 {} 的分区 {} 加载请求已提交。", collectionName, partitionNames);
            return true;
        } catch (Exception e) {
            log.error("加载集合 {} 的分区 {} 时出错: {}", collectionName, partitionNames, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从内存中释放指定集合的若干分区。
     *
     * @param collectionName 集合名称
     * @param partitionNames 要释放的分区名称列表
     * @return 如果释放成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean releasePartitions(String collectionName, List<String> partitionNames) {
        log.info("准备从内存中释放集合 {} 的分区 {}...", collectionName, partitionNames);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法释放分区。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        if (partitionNames == null || partitionNames.isEmpty()) {
            log.warn("分区列表为空，不执行释放操作。");
            return false;
        }
        try {
            R<RpcStatus> response = milvusServiceClient.releasePartitions(
                    ReleasePartitionsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withPartitionNames(partitionNames)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("释放集合 {} 的分区 {} 失败: {}", collectionName, partitionNames, response.getMessage());
                return false;
            }
            log.info("集合 {} 的分区 {} 释放成功。", collectionName, partitionNames);
            return true;
        } catch (Exception e) {
            log.error("释放集合 {} 的分区 {} 时出错: {}", collectionName, partitionNames, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除指定集合中的分区。
     * <p>
     * 警告：此操作将永久删除分区及其所有数据，请谨慎使用。
     * </p>
     *
     * @param collectionName 集合名称
     * @param partitionName  要删除的分区名称
     * @return 如果删除成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean dropPartition(String collectionName, String partitionName) {
        log.warn("警告：即将删除集合 {} 中的分区 {} 及其所有数据！", collectionName, partitionName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法删除分区。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            R<RpcStatus> response = milvusServiceClient.dropPartition(
                    DropPartitionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withPartitionName(partitionName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("删除集合 {} 中的分区 {} 失败: {}", collectionName, partitionName, response.getMessage());
                return false;
            }
            log.info("集合 {} 中的分区 {} 已成功删除。", collectionName, partitionName);
            return true;
        } catch (Exception e) {
            log.error("删除集合 {} 中的分区 {} 时出错: {}", collectionName, partitionName, e.getMessage(), e);
            return false;
        }
    }


    // --- 索引管理 (Index Management) ---

    /**
     * 为集合的向量字段创建索引。
     *
     * @param collectionName 集合名称
     * @param fieldName      向量字段名称
     * @param indexName      索引名称 (可选, Milvus 2.x 通常不需要显式指定索引名，会自动生成)
     * @param indexType      索引类型 (例如：IVF_FLAT, HNSW)
     * @param metricType     度量类型 (例如：L2, IP)
     * @param extraParams    索引的额外参数 (例如：{"nlist":128} for IVF_FLAT, {"M":16, "efConstruction":200} for HNSW)
     * @return 如果创建请求提交成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean createIndex(String collectionName, String fieldName, String indexName,
                               IndexType indexType, MetricType metricType, String extraParams) {
        log.info("准备为集合 {} 的字段 {} 创建索引 (名称: {}), 类型: {}, 度量: {}, 参数: {}",
                collectionName, fieldName, indexName == null ? "auto" : indexName, indexType, metricType, extraParams);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法创建索引。");
            throw new IllegalStateException("Milvus 服务未连接");
        }

        CreateIndexParam.Builder builder = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(fieldName)
                .withIndexType(indexType)
                .withMetricType(metricType)
                .withExtraParam(extraParams)
                .withSyncMode(Boolean.FALSE); // 推荐异步创建索引

        if (indexName != null && !indexName.isEmpty()) {
            builder.withIndexName(indexName);
        }

        try {
            R<RpcStatus> response = milvusServiceClient.createIndex(builder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("为集合 {} 的字段 {} 创建索引失败: {}", collectionName, fieldName, response.getMessage());
                return false;
            }
            log.info("集合 {} 的字段 {} 索引创建请求已提交。索引名称：{}", collectionName, fieldName, indexName == null ? "(自动生成)" : indexName);
            return true;
        } catch (Exception e) {
            log.error("为集合 {} 的字段 {} 创建索引时出错: {}", collectionName, fieldName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取指定集合上索引的描述信息。
     *
     * @param collectionName 集合名称
     * @param indexName      索引名称 (如果创建时未指定，通常是字段名或自动生成的名称)
     * @return 索引的描述信息 {@link DescribeIndexResponse}，失败则返回 null
     */
    public DescribeIndexResponse describeIndex(String collectionName, String indexName) {
        log.debug("获取集合 {} 上索引 {} 的描述信息...", collectionName, indexName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法获取索引信息。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            // Milvus 2.x describeIndex 通常需要字段名，indexName 参数在某些版本中可能不直接用于查询
            // 通常一个字段上只有一个索引。如果 indexName 是字段名，这样使用是正确的。
            // 如果 indexName 是用户自定义的索引名，需要确认SDK版本和用法。
            // 为简单起见，这里假设 indexName 是要描述索引的字段名
            R<DescribeIndexResponse> response = milvusServiceClient.describeIndex(
                    DescribeIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withIndexName(indexName) // 或者 .withFieldName(indexName) 取决于具体场景和SDK版本
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("获取集合 {} 上索引 {} 描述信息失败: {}", collectionName, indexName, response.getMessage());
                return null;
            }
            log.info("成功获取集合 {} 上索引 {} 的描述信息。", collectionName, indexName);
            return response.getData();
        } catch (Exception e) {
            log.error("获取集合 {} 上索引 {} 描述信息时出错: {}", collectionName, indexName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取指定集合上索引的构建状态。
     *
     * @param collectionName 集合名称
     * @param indexName      索引名称 (通常是向量字段的名称，因为一个字段一般只有一个索引)
     * @return 索引状态信息 {@link GetIndexStateResponse}，失败则返回 null
     */
    public GetIndexStateResponse getIndexState(String collectionName, String indexName) {
        log.debug("获取集合 {} 上字段/索引 {} 的构建状态...", collectionName, indexName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法获取索引状态。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            // GetIndexStateParam 通常需要字段名
            R<GetIndexStateResponse> response = milvusServiceClient.getIndexState(
                GetIndexStateParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withIndexName(indexName) // 如果是字段上的索引，这里可以传入字段名
                    .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("获取集合 {} 上索引 {} 的构建状态失败: {}", collectionName, indexName, response.getMessage());
                return null;
            }
            log.info("成功获取集合 {} 上索引 {} 的构建状态: {}", collectionName, indexName, response.getData().getState());
            return response.getData();
        } catch (Exception e) {
            log.error("获取集合 {} 上索引 {} 构建状态时出错: {}", collectionName, indexName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 等待指定集合上的索引构建完成。
     *
     * @param collectionName 集合名称
     * @param indexName      索引名称 (通常是向量字段的名称)
     * @param timeoutInSeconds 超时时间（秒）
     * @param pollingIntervalInMillis 轮询间隔（毫秒）
     * @return 如果索引在超时时间内成功构建则返回 {@code true}，否则返回 {@code false}
     */
    public boolean waitForIndexReady(String collectionName, String indexName,
                                     long timeoutInSeconds, long pollingIntervalInMillis) {
        log.info("等待集合 {} 上的索引 {} 构建完成... 超时时间: {} 秒，轮询间隔: {} 毫秒",
                collectionName, indexName, timeoutInSeconds, pollingIntervalInMillis);
        long startTime = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(timeoutInSeconds)) {
                GetIndexStateResponse stateResponse = getIndexState(collectionName, indexName);
                if (stateResponse != null && stateResponse.getState() == io.milvus.grpc.IndexState.Finished) {
                    log.info("索引 {} 在集合 {} 上已成功构建。", indexName, collectionName);
                    return true;
                } else if (stateResponse != null && stateResponse.getState() == io.milvus.grpc.IndexState.Failed) {
                    log.error("索引 {} 在集合 {} 上构建失败。", indexName, collectionName);
                    return false;
                }
                log.debug("索引 {} 在集合 {} 上仍在构建中 (状态: {}), 等待 {} 毫秒后重试...",
                        indexName, collectionName, stateResponse != null ? stateResponse.getState() : "未知", pollingIntervalInMillis);
                Thread.sleep(pollingIntervalInMillis);
            }
            log.warn("等待索引 {} 在集合 {} 上构建超时 (超过 {} 秒)。", indexName, collectionName, timeoutInSeconds);
            return false;
        } catch (InterruptedException e) {
            log.error("等待索引 {} 在集合 {} 上构建被中断: {}", indexName, collectionName, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.error("等待索引 {} 在集合 {} 上构建时发生未知错误: {}", indexName, collectionName, e.getMessage(), e);
            return false;
        }
    }


    /**
     * 删除指定集合上的索引。
     *
     * @param collectionName 集合名称
     * @param indexName      要删除的索引名称 (通常是向量字段的名称)
     * @return 如果删除成功返回 {@code true}，否则返回 {@code false}
     */
    public boolean dropIndex(String collectionName, String indexName) {
        log.warn("警告：即将删除集合 {} 上的索引 {}！", collectionName, indexName);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法删除索引。");
            throw new IllegalStateException("Milvus 服务未连接");
        }
        try {
            // DropIndexParam 通常需要字段名，因为索引是建在字段上的
            R<RpcStatus> response = milvusServiceClient.dropIndex(
                    DropIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withIndexName(indexName) // 或者 .withFieldName(indexName)
                            .build()
            );
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("删除集合 {} 上的索引 {} 失败: {}", collectionName, indexName, response.getMessage());
                return false;
            }
            log.info("集合 {} 上的索引 {} 已成功删除。", collectionName, indexName);
            return true;
        } catch (Exception e) {
            log.error("删除集合 {} 上的索引 {} 时出错: {}", collectionName, indexName, e.getMessage(), e);
            return false;
        }
    }

    // --- 数据操作 (Data Manipulation Language - DML) ---

    /**
     * 向指定集合插入数据。
     * <p>
     * 注意：所有字段的列表长度必须一致。
     * </p>
     *
     * @param collectionName 目标集合名称
     * @param partitionName  目标分区名称 (可选, 如果不指定则插入到默认分区 `_default`)
     * @param fields         字段数据列表，每个 {@link InsertParam.Field} 包含字段名和对应的值列表
     * @return 插入操作的结果 {@link MutationResult}，失败则返回 null
     */
    public MutationResult insert(String collectionName, String partitionName, List<InsertParam.Field> fields) {
        log.debug("准备向集合 {} (分区: {}) 插入数据，字段数量：{}", collectionName, partitionName == null ? "_default" : partitionName, fields.size());
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法向集合 {} 插入数据。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }

        InsertParam.Builder insertParamBuilder = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields);

        if (partitionName != null && !partitionName.isEmpty()) {
            insertParamBuilder.withPartitionName(partitionName);
        }

        try {
            R<MutationResult> response = milvusServiceClient.insert(insertParamBuilder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("向集合 {} 插入数据失败: {}", collectionName, response.getMessage());
                return null;
            }
            MutationResult result = response.getData();
            log.info("向集合 {} 插入数据成功，影响行数：{}, 主键类型: {}", collectionName, result.getInsertCnt(), result.getIDs().getIdFieldCase());
            if (result.getSuccIndexCount() > 0) {
                 // 根据主键类型打印ID
                if (result.getIDs().hasIntId()) {
                    log.debug("成功插入的 Int64 ID 列表 (部分): {}", result.getIDs().getIntId().getDataList().stream().limit(10).collect(Collectors.toList()));
                } else if (result.getIDs().hasStrId()) {
                     log.debug("成功插入的 String ID 列表 (部分): {}", result.getIDs().getStrId().getDataList().stream().limit(10).collect(Collectors.toList()));
                }
            }
            if (result.getErrIndexCount() > 0) {
                log.warn("插入数据时部分失败，失败的索引位置: {}", result.getErrIndexList());
            }
            return result;
        } catch (Exception e) {
            log.error("向集合 {} 插入数据失败：{}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Milvus 数据插入失败", e);
        }
    }

    /**
     * 向指定集合执行 Upsert 操作 (如果主键已存在则更新，否则插入)。
     * <p>
     * 注意：所有字段的列表长度必须一致。主键字段必须包含在 fields 中。
     * </p>
     *
     * @param collectionName 目标集合名称
     * @param partitionName  目标分区名称 (可选)
     * @param fields         字段数据列表，包含主键字段和要插入/更新的向量及标量字段
     * @return Upsert 操作的结果 {@link MutationResult}，失败则返回 null
     */
    public MutationResult upsert(String collectionName, String partitionName, List<InsertParam.Field> fields) {
        log.debug("准备向集合 {} (分区: {}) 执行 Upsert 操作，字段数量：{}", collectionName, partitionName == null ? "_default" : partitionName, fields.size());
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法向集合 {} 执行 Upsert 操作。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }

        UpsertParam.Builder upsertParamBuilder = UpsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields); // SDK 将 InsertParam.Field 复用于 Upsert

        if (partitionName != null && !partitionName.isEmpty()) {
            upsertParamBuilder.withPartitionName(partitionName);
        }

        try {
            R<MutationResult> response = milvusServiceClient.upsert(upsertParamBuilder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("向集合 {} 执行 Upsert 操作失败: {}", collectionName, response.getMessage());
                return null;
            }
            MutationResult result = response.getData();
            log.info("向集合 {} Upsert 数据成功，影响行数 (Insert+Update)：{}, 主键类型: {}", collectionName, result.getUpsertCnt(), result.getIDs().getIdFieldCase());
             if (result.getSuccIndexCount() > 0) { // Upsert 可能返回成功处理的ID
                if (result.getIDs().hasIntId()) {
                    log.debug("成功 Upsert 的 Int64 ID 列表 (部分): {}", result.getIDs().getIntId().getDataList().stream().limit(10).collect(Collectors.toList()));
                } else if (result.getIDs().hasStrId()) {
                     log.debug("成功 Upsert 的 String ID 列表 (部分): {}", result.getIDs().getStrId().getDataList().stream().limit(10).collect(Collectors.toList()));
                }
            }
            if (result.getErrIndexCount() > 0) {
                log.warn("Upsert 数据时部分失败，失败的索引位置: {}", result.getErrIndexList());
            }
            return result;
        } catch (Exception e) {
            log.error("向集合 {} 执行 Upsert 操作失败：{}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Milvus 数据 Upsert 失败", e);
        }
    }


    /**
     * 根据表达式删除数据。
     *
     * @param collectionName 集合名称
     * @param expression     删除表达式，例如 "id_field in [123, 456]" 或 "age > 30"。
     *                       表达式的语法请参考 Milvus官方文档。
     * @return 删除操作的结果 {@link MutationResult}，失败则返回 null
     */
    public MutationResult delete(String collectionName, String expression) {
        log.info("准备从集合 {} 中删除数据，表达式：'{}'", collectionName, expression);
        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法从集合 {} 删除数据。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }

        if (expression == null || expression.trim().isEmpty()) {
            log.warn("删除操作未提供有效的删除表达式，操作将不会执行。");
            throw new IllegalArgumentException("必须提供有效的删除表达式以执行删除操作。");
        }

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expression)
                .build();

        try {
            R<MutationResult> response = milvusServiceClient.delete(deleteParam);
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("从集合 {} 删除数据失败 (表达式: '{}'): {}", collectionName, expression, response.getMessage());
                return null;
            }
            MutationResult result = response.getData();
            log.info("从集合 {} 删除数据成功，影响行数：{}", collectionName, result.getDeleteCnt());
            return result;
        } catch (Exception e) {
            log.error("从集合 {} 删除数据失败，表达式：'{}'，错误：{}", collectionName, expression, e.getMessage(), e);
            throw new RuntimeException("Milvus 数据删除失败", e);
        }
    }

    /**
     * 根据主键 ID 列表删除数据 (便利方法)。
     *
     * @param collectionName    集合名称
     * @param primaryKeyFieldName 主键字段的名称
     * @param primaryKeyValues  主键 ID 列表 (类型应与主键字段定义一致，通常为 Long 或 String)
     * @return 删除操作的结果 {@link MutationResult}，失败则返回 null
     */
    public MutationResult deleteByIds(String collectionName, String primaryKeyFieldName, List<?> primaryKeyValues) {
        if (primaryKeyValues == null || primaryKeyValues.isEmpty()) {
            log.warn("主键列表为空，不执行删除操作。集合：{}", collectionName);
            return null; // 或者可以返回一个表示无操作的 MutationResult
        }
        if (primaryKeyFieldName == null || primaryKeyFieldName.trim().isEmpty()) {
            log.error("主键字段名未提供，无法通过ID列表删除。");
            throw new IllegalArgumentException("主键字段名不能为空。");
        }

        String expression;
        if (primaryKeyValues.get(0) instanceof String) {
            expression = String.format("%s in [%s]",
                    primaryKeyFieldName,
                    primaryKeyValues.stream().map(id -> "'" + id.toString().replace("'", "''") + "'").collect(Collectors.joining(",")));
        } else if (primaryKeyValues.get(0) instanceof Long || primaryKeyValues.get(0) instanceof Integer) {
            expression = String.format("%s in [%s]",
                    primaryKeyFieldName,
                    primaryKeyValues.stream().map(String::valueOf).collect(Collectors.joining(",")));
        } else {
            log.error("不支持的主键类型：{}。目前仅支持 String, Long, Integer。", primaryKeyValues.get(0).getClass().getName());
            throw new IllegalArgumentException("不支持的主键类型，请使用 String 或 Long/Integer。");
        }
        
        log.info("准备通过主键列表从集合 {} 中删除数据。主键字段：{}，主键数量：{}，生成的表达式：'{}'",
                collectionName, primaryKeyFieldName, primaryKeyValues.size(), expression);
        return delete(collectionName, expression);
    }


    /**
     * 根据标量字段表达式查询数据 (非向量搜索)。
     *
     * @param collectionName    集合名称
     * @param outputFields      需要返回的标量字段和向量字段名称列表 (可选, null 或空列表表示返回所有字段，包括主键和向量)
     * @param filterExpression  查询的过滤表达式 (例如 "age > 18 AND city == 'New York'")
     * @param partitionNames    查询的分区名称列表 (可选, null 或空列表表示查询所有分区)
     * @param consistencyLevel  一致性级别 (可选, 默认为 SDK 默认值)
     * @param limit             返回结果的最大数量 (可选, Milvus query 有其自身的限制)
     * @return 查询结果 {@link QueryResults}，失败则返回 null
     */
    public QueryResults query(String collectionName, List<String> outputFields, String filterExpression,
                              List<String> partitionNames, ConsistencyLevelEnum consistencyLevel, Long limit) {
        log.debug("准备在集合 {} 中执行查询，过滤表达式：'{}'，输出字段：{}，分区：{}",
                collectionName, filterExpression, outputFields, partitionNames);

        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法在集合 {} 中执行查询。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            log.error("查询表达式不能为空。");
            throw new IllegalArgumentException("查询表达式 (filterExpression) 不能为空。");
        }

        QueryParam.Builder queryParamBuilder = QueryParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(filterExpression);

        if (outputFields != null && !outputFields.isEmpty()) {
            queryParamBuilder.withOutFields(outputFields);
        }
        if (partitionNames != null && !partitionNames.isEmpty()) {
            queryParamBuilder.withPartitionNames(partitionNames);
        }
        if (consistencyLevel != null) {
            queryParamBuilder.withConsistencyLevel(consistencyLevel);
        }
        if (limit != null && limit > 0) {
            // 注意: Milvus 的 query 通常有自己的 limit/offset 机制，`withLimit` 是新版SDK才有的参数
            // 老版本可能需要在 expression 中使用 "limit x offset y"
            // 假设使用的SDK版本支持 withLimit
            queryParamBuilder.withLimit(limit);
        }


        try {
            R<QueryResults> response = milvusServiceClient.query(queryParamBuilder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("在集合 {} 中查询失败 (表达式: '{}'): {}", collectionName, filterExpression, response.getMessage());
                return null;
            }
            QueryResults results = response.getData();
            log.info("在集合 {} 中查询完成，返回行数：{}", collectionName, results.getFieldsDataCount() > 0 ? results.getFieldsData(0).getScalars().getDataCase().getNumber() : 0); // 估算行数
            // 详细结果处理和日志记录可以根据需要添加
            // results.getFieldsDataList() 包含输出字段的数据
            return results;
        } catch (Exception e) {
            log.error("在集合 {} 中查询失败 (表达式: '{}'): {}", collectionName, filterExpression, e.getMessage(), e);
            throw new RuntimeException("Milvus 查询失败", e);
        }
    }


    /**
     * 在指定集合中进行向量相似性搜索。
     *
     * @param collectionName    集合名称
     * @param vectorFieldName   向量字段名称
     * @param searchVectors     用于搜索的向量列表 (每个内部 List<Float> 是一个向量)
     * @param topK              返回最相似的结果数量
     * @param searchParamsJson  搜索参数的 JSON 字符串 (例如：HNSW 的 "{"ef": 64}", IVF_FLAT 的 "{"nprobe": 10}")
     * @param outputFields      需要返回的标量字段名称列表 (可选)
     * @param filterExpression  搜索结果的过滤表达式 (可选, 例如 "age > 18")
     * @param consistencyLevel  一致性级别 (可选, 默认为 SDK 或服务端的配置)
     * @return 搜索结果 {@link SearchResults}，失败则返回 null
     */
    public SearchResults search(String collectionName, String vectorFieldName,
                                List<List<Float>> searchVectors, int topK,
                                String searchParamsJson, List<String> outputFields, 
                                String filterExpression, ConsistencyLevelEnum consistencyLevel) {
        log.debug("准备在集合 {} 中搜索，向量字段：{}，搜索向量数量：{}，topK：{}，参数：{}，输出字段：{}，过滤表达式：'{}', 一致性: {}",
                collectionName, vectorFieldName, searchVectors.size(), topK, searchParamsJson, outputFields, filterExpression, consistencyLevel);

        if (milvusServiceClient == null) {
            log.error("Milvus 服务未连接，无法在集合 {} 中执行搜索。", collectionName);
            throw new IllegalStateException("Milvus 服务未连接");
        }
        if (searchVectors == null || searchVectors.isEmpty()) {
            log.warn("搜索向量列表为空，不执行搜索操作。");
            return null;
        }


        SearchParam.Builder searchParamBuilder = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.L2) // 默认 L2, 应与索引一致或根据需求选择 IP, HAMMING, TANIMOTO 等
                .withTopK(topK)
                .withVectors(searchVectors)
                .withVectorFieldName(vectorFieldName)
                .withParams(searchParamsJson);

        if (outputFields != null && !outputFields.isEmpty()) {
            searchParamBuilder.withOutFields(outputFields);
        }

        if (filterExpression != null && !filterExpression.trim().isEmpty()) {
            searchParamBuilder.withExpr(filterExpression);
        }
        
        if (consistencyLevel != null) {
            searchParamBuilder.withConsistencyLevel(consistencyLevel);
        } else {
            // 默认使用会话一致性或由服务端决定
            searchParamBuilder.withConsistencyLevel(io.milvus.common.clientenum.ConsistencyLevelEnum.SESSION);
        }


        try {
            R<SearchResults> response = milvusServiceClient.search(searchParamBuilder.build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                 log.error("在集合 {} 中搜索失败: {}", collectionName, response.getMessage());
                return null;
            }
            SearchResults results = response.getData();
            // SearchResults.getResults().getScoresCount() 在某些SDK版本中可能不存在或行为不同
            // 一种更通用的计算方式是基于 Topks
            long totalResults = 0;
            if (results.getResults() != null && results.getResults().getTopksList() != null) {
                totalResults = results.getResults().getTopksList().stream().mapToLong(val -> val).sum(); // topks 是每个查询向量返回的数量
            } else if (results.getResults() != null && results.getResults().getScoresCount() > 0) { // 兼容旧版或不同结构
                totalResults = results.getResults().getScoresCount();
            }
            
            log.info("在集合 {} 中搜索完成，返回结果数量：{}", collectionName, totalResults);
            return results;
        } catch (Exception e) {
            log.error("在集合 {} 中搜索失败：{}", collectionName, e.getMessage(), e);
            throw new RuntimeException("Milvus 搜索失败", e);
        }
    }

    // --- 辅助方法 ---

    /**
     * 生成一个指定维度的随机浮点型向量。
     *
     * @param dimension 向量维度
     * @return 随机向量列表
     */
    public static List<Float> generateRandomVector(int dimension) {
        if (dimension <= 0) {
            return Collections.emptyList();
        }
        return DoubleStream.generate(() -> RANDOM.nextDouble(-1.0, 1.0))
                .limit(dimension)
                .mapToObj(d -> (float) d)
                .collect(Collectors.toList());
    }
} 