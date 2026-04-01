# 架构调整：从内存有向图驱动改为数据库驱动

## 变更背景

原始架构将整个 DAG 加载到内存中构建完整图，维护前驱/后继任务的对象引用。这种方式在 DAG 规模较大时会占用较多内存，并且需要处理缓存一致性问题。

本次调整将就绪检查完全下移到数据库层，完全移除内存缓存和内存图构建，实现了更优雅的架构。

## 架构对比

### 原始架构（内存驱动）

```
┌─────────────────────────────────────────────────────────────┐
│      AbstractTaskDagServiceImpl                               │
│          │                                                  │
│          └─> LoadingCache<orderKey, List<Task>> 缓存整个 DAG   │
│                  │                                            │
│                  └─> 构建内存图，addPriorTask/addSuccessorTask      │
│                                                             │
│ 当任务完成: Success                                      │
│     for (successorId : successors) {                       │
│         if (dao.isReady(successorId)) {                     │
│             dao.loadTaskById(successorId).ifPresent(submit) │
│         }                                                   │
│     }                                                       │
└─────────────────────────────────────────────────────────────┘
```

**特点:**
- 优点：一次加载多次使用，执行速度快
- 缺点：占用内存大，缓存一致性问题，large DAG 扩展性差

### 新架构（数据库驱动）

```
┌─────────────────────────────────────────────────────────────┐
│      AbstractTaskDagServiceImpl                               │
│          │                                                  │
│ 启动时: findReadyTasksForOrder(orderKey)                     │
│          └─> 单 SQL 查询直接得到所有就绪的起始任务                     │
│                 └─> 逐个提交                                      │
│                                                             │
│ 当任务完成: Success                                      │
│     dao.findReadySuccessors(taskId)                          │
│          └─> 单 SQL 查询直接得到所有就绪的后继任务                   │
│                 └─> 逐个提交                                      │
└─────────────────────────────────────────────────────────────┘
```

**特点:**
- ✅ 零内存缓存，始终从数据库读取，保证一致性
- ✅ 不需要构建内存图，移除了 `successorTasks`/`priorTasks` 字段
- ✅ 单 SQL 查询获取就绪任务，比多次查询更高效
- ✅ 天然支持超大 DAG，不受内存限制
- ✅ 代码更简洁

## 核心SQL设计

### 1. `isReady(Long taskId)` - 检查单个任务是否就绪

```sql
select count(*) from t_task
where (successor_ids::jsonb) @> jsonb_build_array(:taskId)
  and status != 'SUCCESS'
```

**逻辑:** 找出所有"把当前任务放在后继列表中"且**未成功**的前驱任务，如果计数为 0，则当前任务就绪。

### 2. `findReadyTasksForOrder(String orderKey)` - 获取订单所有就绪任务

```sql
select * from t_task
where order_key = :orderKey
  and status = 'INIT'
  and not exists (
    select 1 from t_task t2
    where (t2.successor_ids::jsonb) @> jsonb_build_array(t_task.id)
      and t2.status != 'SUCCESS'
  )
```

**逻辑:** 对订单中每个 INIT 任务，检查是否不存在未完成的前驱任务，如果不存在未完成前驱，则该任务就绪。

### 3. `findReadySuccessors(Long taskId)` - 获取当前任务的所有就绪后继

```sql
select * from t_task
where id in (
    select (jsonb_array_elements(successor_ids::jsonb))::bigint
    from t_task where id = :taskId
  )
  and status = 'INIT'
  and not exists (
    select 1 from t_task t2
    where (t2.successor_ids::jsonb) @> jsonb_build_array(t_task.id)
      and t2.status != 'SUCCESS'
  )
```

**逻辑:**
1. 从当前任务的 `successor_ids` JSON 数组展开得到所有后继任务 ID
2- 对每个后继，检查是否所有前驱都已完成
3. 返回所有就绪的后继任务

## 代码变更总结

### `Task.java`

- ✅ 移除 `successorTasks` / `priorTasks` 字段
- ✅ 移除 `addPriorTask()` / `addSuccessorTask()` / `getSuccessorTasks()` / `getPriorTasks()`
- ✅ 简化 `success()` 方法 - 单句调用 `findReadySuccessors` 代替循环
- ✅ 简化 `skip()` 方法 - 同样使用 `findReadySuccessors`
- ✅ `isReady()` 现在直接调用 DAO 的 SQL 检查

### `TaskRecordDao.java`

- ✅ 添加 `boolean isReady(Long taskId)`
- ✅ 添加 `<I,O> Optional<Task<I,O>> loadTaskById(Long taskId)`
- ✅ 添加 `<I,O> List<Task<I,O>> findReadyTasksForOrder(String orderKey)`
- ✅ 添加 `<I,O> List<Task<I,O>> findReadySuccessors(Long taskId)`

### `AbstractTaskDagServiceImpl.java`

- ✅ 移除 Caffeine `LoadingCache` 相关代码
- ✅ 移除内存图构建循环（不再需要调用 `addSuccessorTask`）
- ✅ `start()` 简化为直接调用 `findReadyTasksForOrder`

### `TaskRecordDaoJdbiImpl.java`

- ✅ 实现所有新添加的方法，使用 PostgreSQL JSONB 函数

## 优点总结

1. **更低内存占用**: 不需要在内存中保存整个 DAG
2. **更好的扩展性**: 支持超大 DAG（上千个任务）
3. **更强一致性**: 总是从数据库读取，不会有缓存 stale 问题
4. **更简洁代码**: 去掉了复杂的图构建逻辑，减少出错可能
5. **数据库层面优化**: PostgreSQL 对 JSONB 有良好索引支持，查询性能优异

## 兼容性说明

- 数据库 schema **没有变化**，不需要迁移
- 现有的业务代码不需要修改，API 保持兼容
- 测试全部通过，行为和之前一致
