# Agent 状态与 Token 关联查询 SQL

## 概述

本文档提供一组常用的 SQL 查询，用于关联 `t_agent_status` 和 `t_agent_tokens` 两张表，查询 Agent 的运行时状态及其对应的 Token 信息。

## 表结构说明

### t_agent_status

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| agent_id | VARCHAR(255) | Agent 唯一标识 |
| agent_url | VARCHAR(1024) | Agent 服务地址 |
| max_concurrent_tasks | INTEGER | 最大并发任务数 |
| max_pending_tasks | INTEGER | 最大待处理任务数 |
| supported_execution_keys | TEXT | 支持的执行类型 |
| running | BOOLEAN | 是否在线运行 |
| pending_tasks | INTEGER | 待处理任务数 |
| running_tasks | INTEGER | 运行中任务数 |
| finished_tasks | INTEGER | 已完成任务数 |
| last_heartbeat_at | TIMESTAMP WITH TIME ZONE | 最后心跳时间 |

### t_agent_tokens

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| token_id | VARCHAR(64) | Token 唯一标识 |
| agent_id | VARCHAR(255) | 关联的 Agent ID |
| name | VARCHAR(255) | Token 名称 |
| description | TEXT | Token 描述 |
| created_by | VARCHAR(100) | 创建者 |
| created_at | TIMESTAMP WITH TIME ZONE | 创建时间 |
| expires_at | TIMESTAMP WITH TIME ZONE | 过期时间 |
| revoked | BOOLEAN | 是否已吊销 |
| revoked_at | TIMESTAMP WITH TIME ZONE | 吊销时间 |
| revoked_by | VARCHAR(100) | 吊销者 |

## 关联查询

### 1. 基础关联查询

查询每个 Agent 的状态信息及其对应的 Token 详情。

```sql
SELECT
    -- Agent 状态信息
    s.agent_id,
    s.agent_url,
    s.running,
    s.pending_tasks,
    s.running_tasks,
    s.finished_tasks,
    s.max_concurrent_tasks,
    s.max_pending_tasks,
    s.supported_execution_keys,
    s.last_heartbeat_at AS status_last_heartbeat,

    -- Token 信息
    t.token_id,
    t.name AS token_name,
    t.description AS token_description,
    t.created_by AS token_created_by,
    t.created_at AS token_created_at,
    t.expires_at AS token_expires_at,
    t.revoked AS token_revoked,
    t.revoked_at AS token_revoked_at,
    t.revoked_by AS token_revoked_by

FROM t_agent_status s
LEFT JOIN t_agent_tokens t ON s.agent_id = t.agent_id;
```

### 2. 在线且 Token 有效的 Agent 查询

只查询当前在线运行且拥有未过期、未吊销 Token 的 Agent。

```sql
SELECT
    s.agent_id,
    s.agent_url,
    s.running,
    s.pending_tasks,
    s.running_tasks,
    s.finished_tasks,
    t.token_id,
    t.name AS token_name,
    t.expires_at,
    t.revoked
FROM t_agent_status s
JOIN t_agent_tokens t ON s.agent_id = t.agent_id
WHERE s.running = TRUE
  AND t.revoked = FALSE
  AND t.expires_at > CURRENT_TIMESTAMP;
```

### 3. 每个 Agent 的 Token 数量统计

按 Agent 分组，统计 Token 总数及有效 Token 数量。

```sql
SELECT
    s.agent_id,
    s.agent_url,
    s.running,
    s.pending_tasks,
    s.running_tasks,
    s.finished_tasks,
    COUNT(t.id) AS token_count,
    COUNT(CASE WHEN t.revoked = FALSE AND t.expires_at > CURRENT_TIMESTAMP THEN 1 END) AS active_token_count
FROM t_agent_status s
LEFT JOIN t_agent_tokens t ON s.agent_id = t.agent_id
GROUP BY
    s.agent_id,
    s.agent_url,
    s.running,
    s.pending_tasks,
    s.running_tasks,
    s.finished_tasks;
```

### 4. 无 Token 的 Agent 查询

用于排查异常：找出已注册状态但没有关联 Token 的 Agent。

```sql
SELECT
    s.agent_id,
    s.agent_url,
    s.running,
    s.last_heartbeat_at
FROM t_agent_status s
LEFT JOIN t_agent_tokens t ON s.agent_id = t.agent_id
WHERE t.id IS NULL;
```

## 注意事项

1. `t_agent_tokens` 与 `t_agent_status` 是一对多关系，一个 Agent 可以拥有多个 Token。因此基础关联查询可能返回多行（每个 Token 一行）。
2. 查询 1 和 3 使用 `LEFT JOIN`，以兼容暂时没有 Token 的 Agent（例如刚注册但尚未生成 Token）。
3. 查询 2 使用 `JOIN`，只返回既有状态记录又有有效 Token 的 Agent。
4. 所有时间字段均使用 `TIMESTAMP WITH TIME ZONE` 类型，查询时无需额外时区转换。
