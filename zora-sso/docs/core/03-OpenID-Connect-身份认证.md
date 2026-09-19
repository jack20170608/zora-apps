# 03-OpenID-Connect-身份认证

## 概述

OpenID Connect（OIDC）是建立在 OAuth 2.0 基础上的身份认证协议，它在 OAuth 2.0 的授权能力之上增加了身份认证功能。本章将详细介绍 OIDC 的核心概念、ID Token、核心端点以及与 OAuth 2.0 的区别。

## 3.1 OIDC 概述

### 为什么需要 OIDC？

OAuth 2.0 是一个授权框架，解决的是"你能做什么"的问题，但不回答"你是谁"的问题。

```
┌─────────────────────────────────────────────────────────────────┐
│              OAuth 2.0 vs OpenID Connect                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  OAuth 2.0 (授权框架)                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  只能回答："用户授权了这个应用访问他们的资源"            │   │
│  │  不能回答："这个用户到底是谁"                            │   │
│  │  适用于：API 授权、第三方登录（但身份不明确）             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  OpenID Connect (身份认证 + 授权)                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  回答："这个用户是谁" + "用户授权了哪些权限"             │   │
│  │  在 OAuth 2.0 基础上增加了身份认证层                     │   │
│  │  适用于：单点登录、用户身份识别                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### OIDC 的核心扩展

```
┌─────────────────────────────────────────────────────────────────┐
│                    OIDC 在 OAuth 2.0 上的扩展                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  OAuth 2.0 基础组件：                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • Authorization Endpoint                                │   │
│  │  • Token Endpoint                                        │   │
│  │  • Access Token                                          │   │
│  │  • Refresh Token                                         │   │
│  │  • 授权范围 (scope)                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           +                                     │
│  OIDC 新增组件：                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • ID Token：身份令牌                                     │   │
│  │  • UserInfo Endpoint：用户信息端点                        │   │
│  │  • Discovery：服务发现                                     │   │
│  │  • 标准化 scope：openid                                   │   │
│  │  • 身份认证流程                                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### OIDC 术语对照

| OIDC 术语 | 英文 | 说明 |
|-----------|------|------|
| **身份提供者** | Identity Provider (IdP) | 负责验证用户身份的服务，如 Keycloak |
| **依赖方** | Relying Party (RP) | 使用 OIDC 登录的客户端应用 |
| **声明** | Claim | ID Token 中关于用户身份的信息 |
| **主题标识符** | Subject Identifier (sub) | 用户的唯一标识符 |

## 3.2 ID Token vs Access Token

### 核心区别

```
┌─────────────────────────────────────────────────────────────────┐
│                   ID Token vs Access Token                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ID Token                                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  用途：告诉客户端"用户是谁"                               │   │
│  │  接收方：OIDC 客户端（你的应用）                          │   │
│  │  内容：用户身份信息（sub, name, email 等）               │   │
│  │  验证：验证用户已完成登录                                 │   │
│  │  有效期：通常较短（几分钟）                               │   │
│  │  使用场景：建立应用会话、显示用户信息                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Access Token                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  用途：告诉 API"用户可以访问什么"                         │   │
│  │  接收方：资源服务器（你的后端 API）                       │   │
│  │  内容：授权信息（scope, client_id 等）                   │   │
│  │  验证：验证用户是否有权访问特定资源                       │   │
│  │  有效期：通常较短（5-60 分钟）                            │   │
│  │  使用场景：调用受保护的 API                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  重要：两者不能混用！                                           │
│  • 不要用 ID Token 调用 API                                     │
│  • 不要用 Access Token 建立应用会话                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 比喻理解

```
┌─────────────────────────────────────────────────────────────────┐
│                        生活中的比喻                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ID Token = 身份证                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • 证明你是谁                                            │   │
│  │  • 上面有照片、姓名、身份证号                             │   │
│  │  • 有效期：通常几年                                      │   │
│  │  • 用途：实名认证、办理业务                               │   │
│  │  • 对应：登录确认                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Access Token = 登机牌                                         │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • 证明你可以上哪架飞机                                   │   │
│  │  • 上面有航班号、座位号、目的地                           │   │
│  │  • 有效期：很短（几小时）                                │   │
│  │  • 用途：登机、进入候机室                                 │   │
│  │  • 对应：API 访问授权                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 错误使用场景

```
┌─────────────────────────────────────────────────────────────────┐
│                    常见错误：混用 Token                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ❌ 错误：用 ID Token 调用 API                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  GET /api/user HTTP/1.1                                  │   │
│  │  Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6... │   │
│  │                                                           │   │
│  │  问题：ID Token 的受众（aud）是客户端应用，不是 API      │   │
│  │  风险：API 无法验证 Token 的合法性                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ❌ 错误：用 Access Token 建立会话                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  // 使用 Access Token 创建 Session Cookie               │   │
│  │  cookie.setValue(accessToken);                          │   │
│  │                                                           │   │
│  │  问题：Access Token 是短期有效的，不适合作为会话标识     │   │
│  │  风险：Token 过期后用户会话失效                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ✅ 正确：分离使用                                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  // 用 ID Token 验证登录结果，建立 Session               │   │
│  │  Session session = sessionManager.create(userId);       │   │
│  │  cookie.setValue(session.getId());                      │   │
│  │                                                           │   │
│  │  // 用 Access Token 调用 API                             │   │
│  │  apiClient.setBearerToken(accessToken);                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 3.3 ID Token 结构

### JWT 格式

ID Token 是一个 JWT（JSON Web Token），由三部分组成，用 `.` 分隔：

```
┌─────────────────────────────────────────────────────────────────┐
│                       JWT 结构                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9 .                         │
│  eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4BEb2dlIiwiYWRtaW4i                         │
│ OnRydWUsImlhdCI6MTUxNjIzOTAyMn0 .                               │
│  SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c                    │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Header    │  │   Payload   │  │   Signature │            │
│  │   (头部)    │  │   (载荷)    │  │   (签名)    │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Header（头部）

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-id-123"
}
```

| 字段 | 说明 |
|------|------|
| `alg` | 签名算法，如 RS256、ES256 |
| `typ` | Token 类型，固定为 JWT |
| `kid` | 密钥 ID，用于选择验签公钥 |

### Payload（载荷）- 标准 Claims

```json
{
  "iss": "https://keycloak.example.com/realms/myrealm",
  "sub": "user-12345",
  "aud": ["my-client-id", "account"],
  "exp": 1700000000,
  "iat": 1700000000,
  "auth_time": 1699999999,
  "nonce": "abc123xyz",
  "name": "John Doe",
  "email": "john@example.com",
  "email_verified": true,
  "preferred_username": "johndoe",
  "groups": ["admin", "users"]
}
```

### 必需 Claims（OIDC 规范要求）

| Claim | 说明 | 示例 |
|-------|------|------|
| `iss` | 签发者（Issuer） | `https://keycloak.example.com/realms/myrealm` |
| `sub` | 主题标识符（Subject） | `user-12345` |
| `aud` | 受众（Audience） | `my-client-id` |
| `exp` | 过期时间（Expiration Time） | `1700000000` |
| `iat` | 签发时间（Issued At） | `1699999999` |
| `auth_time` | 认证时间（Authentication Time） | `1699999999` |

### 常用可选 Claims

| Claim | 说明 | 示例 |
|-------|------|------|
| `nonce` | 防重放随机数 | `abc123xyz` |
| `name` | 全名 | `John Doe` |
| `given_name` | 名 | `John` |
| `family_name` | 姓 | `Doe` |
| `email` | 邮箱 | `john@example.com` |
| `email_verified` | 邮箱是否验证 | `true` |
| `preferred_username` | 首选用户名 | `johndoe` |
| `groups` | 用户组 | `["admin", "users"]` |
| `roles` | 角色 | `["ROLE_ADMIN"]` |

### Signature（签名）

```
┌─────────────────────────────────────────────────────────────────┐
│                       JWT 签名验证                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  签名生成：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Signature = RS256(                                      │   │
│  │      Base64URL(Header) + "." +                          │   │
│  │      Base64URL(Payload),                                │   │
│  │      PrivateKey                                         │   │
│  │  )                                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  签名验证：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 从 ID Token 获取 kid                                 │   │
│  │  2. 从 JWKS 获取对应公钥                                 │   │
│  │  3. 使用公钥验证签名                                      │   │
│  │  4. 验证通过则 Token 合法                                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 3.4 OIDC 核心端点

### 端点概览

```
┌─────────────────────────────────────────────────────────────────┐
│                    OIDC 核心端点                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  /.well-known/openid-configuration                      │   │
│  │  • 服务发现端点，返回 OIDC 提供者配置                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           │                                     │
│            ┌──────────────┼──────────────┐                    │
│            ▼              ▼              ▼                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ Authorization│  │ Token       │  │ UserInfo    │            │
│  │  Endpoint   │  │ Endpoint    │  │ Endpoint    │            │
│  │  授权端点   │  │ 令牌端点    │  │ 用户信息端点│            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│            │              │              │                     │
│            ▼              ▼              ▼                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ 登出端点    │  │ JWKS 端点   │  │ 撤销端点    │            │
│  │ EndSession │  │ 密钥端点    │  │ Revocation  │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.4.1 Discovery 端点

OIDC 服务发现，允许客户端自动获取配置信息。

**请求：**

```
GET /.well-known/openid-configuration HTTP/1.1
Host: keycloak.example.com
```

**响应：**

```json
{
  "issuer": "https://keycloak.example.com/realms/myrealm",
  "authorization_endpoint": "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/auth",
  "token_endpoint": "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/token",
  "userinfo_endpoint": "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/userinfo",
  "jwks_uri": "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/certs",
  "end_session_endpoint": "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/logout",
  "revocation_endpoint": "https://keycloak.example.com/realms/myrealm/protocol/openid-connect/token/revoke",
  "response_types_supported": ["code", "none", "id_token", "code id_token", "code token", "id_token token", "code id_token token"],
  "subject_types_supported": ["public"],
  "id_token_signing_alg_values_supported": ["RS256", "RS384", "RS512", "ES256", "ES384", "ES512"],
  "scopes_supported": ["openid", "profile", "email", "roles"],
  "token_endpoint_auth_methods_supported": ["client_secret_basic", "client_secret_post", "private_key_jwt"],
  "claims_supported": ["sub", "iss", "auth_time", "name", "email", "email_verified", "groups"]
}
```

### 3.4.2 Authorization Endpoint（授权端点）

用于发起用户授权请求。

**请求参数：**

| 参数 | 必需 | 说明 |
|------|------|------|
| `response_type` | 是 | 授权类型，如 `code` |
| `client_id` | 是 | 客户端 ID |
| `redirect_uri` | 是 | 回调地址 |
| `scope` | 是 | 请求的权限范围，必须包含 `openid` |
| `state` | 建议 | CSRF 防护随机字符串 |
| `nonce` | 建议 | 防重放随机字符串 |
| `code_challenge` | PKCE | PKCE code challenge |
| `code_challenge_method` | PKCE | PKCE 方法，如 `S256` |

**请求示例：**

```
GET /realms/myrealm/protocol/openid-connect/auth?
  response_type=code&
  client_id=my-client&
  redirect_uri=https://myapp.com/callback&
  scope=openid%20profile%20email&
  state=xyz123&
  nonce=abc456&
  code_challenge=abc123...&
  code_challenge_method=S256
HTTP/1.1
Host: keycloak.example.com
```

### 3.4.3 Token Endpoint（令牌端点）

用于兑换授权码、刷新令牌等。

**请求（Authorization Code 兑换）：**

```
POST /realms/myrealm/protocol/openid-connect/token HTTP/1.1
Host: keycloak.example.com
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
code=SplxlOBeZQQYbYS6WxSbIA&
redirect_uri=https://myapp.com/callback&
client_id=my-client&
client_secret=secret&
code_verifier=abc123...
```

**响应：**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "scope": "openid profile email"
}
```

**请求（Refresh Token 刷新）：**

```
POST /realms/myrealm/protocol/openid-connect/token HTTP/1.1
Host: keycloak.example.com
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&
refresh_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...&
client_id=my-client&
client_secret=secret
```

### 3.4.4 UserInfo Endpoint（用户信息端点）

用于获取更详细的用户信息。

**请求：**

```
GET /realms/myrealm/protocol/openid-connect/userinfo HTTP/1.1
Host: keycloak.example.com
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应：**

```json
{
  "sub": "user-12345",
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "email": "john@example.com",
  "email_verified": true,
  "preferred_username": "johndoe"
}
```

### 3.4.5 JWKS Endpoint（密钥端点）

提供用于验证 Token 签名的公钥。

**请求：**

```
GET /realms/myrealm/protocol/openid-connect/certs HTTP/1.1
Host: keycloak.example.com
```

**响应：**

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "key-id-123",
      "alg": "RS256",
      "n": "0vx7agoebGcQSuuPiLJXZpt... (RSA modulus)",
      "e": "AQab (RSA exponent)"
    }
  ]
}
```

## 3.5 OIDC 登录流程完整示例

### 完整流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                    完整 OIDC 登录流程                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 发起登录                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  用户访问受保护页面                                       │   │
│  │  应用发现未登录，重定向到授权服务器                        │   │
│  │  生成 state、nonce、code_verifier                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▼                                     │
│  2. 授权请求                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  GET /auth?response_type=code&client_id=xxx&            │   │
│  │          redirect_uri=xxx&scope=openid%20profile&       │   │
│  │          state=xxx&nonce=xxx&code_challenge=xxx&        │   │
│  │          code_challenge_method=S256                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▼                                     │
│  3. 用户认证                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  用户在授权服务器登录（输入密码或使用 MFA）               │   │
│  │  授权服务器创建 SSO 会话                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▼                                     │
│  4. 授权确认                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  用户同意授权（可选：自定义同意页面）                     │   │
│  │  授权服务器生成授权码                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▼                                     │
│  5. 回调                                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  302 Location: https://myapp.com/callback?              │   │
│  │             code=xxx&state=xxx&iss=xxx                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▼                                     │
│  6. 兑换 Token                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  POST /token                                            │   │
│  │  grant_type=authorization_code&code=xxx&               │   │
│  │  client_id=xxx&code_verifier=xxx                        │   │
│  │                                                           │   │
│  │  返回: {id_token, access_token, refresh_token}          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▼                                     │
│  7. 验证 ID Token                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • 验证签名（使用 JWKS 公钥）                             │   │
│  │  • 验证 iss 匹配预期 issuer                              │   │
│  │  • 验证 aud 包含当前 client_id                           │   │
│  │  • 验证 exp 未过期                                       │   │
│  │  • 验证 nonce 匹配登录请求中的值                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                           ▼                                     │
│  8. 建立会话                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • 创建应用 Session                                      │   │
│  │  • 生成新的 Session ID                                    │   │
│  │  • 设置 HttpOnly Session Cookie                         │   │
│  │  • 跳转回原始请求页面                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 登录请求示例

```java
// 构造 OIDC 授权请求
public class OIDCAuthRequest {
    
    public static String buildAuthUrl(String authEndpoint, 
                                      String clientId, 
                                      String redirectUri,
                                      String state, 
                                      String nonce,
                                      String codeChallenge,
                                      String codeChallengeMethod) {
        
        Map<String, String> params = new HashMap<>();
        params.put("response_type", "code");
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("scope", "openid profile email");
        params.put("state", state);
        params.put("nonce", nonce);
        params.put("code_challenge", codeChallenge);
        params.put("code_challenge_method", codeChallengeMethod);
        
        // 构建 URL
        StringBuilder url = new StringBuilder(authEndpoint);
        url.append("?");
        params.forEach((k, v) -> url.append(k).append("=")
            .append(URLEncoder.encode(v, StandardCharsets.UTF_8))
            .append("&"));
        
        return url.toString();
    }
}
```

### 回调处理示例

```java
// 处理 OIDC 回调
public class OIDCCallbackHandler {
    
    public AuthenticationResult handleCallback(String code, 
                                                String state, 
                                                String iss,
                                                String expectedState,
                                                String expectedIssuer,
                                                String codeVerifier) {
        
        // 1. 验证 state
        if (!expectedState.equals(state)) {
            throw new SecurityException("State mismatch");
        }
        
        // 2. 验证 issuer
        if (!expectedIssuer.equals(iss)) {
            throw new SecurityException("Issuer mismatch");
        }
        
        // 3. 使用授权码兑换 Token
        TokenResponse tokens = exchangeCodeForTokens(code, codeVerifier);
        
        // 4. 验证 ID Token
        IDToken idToken = validateIDToken(tokens.getIdToken());
        
        // 5. 返回认证结果
        return new AuthenticationResult(idToken, tokens.getAccessToken());
    }
    
    private IDToken validateIDToken(String idTokenStr) {
        // 1. 解析 JWT
        // 2. 获取 kid
        // 3. 从 JWKS 获取公钥
        // 4. 验证签名
        // 5. 验证 iss, aud, exp, nonce
        // 6. 返回解析后的 ID Token
    }
}
```

## 3.6 Scope 详解

### 标准 Scope

| Scope | 说明 | 对应 Claims |
|-------|------|-------------|
| `openid` | 必选，标识 OIDC 请求 | `sub`, `iss` |
| `profile` | 用户基本信息 | `name`, `family_name`, `given_name`, `picture` |
| `email` | 邮箱信息 | `email`, `email_verified` |
| `phone` | 电话信息 | `phone_number`, `phone_number_verified` |
| `address` | 地址信息 | `address` |
| `offline_access` | 离线访问，返回 Refresh Token | - |

### Keycloak 特有 Scope

| Scope | 说明 |
|-------|------|
| `roles` | 返回用户角色 |
| `groups` | 返回用户组 |
| `microprofile-jwt` | 返回 MP-JWT 特定声明 |

## 3.7 本章小结

### 核心要点

```
┌─────────────────────────────────────────────────────────────────┐
│                      本章核心要点                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. OIDC 与 OAuth 2.0 的关系                                    │
│     • OAuth 2.0：授权框架，解决"能做什么"                       │
│     • OIDC：身份认证协议，解决"是谁"                            │
│     • OIDC 在 OAuth 2.0 基础上增加身份认证                     │
│                                                                 │
│  2. ID Token vs Access Token                                    │
│     • ID Token：身份令牌，告诉客户端"用户是谁"                  │
│     • Access Token：访问令牌，告诉 API"能访问什么"             │
│     • 两者不能混用                                              │
│                                                                 │
│  3. ID Token 结构                                               │
│     • Header：算法、类型、密钥 ID                               │
│     • Payload：标准 Claims (iss, sub, aud, exp, iat)          │
│     • Signature：使用私钥签名，公钥验证                         │
│                                                                 │
│  4. OIDC 核心端点                                               │
│     • Discovery：服务发现                                       │
│     • Authorization Endpoint：授权端点                          │
│     • Token Endpoint：令牌端点                                  │
│     • UserInfo Endpoint：用户信息端点                           │
│     • JWKS Endpoint：密钥端点                                   │
│                                                                 │
│  5. 登录流程                                                    │
│     • 发起授权请求 → 用户登录 → 回调 → 兑换 Token →            │
│     • 验证 ID Token → 建立会话                                  │
│                                                                 │
│  6. Scope                                                       │
│     • openid：必选                                              │
│     • profile：基本信息                                         │
│     • email：邮箱信息                                           │
│     • offline_access：获取 Refresh Token                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 课后思考

1. 为什么 OIDC 需要 `openid` 这个 Scope？
2. ID Token 中的 `nonce` 有什么作用？为什么需要它？
3. 为什么不能把 ID Token 当作 Access Token 使用？
4. JWKS 中的 `kid` 字段有什么用？

## 3.8 实践任务

### 任务 1：使用 Keycloak 测试 OIDC 流程

```bash
# 1. 发起授权请求（浏览器中打开）
# 替换参数后访问
https://keycloak.example.com/realms/myrealm/protocol/openid-connect/auth?
  response_type=code&
  client_id=my-client&
  redirect_uri=http://localhost:8080/callback&
  scope=openid%20profile%20email&
  state=random-state&
  nonce=random-nonce&
  code_challenge=CHALLENGE&
  code_challenge_method=S256
```

### 任务 2：解析和验证 ID Token

```java
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;

public class IDTokenValidator {
    
    public static JWTClaimsSet validate(String idToken, 
                                         String expectedIssuer,
                                         String expectedAudience,
                                         String expectedNonce,
                                         RSAKey rsaKey) throws Exception {
        
        // 1. 解析 JWT
        SignedJWT signedJWT = SignedJWT.parse(idToken);
        
        // 2. 验证签名
        RSASSAVerifier verifier = new RSASSAVerifier(rsaKey);
        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Invalid signature");
        }
        
        // 3. 获取 Claims
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        
        // 4. 验证必需 Claims
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new SecurityException("Invalid issuer");
        }
        
        if (!claims.getAudience().contains(expectedAudience)) {
            throw new SecurityException("Invalid audience");
        }
        
        if (claims.getExpirationTime().before(new Date())) {
            throw new SecurityException("Token expired");
        }
        
        if (expectedNonce != null && 
            !expectedNonce.equals(claims.getClaim("nonce"))) {
            throw new SecurityException("Invalid nonce");
        }
        
        return claims;
    }
}
```

### 任务 3：实现 OIDC Discovery 客户端

```java
public class OIDCDiscoveryClient {
    
    public static OIDCProviderMetadata discover(String issuer) throws Exception {
        // 1. 构建 Discovery URL
        URL discoveryUrl = new URL(issuer + "/.well-known/openid-configuration");
        
        // 2. 发起请求
        HttpURLConnection conn = (HttpURLConnection) discoveryUrl.openConnection();
        conn.setRequestMethod("GET");
        
        // 3. 解析响应
        try (InputStream is = conn.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(is, OIDCProviderMetadata.class);
        }
    }
}
```

## 下章预告

下一章我们将学习 **SSO 安全机制与最佳实践**，深入探讨 OAuth 2.0/OIDC 实施中的安全要点和常见攻击防护。我们将详细讲解：
- CSRF 攻击与 state 参数防护
- Token 重放攻击与 nonce 防护
- PKCE 原理与实现
- 开放重定向攻击防护
- JWT 算法混淆攻击防护
- 会话安全最佳实践