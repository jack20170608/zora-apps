# dag-task

基于DAG（有向无环图）的任务调度系统。

## 项目概述

该系统将支持任务的定义、依赖关系管理、调度执行和结果跟踪等功能，支持分布式任务执行。

## 模块结构

- **dag-task-si**: DAG调度系统的领域模型接口定义
- **dag-task-core**: DAG调度系统核心实现，包含任务定义、依赖关系管理、调度算法和执行引擎等功能
- **dag-task-server**: 基于MuServer的任务调度中心，提供RESTful接口用于任务管理和调度
- **dag-task-agent**: 基于MuServer的任务执行器，负责执行调度中心分配的任务，并将执行结果反馈给调度中心

## 技术栈

- JDK 25
- Maven 3.x
- JUnit 5
- Mockito
- SLF4J + Logback

## 构建

```bash
cd dag-task
mvn clean install
```

## 模块说明

### dag-task-si
包含所有领域模型的接口定义，定义了任务、DAG图、执行上下文等核心抽象。

### dag-task-core
核心实现，包括：
- DAG图构建和验证
- 拓扑排序
- 任务调度算法
- 执行引擎
- 错误处理和重试机制

### dag-task-server
调度中心服务：
- 任务定义管理API
- DAG工作流部署
- 调度触发
- 执行状态监控
- Agent集群管理

### dag-task-agent
任务执行器：
- 向Server注册
- 心跳保持
- 任务拉取和执行
- 结果上报
- 本地任务日志记录

## License

Copyright © 2026 zora-apps
