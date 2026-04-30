# dag-si

DAG 任务调度系统的领域模型层（Domain Model）。

## 概述

dag-si 定义了 DAG 任务调度系统的核心领域对象、枚举、服务接口和数据访问接口，为上层的 `dag-scheduler`（调度核心）、`dag-scheduler-muserver`（调度中心服务）、`dag-agent`（执行器）和 `dag-agent-muserver`（执行器服务）模块提供了统一的抽象。

本模块只包含接口和领域模型，不包含具体的业务逻辑实现。

## 核心领域对象

| 类/接口 | 描述 |
|--------|------|
| `TaskInput` | 任务输入，封装任务ID、输入数据和属性 |
| `TaskOutput` | 任务输出，封装任务执行结果（成功/失败、消息、输出数据）|
| `TaskExecution` | 任务执行函数式接口，定义任务实际执行逻辑 |
| `TaskOrder` | 任务订单，代表一次 DAG 任务执行订单 |
| `TaskRecord` | 任务记录，持久化单个任务的执行状态和生命周期信息 |
| `TaskTemplate` | DAG 模板，支持版本化的可复用 DAG 工作流定义 |
| `TaskDispatchRecord` | 任务派发记录，追踪任务被派发到哪个 Agent 执行 |
| `DispatchResult` | 派发结果，记录任务派发成功或失败的信息 |
| `TaskFactory` | 任务工厂，根据 executionKey 创建任务执行实例 |
| `TaskDagConfigLoader` | DAG 配置加载器接口 |
| `Constants` | 系统常量定义 |

## Agent 相关领域对象

| 类/接口 | 描述 |
|--------|------|
| `Agent` | 执行代理的静态身份信息 |
| `AgentStatus` | 执行代理的运行时状态（并发数、待处理任务、运行中任务等）|
| `AgentStatusReport` | Agent 向调度中心上报的状态报告 |
| `AgentUnregistration` | Agent 注销请求 |
| `AgentRegisterRequest` / `AgentRegisterResponse` | Agent 注册请求/响应 |
| `AgentSchedulerClient` | Agent 与调度中心通信的客户端接口 |
| `TaskExecuteResult` | 任务执行结果上报 |

## 认证相关

| 类 | 描述 |
|-----|------|
| `AgentRegistrationRequest` / `AgentRegistrationResponse` | Agent 注册认证请求/响应 |
| `GenerateTokenRequest` | 生成 Token 请求 |
| `TokenPushRequest` | Token 推送请求 |
| `TokenInfo` | Token 信息 |
| `AgentToken` | Agent Token 领域对象 |

## 枚举类型

| 枚举 | 描述 |
|------|------|
| `OrderType` | 订单类型（Yearly, Monthly, Weekly, Daily, Free, INSTANTIATED_FROM_TEMPLATE）|
| `TaskStatus` | 任务状态（INIT, READY, DISPATCHED, HOLD, RUNNING, SUCCESS, ERROR, TIMEOUT, SKIPPED, UNKNOWN）|
| `DispatchStatus` | 派发状态（DISPATCHED, ACCEPTED, REJECTED, FAILED, COMPLETED）|
| `TaskType` | 任务类型（JAVA_CLASS_NAME, GROOVY_SOURCE_CODE, BASH_SCRIPTS, PYTHON_SCRIPTS）|
| `PriorityType` | 优先级类型（URGENT, HIGH, NORMAL, LOW）|
| `OpsType` | 操作类型（SUBMIT, KILL, HOLD, FREE, FORCE_OK, FORCE_NOK）|

## 服务接口

| 接口 | 描述 |
|------|------|
| `TaskDagService` | DAG 任务记录生命周期管理（CRUD 和查询）|
| `DagManageService` | DAG 管理操作（创建任务、从模板实例化等）|
| `DagQueryService` | DAG 查询服务（支持分页查询）|
| `DagScheduleService` | DAG 运行时调度服务（启动、触发、手动操作等）|
| `TaskOrderService` | 任务订单服务接口 |
| `TaskTemplateService` | DAG 模板服务接口 |

## 数据访问接口（DAO）

| 接口 | 描述 |
|------|------|
| `TaskOrderDao` | 任务订单数据访问接口 |
| `TaskRecordDao` | 任务记录数据访问接口 |
| `TaskTemplateDao` | 任务模板数据访问接口 |
| `TaskDispatchDao` | 任务派发记录数据访问接口 |
| `AgentDao` | Agent 数据访问接口 |
| `AgentStatusDao` | Agent 状态数据访问接口 |
| `AgentTokenDao` | Agent Token 数据访问接口 |
| `AgentWhitelistDao` | Agent 白名单数据访问接口 |

## DTO 与查询条件

| 类 | 描述 |
|-----|------|
| `ResEntity` / `ResEntityHelper` | 统一响应实体及辅助工具 |
| `TaskOrderSearchCriteria` | 任务订单查询条件 |
| `TaskRecordSearchCriteria` | 任务记录查询条件 |
| `TaskTemplateSearchCriteria` | 任务模板查询条件 |
| `TaskSearchCriteria` | 任务查询条件 |
| `TaskDispatchRecordSearchCriteria` | 任务派发记录查询条件 |
| `AgentWhitelistSearchCriteria` | Agent 白名单查询条件 |
| `OperationRequest` | 操作请求 |
| `SubmitRequest` | 提交请求 |

## 异常

| 类 | 描述 |
|-----|------|
| `DagConfigurationException` | DAG 配置异常 |
| `TaskResultReportFailException` | 任务结果上报失败异常 |

## 依赖

- `zora-common` - Zora 框架公共工具类
- `zora-jdbi` - Zora JDBI 支持，提供 BaseDao 基类
- `zora-json` - Zora JSON 工具
- `jackson-databind` - JSON 序列化支持
- `jackson-datatype-jsr310` - JDK 8 日期时间 Jackson 支持
- `jakarta.ws.rs-api` - Jakarta RESTful Web Services API
- `swagger-annotations` - OpenAPI/Swagger 注解
- `slf4j-api` - 日志门面

## 模块结构

```
dag-si/
├── src/main/java/top/ilovemyhome/dagtask/si/
│   ├── agent/
│   │   ├── Agent.java
│   │   ├── AgentStatus.java
│   │   ├── AgentStatusReport.java
│   │   ├── AgentUnregistration.java
│   │   ├── AgentRegisterRequest.java
│   │   ├── AgentRegisterResponse.java
│   │   ├── AgentSchedulerClient.java
│   │   ├── TaskExecuteResult.java
│   │   └── TaskFactory.java
│   ├── auth/
│   │   ├── AgentRegistrationRequest.java
│   │   ├── AgentRegistrationResponse.java
│   │   ├── GenerateTokenRequest.java
│   │   ├── TokenPushRequest.java
│   │   ├── TokenInfo.java
│   │   └── AgentToken.java
│   ├── dto/
│   │   ├── ResEntity.java
│   │   ├── ResEntityHelper.java
│   │   ├── TaskOrderSearchCriteria.java
│   │   ├── TaskRecordSearchCriteria.java
│   │   ├── TaskTemplateSearchCriteria.java
│   │   ├── TaskSearchCriteria.java
│   │   ├── TaskDispatchRecordSearchCriteria.java
│   │   ├── AgentWhitelistSearchCriteria.java
│   │   ├── OperationRequest.java
│   │   └── SubmitRequest.java
│   ├── enums/
│   │   ├── OrderType.java
│   │   ├── TaskStatus.java
│   │   ├── DispatchStatus.java
│   │   ├── TaskType.java
│   │   ├── PriorityType.java
│   │   └── OpsType.java
│   ├── persistence/
│   │   ├── TaskOrderDao.java
│   │   ├── TaskRecordDao.java
│   │   ├── TaskTemplateDao.java
│   │   ├── TaskDispatchDao.java
│   │   ├── AgentDao.java
│   │   ├── AgentStatusDao.java
│   │   ├── AgentTokenDao.java
│   │   └── AgentWhitelistDao.java
│   ├── service/
│   │   ├── TaskDagService.java
│   │   ├── DagManageService.java
│   │   ├── DagQueryService.java
│   │   ├── DagScheduleService.java
│   │   ├── TaskOrderService.java
│   │   └── TaskTemplateService.java
│   ├── Constants.java
│   ├── DagConfigurationException.java
│   ├── DispatchResult.java
│   ├── TaskDagConfigLoader.java
│   ├── TaskDispatchRecord.java
│   ├── TaskExecution.java
│   ├── TaskFactory.java
│   ├── TaskInput.java
│   ├── TaskOrder.java
│   ├── TaskOutput.java
│   ├── TaskRecord.java
│   ├── TaskResultReportFailException.java
│   └── TaskTemplate.java
├── src/main/resources/
│   └── db/migration/postgresql/
│       ├── V1__t_task.sql
│       ├── V2__t_task_order.sql
│       ├── V3__t_task_template.sql
│       └── V4__t_task_dispatch.sql
├── metadata/
│   └── metadata.json
└── pom.xml
```

## 使用说明

本模块是领域模型层，通常被 `dag-scheduler` 和 `dag-agent` 依赖，由上层模块提供具体实现。独立使用本模块没有实际意义。

```xml
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-si</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
