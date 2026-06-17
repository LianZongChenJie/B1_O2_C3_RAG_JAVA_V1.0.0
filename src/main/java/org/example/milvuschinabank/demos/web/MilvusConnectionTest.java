package org.example.milvuschinabank.demos.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;

@Component
public class MilvusConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(MilvusConnectionTest.class);

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private int port;

    @Value("${milvus.username}")
    private String username;

    @Value("${milvus.password}")
    private String password;

    @Value("${milvus.use-authentication}")
    private boolean useAuthentication;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MilvusConnectionTest.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(args);
        
        MilvusConnectionTest test = context.getBean(MilvusConnectionTest.class);
        test.runTest();
        
        context.close();
    }

    public void runTest() {
        MilvusClient milvusClient = null;
        try {
            logger.info("========== 开始连接测试 ==========");
            logger.info("目标地址: {}:{}", host, port);
            logger.info("用户名: {}", username);
            logger.info("认证启用: {}", useAuthentication);

            ConnectParam.Builder builder = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port);

            if (useAuthentication) {
                builder.withAuthorization(username, password);
            }

            ConnectParam connectParam = builder.build();

            logger.info("正在初始化 MilvusServiceClient...");
            milvusClient = new MilvusServiceClient(connectParam);
            logger.info("✓ MilvusServiceClient 创建成功");

            logger.info("正在发送测试请求 (HasCollection)...");

            R<Boolean> response = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName("test_connection_check")
                            .build()
            );

            if (response.getStatus() == R.Status.Success.getCode()) {
                logger.info("==========================================");
                logger.info("✓✓✓ 连接成功！✓✓✓");
                logger.info("数据库已就绪，认证通过。");
                logger.info("==========================================");
            } else {
                logger.error("==========================================");
                logger.error("✗ 连接失败！");
                logger.error("错误代码: {}", response.getStatus());
                logger.error("错误信息: {}", response.getMessage());
                logger.error("==========================================");
            }

        } catch (Exception e) {
            logger.error("==========================================");
            logger.error("✗ 连接过程中发生异常！");
            logger.error("异常类型: {}", e.getClass().getName());
            logger.error("异常信息: {}", e.getMessage());
            logger.error("==========================================", e);
        } finally {
            if (milvusClient != null) {
                logger.info("\n正在关闭 Milvus 连接...");
                milvusClient.close();
                logger.info("连接已关闭。");
            }
        }
    }
}