# Keycloak 部署与配置

**状态：目标设计，具体版本和配置需在实施时锁定并验证。**

## 1. 环境策略

| 环境 | 用途 | 最低要求 |
|---|---|---|
| 开发 | 本机协议联调 | 固定测试 Realm、非生产账号 |
| 测试 | 自动化集成测试 | 可重建配置、测试密钥 |
| 预生产 | 升级与迁移演练 | 与生产同拓扑、独立数据 |
| 生产 | 正式身份服务 | HA、外部数据库、TLS、备份、监控 |

镜像必须固定到经过验证的版本或摘要，不使用浮动标签。升级前阅读该版本迁移说明，并在预生产恢复真实规模的脱敏备份进行演练。

## 2. Realm 基线

1. 配置稳定且唯一的 Issuer URL。
2. 启用组织要求的 MFA、密码、暴力破解检测和恢复策略。
3. 设置中央会话的空闲与绝对超时。
4. 限制管理控制台来源，管理账号强制 MFA。
5. 配置审计事件及安全日志保留期。
6. 禁止不需要的流程、协议和 Direct Access Grants。

## 3. Client 基线

每个应用单独注册 Client，不共享 Secret：

- 精确登记 Redirect URI 和 Post Logout Redirect URI；
- Web 后端使用 Confidential Client，Secret 进入 Secret 管理系统；
- SPA/移动端使用 Public Client，不配置伪 Secret；
- 强制 Authorization Code Flow 与 PKCE `S256`；
- 关闭 Implicit Flow 和 Resource Owner Password Credentials；
- Scope 按最小权限配置；
- Web Origin 使用明确允许列表，不设置宽泛通配符。

## 4. Claims 与角色

保持 Token 最小化。建议只发布稳定主体标识和确有需要的 Claims。角色采用显式命名和映射表：

```text
Keycloak client role: orders-reader
Zora permission: ORDER_READ
```

不把 Realm 管理角色、任意组名或用户可编辑属性直接映射为应用权限。

## 5. 密钥与 Secret

- Keycloak 签名私钥由平台能力安全托管，公钥通过 JWKS 发布。
- 轮换时保留验证旧 Token 所需的重叠窗口。
- Client Secret 有版本、负责人、轮换周期和紧急吊销流程。
- 数据库凭据、管理凭据和 Client Secret 不进入 Git、镜像或普通日志。

## 6. 上线验收

上线前必须验证：

- Discovery、JWKS、登录、刷新、登出和密钥轮换；
- 错误 Issuer、Audience、签名、过期 Token 和未知 `kid` 被拒绝；
- 开放重定向、登录 CSRF、Code 重放和权限提升测试；
- 节点故障、数据库故障、备份恢复和版本回滚；
- 指标、审计、告警和值班手册可用。
