# dag-task-si

DAG 任务调度系统的领域模型层（Domain Model）。

## 概述

dag-task-si 定义了 DAG 任务调度系统的核心领域对象和接口，为上层的核心实现、服务端和客户端模块提供了统一的抽象。本模块只包含接口和领域模型，不包含具体的业务逻辑实现。

## 核心领域对象

| 类/接口 | 描述 |
|--------|------|
| `Task<I, O>` | 抽象任务类，定义了任务的基本属性和生命周期方法 |
| `TaskInput<I>` | 任务输入记录，封装任务ID、输入数据和属性 |
| `TaskOutput<O>` | 任务输出记录，封装任务执行结果 |
| `TaskContext<I, O>` | 任务上下文接口，提供任务执行时需要的环境信息 |
| `TaskExecution<I, O>` | 任务执行函数式接口，定义任务实际执行逻辑 |
| `TaskFactory` | 任务工厂接口，负责根据执行键创建任务执行实例 |
| `TaskOrder` | 任务订单，代表一次 DAG 任务执行订单 |
| `TaskOrderDao` | 任务订单数据访问接口 |
| `TaskRecord` | 任务记录，持久化单个任务的执行状态 |
| `TaskRecordDao` | 任务记录数据访问接口 |
| `TaskDagService<I, O>` | DAG 服务核心接口 |

## 枚举类型

| 枚举 | 描述 |
|------|------|
| `OrderType` | 订单类型 |
| `TaskStatus` | 任务状态（READY, RUNNING, SUCCESS, ERROR, TIMEOUT, SKIPPED, UNKNOWN）|

## 依赖

- `zora-common` - Zora 框架公共工具类
- `zora-jdbi` - Zora JDBBI 支持，提供 BaseDao 基类
- `jackson-databind` - JSON 序列化支持
- `slf4j-api` - 日志门面

## 模块结构

```
dag-task-si/
├── src/main/java/top/ilovemyhome/dagtask/si/
│   ├── enums/
│   │   ├── OrderType.java
│   │   └── TaskStatus.java
│   ├── Task.java
│   ├── TaskContext.java
│   ├── TaskDagService.java
│   ├── TaskExecution.java
│   ├── TaskFactory.java
│   ├── TaskInput.java
│   ├── TaskOrder.java
│   ├── TaskOrderDao.java
│   ├── TaskOutput.java
│   ├── TaskRecord.java
│   └── TaskRecordDao.java
└── pom.xml
```

## 使用说明

本模块是领域模型层，通常被 `dag-task-core` 依赖，由 `dag-task-core` 提供具体实现。独立使用本模块没有实际意义。

```xml
<dependency>
    <groupId>top.ilovemyhome</groupId>
    <artifactId>dag-task-si</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
