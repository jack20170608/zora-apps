# 01 ARCHITECTURE: 项目初始化架构设计

## 概述

本文档描述了dag-task项目的初始架构设计，包括模块划分、依赖关系和技术选型。

## 架构设计

### 模块划分

项目采用分层模块化架构，分为四个核心模块：

1. **dag-task-si** - 领域模型层
   - 职责：定义所有核心抽象接口和数据结构
   - 依赖：无核心依赖，仅SLF4J日志

2. **dag-task-core** - 核心实现层
   - 职责：实现DAG调度核心逻辑
   - 依赖：dag-task-si

3. **dag-task-server** - 调度中心服务层
   - 职责：提供REST API，负责任务管理和调度
   - 依赖：dag-task-core

4. **dag-task-agent** - 执行器服务层
   - 职责：执行具体任务，上报执行结果
   - 依赖：dag-task-core

### 依赖关系图

```
dag-task-si ← dag-task-core ← dag-task-server
                       ↓
                  dag-task-agent
```

所有模块都依赖父项目的依赖管理，统一版本控制。

## 技术选型

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 25 | 开发语言 |
| Maven | 3.x | 构建工具 |
| JUnit 5 | 5.10.0 | 单元测试 |
| Mockito | 5.11.0 | 测试Mock |
| SLF4J | 2.0.9 | 日志抽象 |
| Logback | 1.4.11 | 日志实现 |

## 编码规范

- 代码注释使用英文
- 文档使用中文
- 遵循Maven标准目录结构
- 每个模块都必须创建 `src/main/resources` 和 `src/test/resources` 目录

## 后续计划

1. 在dag-task-si中定义核心接口
2. 在dag-task-core中实现DAG构建和拓扑排序
3. 实现任务调度算法
4. 实现执行引擎
5. 在dag-task-server中实现REST API
6. 在dag-task-agent中实现任务执行协议
