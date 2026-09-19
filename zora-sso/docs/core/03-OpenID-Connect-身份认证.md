# OpenID Connect 身份认证

## 1. OIDC 是什么

OpenID Connect（OIDC）在 OAuth 2.0 之上增加身份层。客户端请求包含 `openid` Scope 后，授权服务器以 OpenID Provider（OP）的身份签发 ID Token，让客户端可以验证这次用户认证。

常见角色：

- **OpenID Provider / OP**：执行认证并签发 ID Token。
- **Relying Party / RP**：依赖认证结果的应用。
- **End-User**：被认证的用户。

## 2. ID Token 不等于 Access Token

| Token | 主要消费者 | 用途 |
|---|---|---|
| ID Token | Client / RP | 描述一次认证结果 |
| Access Token | Resource Server | 授权访问 API |
| Refresh Token | Authorization Server | 获取新的 Token |

业务 API 不应把 ID Token 当 Access Token。客户端也不应通过解析 Access Token 来代替 OIDC 身份验证。

## 3. ID Token 的核心 Claims

| Claim | 含义 | 校验要点 |
|---|---|---|
| `iss` | 签发者 | 必须与预配置或 Discovery 结果完全一致 |
| `sub` | 签发者范围内的稳定主体标识 | 用作外部身份键，不用邮箱替代 |
| `aud` | 目标客户端 | 必须包含当前 `client_id` |
| `exp` | 过期时间 | 当前时间必须早于它，并采用有限时钟偏差 |
| `iat` | 签发时间 | 检查是否明显异常 |
| `nonce` | 请求关联值 | 使用 nonce 时必须与本地值恒定时间比较 |
| `azp` | 被授权方 | 多受众等场景按 OIDC 规则校验 |

JWT 的 Base64URL 编码不是加密。任何持有者通常都能读取载荷。

## 4. Discovery 与 JWKS

RP 可以从签发者的 Discovery 文档获得端点和能力：

```text
https://id.example/.well-known/openid-configuration
```

其中 `jwks_uri` 指向公钥集合。验证时应：

1. 只信任预配置的 HTTPS 签发者；
2. 限制允许的签名算法；
3. 按 `kid` 选择密钥，并支持正常轮换；
4. 对 Discovery/JWKS 设置超时、大小限制和缓存策略；
5. 获取失败时显式失败，不接受未验证 Token。

## 5. 标准登录流程

OIDC 通常复用 Authorization Code + PKCE，并增加：

- 请求中的 `scope=openid`；
- 客户端生成和保存 `nonce`；
- Token Endpoint 返回 ID Token；
- 客户端完整验证 ID Token 后建立自己的应用 Session。

SSO 的“只登录一次”来自 OP 的中央会话，不是来自把同一个业务 Cookie 分享给所有应用。

## 6. 用户标识

外部身份的可靠主键通常是 `(iss, sub)`。邮箱、用户名和显示名称可能变化或重复，不适合作为跨系统永久主键。角色和组也应经过明确映射，而不是把任意 Claim 原样提升为本地权限。

## 7. 检查点

能解释 OAuth 和 OIDC 的边界，并列出 ID Token 至少需要验证的签名、`iss`、`aud`、`exp` 和 `nonce`。

[下一章：浏览器 SSO 与会话](04-浏览器SSO与会话.md)
