# 06-FEATURE: 死信文件夹实现读写分离

## 概述

当前死信队列使用单个JSONL文件存储所有失败的任务结果报告。这种实现存在以下问题：

1. **读写不分离**：重试时需要读取整个文件，处理完后需要重写整个文件来保留未成功项，写入操作会被重试阻塞
2. **并发安全问题**：并发写入同一个文件可能导致文件损坏
3. **性能问题**：死信文件较大时，每次重试都要全量读取重写，性能较差

本功能将死信存储改为**文件夹模式**，每个失败批次写入一个独立文件，实现完全的读写分离。

## 设计方案

### 配置变更

| 原配置项 | 新配置项 | 说明 |
|---------|---------|------|
| `deadLetterPersistenceFile` | `deadLetterPersistencePath` | 原配置指向文件，新配置指向文件夹 |

**向后兼容**：如果配置仍然是原名称，或者路径指向一个已存在文件，保持原有单文件模式工作，确保平滑升级。

### 存储结构

```
dead-letter/
  ├── 20260413-123456-550e8400-e29b-41d4-a716-446655440000.json
  ├── 20260413-123500-550e8400-e29b-41d4-a716-446655440001.json
  └── ...
```

- 每个失败批次（一批可以包含多个任务结果）创建一个独立文件
- 文件名格式：`YYYYMMDD-HHmmss-<UUID>.json`，保证唯一性
- 每个文件内容就是该批次失败任务的JSON数组

### 写入流程（新增死信）

1. 检查死信目录是否存在，不存在则创建
2. 生成唯一文件名
3. 打开新文件，写入JSON内容
4. 关闭文件 → 完成，不会修改任何已有文件

**特点**：写入是完全无锁的，不会被任何重试操作阻塞。

### 重试流程（定期执行）

1. 列出死信目录下所有文件
2. 对每个文件依次处理：
   - 读取文件内容
   - 逐个重试报告任务结果
   - **无论成功失败，处理完后都删除该文件**
     - 全部成功 → 文件删除，干净
     - 部分失败 → 失败项会被重新持久化到新文件 → 保证新文件，不会和旧文件混合
3. 统计成功重试数量返回

**特点**：
- 读取处理只涉及单个文件，处理完就删除
- 不需要修改任何文件，也不需要重写
- 读写完全分离

## 代码变更清单

### 1. AgentConfiguration.java
- 添加新字段 `deadLetterPersistencePath`
- 保持旧字段 `deadLetterPersistenceFile` 用于向后兼容
- 更新Builder、getter/setter、toString

### 2. TaskExecutionEngine.java
- 将 `private File deadLetterFile` 改为 `private File deadLetterDirectory`
- 修改 `initDeadLetterFile()` → `initDeadLetterPersistence()`，支持两种模式检测
- 修改 `persistToDeadLetterFile()` → 新增目录模式的写入逻辑
- 修改 `retryPersistedDeadLetterOnce()` → 新增目录模式的重试逻辑
- `getDeadLetterQueueSize()` 改为统计目录下文件数量/总条目数
- 保持public API不变（`retryDeadLetter()`, `getDeadLetterQueueSize()`, `recoverPersistedDeadLetter()` 方法签名不变）

### 3. README.md
- 更新配置示例，将 `deadLetterPersistenceFile` 改为 `deadLetterPersistencePath`
- 更新死信队列机制说明

## 优势对比

| 特性 | 单文件模式 | 文件夹模式 |
|-----|----------|-----------|
| 读写分离 | ❌ 读写互相阻塞 | ✅ 完全分离 |
| 并发写入安全 | ❌ 可能冲突损坏 | ✅ 完全安全 |
| 大文件性能 | ❌ 每次全量重写 | ✅ 只处理需要的文件 |
| 文件碎片 | 少 | 较多（但每个文件很小，不影响） |
| 向后兼容 | - | ✅ 支持平滑升级 |

## 默认配置

新的默认配置为：
```hocon
deadLetterPersistencePath = "./dead-letter"
```

指向一个文件夹，而不是单个文件。
