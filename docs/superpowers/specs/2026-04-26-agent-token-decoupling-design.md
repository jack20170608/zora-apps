# Agent 与 Token 管理解耦设计

## 背景

当前 `t_agent_registry` 表将 Agent 运行时数据与 Token 认证元数据混合存储，导致：
- Token 生成需要伪装成 `agent_id = "__token__" + tokenId`，语义混乱
- 一个 Agent 只能关联一个 Token，无法支持多 Token 场景
- Agent 身份信息（名称、标签等）与运行时状态（心跳、任务数）耦合，不利于独立扩展

本设计将 Agent、Token、Registration（注册流程）拆分为独立的表和数据模型。

---

## 核心决策

| 决策项 | 选择 | 说明 |
|--------|------|------|
| Token 归属 | **绑定到 Agent** | 每个 Token 必须属于某个 Agent（FK `agent_id`） |
| 多 Token 支持 | **允许** | 一个 Agent 可同时拥有多个有效 Token，便于轮换和灰度 |
| Agent 实体创建时机 | **Registration 审批后** | `t_agent_registrations` 是流程记录，`t_agents` 是最终实体 |
| 运行时状态 | **独立表** | `t_agent_status` 存储心跳、任务数等易变数据，与静态身份信息分离 |

---

## 数据模型

### 1. `t_agents` — Agent 实体表

存储 Agent 的静态身份信息。

```sql
CREATE TABLE t_agents (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    labels JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_agents_status ON t_agents(status);
CREATE INDEX idx_t_agents_labels ON t_agents USING GIN(labels);
```

**字段说明：**
- `agent_id`: 业务键，由 Agent 自报（如 `prod-worker-01`）
- `status`: `PENDING`（注册中）→ `ACTIVE`（正常运行）→ `INACTIVE`（下线）
- `labels`: JSONB，扩展字段，存储环境、区域等标签

### 2. `t_agent_tokens` — Token 凭证表

独立管理 Token，与 Agent 多对一关联。

```sql
CREATE TABLE t_agent_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by VARCHAR(100),

    CONSTRAINT fk_agent_tokens_agent_id FOREIGN KEY (agent_id)
        REFERENCES t_agents(agent_id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_tokens_agent_id ON t_agent_tokens(agent_id);
CREATE INDEX idx_agent_tokens_token_id ON t_agent_tokens(token_id);
CREATE INDEX idx_agent_tokens_revoked ON t_agent_tokens(revoked);
```

### 3. `t_agent_registrations` — 注册申请流程表

存储 Agent 的注册申请记录，审批后关联到 `t_agents`。

```sql
CREATE TABLE t_agent_registrations (
    id BIGSERIAL PRIMARY KEY,
    registration_id VARCHAR(64) NOT NULL UNIQUE,
    agent_name VARCHAR(255) NOT NULL,
    description TEXT,
    labels JSONB,
    callback_url TEXT NOT NULL,
    nonce VARCHAR(64) NOT NULL,
    client_address VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    processed_by VARCHAR(100),
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    agent_id VARCHAR(255),

    CONSTRAINT fk_registrations_agent_id FOREIGN KEY (agent_id)
        REFERENCES t_agents(agent_id) ON DELETE SET NULL
);

CREATE INDEX idx_agent_registrations_status ON t_agent_registrations(status);
CREATE INDEX idx_agent_registrations_expires_at ON t_agent_registrations(expires_at);
```

### 4. `t_agent_status` — 运行时状态表

从原 `t_agent_registry` 剥离，仅存储运行时动态数据。

```sql
CREATE TABLE t_agent_status (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL UNIQUE,
    agent_url VARCHAR(1024) NOT NULL,
    max_concurrent_tasks INTEGER NOT NULL,
    max_pending_tasks INTEGER NOT NULL,
    supported_execution_keys TEXT NOT NULL,
    running BOOLEAN NOT NULL DEFAULT TRUE,
    pending_tasks INTEGER NOT NULL DEFAULT 0,
    running_tasks INTEGER NOT NULL DEFAULT 0,
    finished_tasks INTEGER NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_agent_status_agent_id FOREIGN KEY (agent_id)
        REFERENCES t_agents(agent_id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_status_running ON t_agent_status(running);
```

### 实体关系图

```
+-----------------+     1:N      +------------------+
|   t_agents      |◄─────────────|  t_agent_tokens  |
|   (身份信息)     |              |   (凭证管理)      |
+--------+--------+              +------------------+
         |
         | 1:1
         +────────────────────────►+------------------+
                                  |  t_agent_status  |
                                  |   (运行时状态)     |
                                  +------------------+
         |
         | 1:0..1
         +────────────────────────►+------------------------+
                                  | t_agent_registrations  |
                                  |   (注册流程记录)         |
                                  +------------------------+
```

---

## 业务流程

### 1. 自动注册流程（Agent 首次启动）

```
Agent 启动 → 无本地 token
    │
    ▼
POST /api/v1/agent/register
{ name, description, labels, callbackUrl }
    │
    ▼
Server:
  ├─ 检查 whitelist
  ├─ 创建 Agent 记录 → t_agents (status=PENDING)
  ├─ 创建 Registration 记录 → t_agent_registrations (status=PENDING)
  │
  ├─ 【Whitelist 匹配 → Auto-approve】
  │   ├─ 生成 Token → t_agent_tokens (agent_id = 新 agent)
  │   ├─ 更新 Registration → status=APPROVED, agent_id 回填
  │   ├─ 更新 Agent → status=ACTIVE
  │   └─ 推送 token 到 callback
  │
  └─ 【未匹配 → 等待 Manual-approve】
      └─ Admin 调用 approve → 执行相同 token 生成 + 推送逻辑
```

### 2. 手动生成 Token（管理员操作）

```
Admin POST /api/v1/admin/tokens/generate
{
  "agentId": "prod-worker-01",   // 新增必填
  "name": "轮换 token",
  "description": "用于热切换",
  "expiresInDays": 365
}
    │
    ▼
Server:
  ├─ 校验 agentId 存在于 t_agents → 不存在: 400 Bad Request
  └─ 存在 → 生成 token 写入 t_agent_tokens
```

### 3. Token 校验流程

```
Agent 请求 → AgentTokenAuthFilter
    │
    ▼
提取 Authorization: Bearer {jwt}
    │
    ▼
校验 JWT 签名 + 过期时间
    │
    ▼
解析 jti → token_id
    │
    ▼
查 t_agent_tokens:
  ├─ 不存在 → 401
  ├─ revoked=true → 401
  └─ revoked=false 且未过期 → 通过，获取 agent_id
```

---

## API 变更

### `GenerateTokenRequest`（dag-si）

```java
public record GenerateTokenRequest(
    String agentId,        // 新增：必填，token 绑定目标 agent
    String name,
    String description,
    int expiresInDays
) {}
```

### `TokenService` 签名变更

```java
// 旧：
public GenerateTokenResult generateToken(String name, String description, int expiresInDays, String createdBy)

// 新：
public GenerateTokenResult generateToken(
    String agentId,
    String name,
    String description,
    int expiresInDays,
    String createdBy
)
```

内部逻辑：
1. 调用 `agentDao.findByAgentId(agentId)` 校验存在性
2. 不存在 → 抛 `IllegalArgumentException`，上层 API 转 400
3. 存在 → 生成 token 写入 `t_agent_tokens`

---

## DAO 层调整

### 新增 `AgentDao`

```java
public interface AgentDao {
    Agent create(Agent agent);
    Optional<Agent> findByAgentId(String agentId);
    List<Agent> findByStatus(Agent.Status status);
    void updateStatus(String agentId, Agent.Status status);
    void updateHeartbeat(String agentId, Instant heartbeatAt);
}
```

### 新增 `AgentTokenDao`

```java
public interface AgentTokenDao {
    AgentToken insert(AgentToken token);
    Optional<AgentToken> findByTokenId(String tokenId);
    List<AgentToken> findByAgentId(String agentId);
    List<AgentToken> findActiveByAgentId(String agentId);
    void revokeToken(String tokenId, String revokedBy);
    List<AgentToken> listAll();
}
```

### 改造 `AgentRegistryDao` → `AgentStatusDao`

- 原接口保留但重命名，实现从 `t_agent_registry` 迁移到 `t_agent_status`
- 删除 token 相关方法（`findByTokenId`, `revokeToken`），移至 `AgentTokenDao`

### `TokenService` 依赖变更

```java
// 旧：
public TokenService(AgentRegistryDao agentRegistryDao, JwtConfig jwtConfig)

// 新：
public TokenService(AgentTokenDao agentTokenDao, AgentDao agentDao, JwtConfig jwtConfig)
```

---

## 数据迁移（Flyway）

### V2 迁移脚本

```sql
-- ============================================================================
-- Flyway Migration: V2
-- Description: Decouple agent, token, and registration into separate tables
-- ============================================================================

-- 1. Create new tables
CREATE TABLE t_agents (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    labels JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_agent_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by VARCHAR(100),
    CONSTRAINT fk_agent_tokens_agent_id FOREIGN KEY (agent_id)
        REFERENCES t_agents(agent_id) ON DELETE CASCADE
);

CREATE TABLE t_agent_status (
    id BIGSERIAL PRIMARY KEY,
    agent_id VARCHAR(255) NOT NULL UNIQUE,
    agent_url VARCHAR(1024) NOT NULL,
    max_concurrent_tasks INTEGER NOT NULL,
    max_pending_tasks INTEGER NOT NULL,
    supported_execution_keys TEXT NOT NULL,
    running BOOLEAN NOT NULL DEFAULT TRUE,
    pending_tasks INTEGER NOT NULL DEFAULT 0,
    running_tasks INTEGER NOT NULL DEFAULT 0,
    finished_tasks INTEGER NOT NULL DEFAULT 0,
    last_heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_agent_status_agent_id FOREIGN KEY (agent_id)
        REFERENCES t_agents(agent_id) ON DELETE CASCADE
);

-- 2. Migrate data from t_agent_registry
INSERT INTO t_agents (agent_id, name, status, registered_at, last_heartbeat_at, created_at)
SELECT
    agent_id,
    COALESCE(token_name, agent_id) as name,
    CASE WHEN running THEN 'ACTIVE' ELSE 'INACTIVE' END as status,
    registered_at,
    last_heartbeat_at,
    registered_at as created_at
FROM t_agent_registry;

INSERT INTO t_agent_tokens (token_id, agent_id, name, description, created_by, created_at, expires_at, revoked, revoked_at, revoked_by)
SELECT
    token_id,
    agent_id,
    token_name,
    description,
    created_by,
    created_at,
    expires_at,
    revoked,
    revoked_at,
    revoked_by
FROM t_agent_registry;

INSERT INTO t_agent_status (agent_id, agent_url, max_concurrent_tasks, max_pending_tasks, supported_execution_keys, running, pending_tasks, running_tasks, finished_tasks, last_heartbeat_at)
SELECT
    agent_id,
    agent_url,
    max_concurrent_tasks,
    max_pending_tasks,
    supported_execution_keys,
    running,
    pending_tasks,
    running_tasks,
    finished_tasks,
    last_heartbeat_at
FROM t_agent_registry;

-- 3. Add agent_id to t_agent_registrations and establish FK
ALTER TABLE t_agent_registrations ADD COLUMN agent_id VARCHAR(255);
ALTER TABLE t_agent_registrations ADD CONSTRAINT fk_registrations_agent_id
    FOREIGN KEY (agent_id) REFERENCES t_agents(agent_id) ON DELETE SET NULL;

-- 4. Create indexes
CREATE INDEX idx_t_agents_status ON t_agents(status);
CREATE INDEX idx_t_agents_labels ON t_agents USING GIN(labels);
CREATE INDEX idx_agent_tokens_agent_id ON t_agent_tokens(agent_id);
CREATE INDEX idx_agent_tokens_token_id ON t_agent_tokens(token_id);
CREATE INDEX idx_agent_tokens_revoked ON t_agent_tokens(revoked);
CREATE INDEX idx_agent_status_running ON t_agent_status(running);

-- 5. Keep old table for observation (can be dropped in V3 after validation)
-- Note: t_agent_registry is kept temporarily, code will switch to new tables
```

---

## 改动清单汇总

| 层级 | 改动项 |
|------|--------|
| **DB** | 新建 `t_agents`, `t_agent_tokens`, `t_agent_status`；Flyway V2 迁移；`t_agent_registry` 保留观察 |
| **DAO** | 新增 `AgentDao`, `AgentTokenDao`；改造 `AgentRegistryDao` → `AgentStatusDao` |
| **Service** | `TokenService` 增加 `agentId` 参数和校验；`RegistrationService` 审批时创建 Agent + Token |
| **API** | `GenerateTokenRequest` 增加 `agentId` 字段；`TokenManagementApi` 校验 agentId 存在性 |
| **Filter** | `AgentTokenAuthFilter` 从查旧表改为查 `t_agent_tokens` |
| **Agent** | 自动注册流程不变 |

---

## 兼容性说明

- 迁移脚本保持 `t_agent_registry` 原表不动，确保回滚能力
- 代码切换后，旧表数据仅作为备份，不再读写
- 建议在下一个版本（V3）中删除 `t_agent_registry` 表
