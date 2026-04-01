# dag-task-core 架构分析与改进建议

## 当前架构概述

`dag-task-core` 是一个设计良好的**基于 DAG（有向无环图）的任务调度执行框架**，采用清晰的分层架构：

| 层级 | 职责 |
|------|------|
| **dag-task-si** | 领域模型层，定义核心领域对象（Task、TaskContext、TaskExecution、TaskRecord、TaskOrder 等） |
| **dag-task-core** | 核心实现层，包含 DAG 验证、任务执行、持久化、缓存 |

### 核心组件交互

```
┌─────────────────────────────────────────────────────────────┐
│        AbstractTaskDagServiceImpl (Main Service API)         │
├─────────────────────────────────────────────────────────────┤
│  • 创建任务订单/任务                                              │
│  • DAG 循环检测 (DagHelper)                                     │
│  • Caffeine 缓存任务 DAG                                        │
│  • 启动 DAG 执行                                                 │
│  • 接收任务完成事件                                              │
└────────────┬──────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│               Task (Base Abstract Class)                      │
├─────────────────────────────────────────────────────────────┤
│  • 维护任务状态（INIT/RUNNING/SUCCESS/ERROR/TIMEOUT/SKIPPED）  │
│  • 状态流转和持久化更新                                         │
│  • 成功后自动触发后继任务                                        │
└────────────┬──────────────────────────────────────────────────┘
             ├────────────┐
             ▼            ▼
┌───────────────────┐  ┌───────────────────┐
│    SyncTask        │  │    AsyncTask      │
│  (同步执行)        │  │  (异步执行)        │
│ CompletableFuture  │  │ 等待外部事件回调  │
│ 带超时控制         │  │                   │
└───────────────────┘  └───────────────────┘
```

---

## 当前架构优点

✅ **优点总结：**

1. **关注点分离清晰** - SI 层与实现层分离，依赖方向正确，易于扩展和测试
2. **良好的抽象设计** - Task 基类处理通用逻辑，SyncTask/AsyncTask 分别处理不同执行模式
3. **DAG 验证正确** - 增量添加任务时进行全图环检测，保证 DAG 正确性
4. **灵活的执行模式** - 同时支持同步（自动完成）和异步（外部回调）两种执行模式
5. **合理的超时处理** - 每个任务可独立配置超时时间
6. **高效的缓存策略** - 使用 Caffeine LRU 缓存，自动过期，支持统计
7. **SQL-first 持久化** - Jdbi 避免了 Hibernate 的复杂性，性能更好，控制更精确
8. **线程池防护** - 固定大小线程池 + 有界队列 + 拒绝策略，避免资源耗尽

---

## 架构改进建议

### 🔴 **高优先级改进**

#### 1. 移除不必要的 `synchronized`，提升并发性能

**问题**: `createTasks`, `deleteOrderByKey`, `start`, `receiveTaskEvent` 都是 `synchronized`，造成不必要的锁竞争，成为并发瓶颈。

**改进**:
- 移除 `synchronized` 关键字
- Caffeine 本身是线程安全的，不需要额外锁
- 数据库事务已经提供可见性保证
- 在修改任务后添加缓存失效

```java
// Before:
@Override
public synchronized List<Long> createTasks(List<TaskRecord> records) { ... }

// After:
@Override
public List<Long> createTasks(List<TaskRecord> records) {
    // ... existing code ...
    loadingCache.invalidate(orderKey);  // 添加这行
    return rs;
}
```

#### 2. 保证缓存一致性

**问题**: 只有删除操作才失效缓存，新增任务后缓存仍然保留旧版本 DAG

**修复**: 在 `createTasks` 中添加 `loadingCache.invalidate(orderKey)`

---

### 🟡 **中优先级改进**

#### 3. 自定义异常体系

**问题**: 大量使用 `IllegalArgumentException` 处理业务错误，调用者难以分类处理

**改进**: 创建具体的异常类型:
```java
// 新增
public class DagValidationException extends RuntimeException { ... }
public class TaskNotFoundException extends RuntimeException { ... }
public class DuplicateTaskOrderException extends RuntimeException { ... }
```

#### 4. 线程池配置可定制

**问题**: `AbstractTaskContext` 硬编码了线程池大小 (`min(CPU, 16)`) 和队列容量 (1024)

**改进**: 添加构造函数允许外部配置:
```java
protected AbstractTaskContext(Jdbi jdbi, int corePoolSize, int maxPoolSize,
    int queueCapacity, String threadNamePattern) {
    // 使用自定义参数创建线程池
}
```
保留原有默认构造方法保持向后兼容。

#### 5. 添加任务状态变化事件发布

**问题**: 外部系统无法监听任务进度变化

**改进**:
- 添加 `TaskStatusChangeEvent` 事件类
- 添加 `TaskEventPublisher` 接口
- 在任务启动、成功、失败、超时时发布事件
- 支持 SPI 多种实现（Spring Events/Guava EventBus/Custom）

#### 6. 失败任务自动重试

**问题**: 失败/超时任务直接标记为最终失败，没有自动重试

**改进**:
- 在 `TaskRecord` 添加 `retryCount` 和 `maxRetries` 字段
- 任务失败时，如果重试次数未满，自动重新提交
- 可选支持指数退避策略

---

### 🟢 **低优先级改进**

#### 7. 暴露缓存统计信息

**问题**: Caffeine 开启了 `recordStats()` 但没有暴露统计信息

**改进**: 添加方法暴露:
```java
public CacheStats getCacheStats() {
    return loadingCache.stats();
}
```

#### 8. 支持条件分支

**当前架构**不支持根据前驱任务结果动态选择执行路径。可以添加 `Predicate<TaskOutput<?>>` 支持条件执行，满足更复杂的业务流程需求。

#### 9. 缓存配置可外部化

**问题**: Caffeine 参数（过期时间、最大大小）硬编码

**改进**: 将这些参数提取为可配置的构造参数。

---

## 优先级总结

| 优先级 | 改进项 | 收益 | 改动量 |
|--------|--------|------|--------|
| 🔴 高 | 移除不必要的 `synchronized` | ⚡ 大幅提升并发性能 | 小 |
| 🔴 高 | 添加缓存失效保证一致性 | 🗽️ 避免脏读 | 小 |
| 🟡 中 | 自定义异常体系 | 👍 更好的错误处理 | 小 |
| 🟡 中 | 线程池配置可定制 | 🔧 适应不同场景 | 小 |
| 🟡 中 | 添加事件发布 | 🔌 便于外部集成 | 中 |
| 🟡 中 | 添加重试机制 | ✅ 提高鲁棒性 | 中 |
| 🟢 低 | 暴露缓存统计 | 📊 提升可观测性 | 小 |
| 🟢 低 | 条件分支支持 | ⭐ 功能增强 | 大 |

---

## 总结

**整体评价**: 当前架构设计优秀，分层清晰，职责明确，已经满足 DAG 任务调度的基本需求。主要改进集中在**并发性能**、**一致性**和**可配置性**，这些改进都是增量式的，可以在保持向后兼容的前提下完成，能显著提升框架在工业生产环境中的适用性。
