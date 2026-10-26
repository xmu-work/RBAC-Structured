package com.iam.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 统一应用软件权限管理系统启动类。
 *
 * <p>对应详细设计说明书 3.1 分层结构：接口层 → 应用服务层 → 基础设施层。
 * 采用结构化编程思想：自顶向下分层、单一职责、高内聚低耦合。
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class IamApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamApplication.class, args);
    }
}
