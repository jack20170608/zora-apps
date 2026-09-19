# 02-OAuth-2.0-授权框架基础

## 概述

OAuth 2.0（Open Authorization 2.0）是一个授权框架，用于授权第三方应用访问用户资源而不需要暴露用户密码。本章将深入介绍 OAuth 2.0 的核心概念、授权流程和安全机制。

## 2.1 OAuth 2.0 概述

### 解决的问题

在 OAuth 2.0 出现之前，如果第三方应用需要访问用户的资源（如邮箱联系人、照片等），用户需要提供用户名和密码：

```
┌─────────────────────────────────────────────────────────────────┐
│                    传统方式的安全问题                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用户 ──▶ 第三方应用 ──▶ 资源服务器                              │
│              │                                                  │
│              │  提供用户名和密码                                 │
│              └────────────────────────────────▶ 危险！           │
│                                                               │
│  问题：                                                          │
│  • 第三方应用存储了用户密码                                       │
│  • 第三方应用获得了完整账号权限                                   │
│  • 无法撤销单个第三方应用的访问权限                               │
│  • 密码泄露影响所有关联服务                                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### OAuth 2.0 的解决方案

```
┌─────────────────────────────────────────────────────────────────┐
│                    OAuth 2.0 授权方式                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用户 ──▶ 第三方应用 ──▶ 授权服务器 ──▶ 资源服务器               │
│              │              │              │                    │
│              │              │              │                    │
│              │              │  获得访问令牌  │                    │
│              │              │◀───────────── │                    │
│              │              │              │                    │
│              │      资源所有者授权         │                    │
│              │◀───────────── │              │                    │
│              │              │              │                    │
│              │  使用令牌访问资源           │                    │
│              └─────────────────────────────▶│                    │
│                                                                 │
│  优势：                                                          │
│  • 用户无需提供密码                                              │
│  • 可以细粒度控制权限                                            │
│  • 可以随时撤销访问权限                                          │
│  • 密码不会被第三方应用获取                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 核心角色

| 角色 | 英文 | 说明 | 示例 |
|------|------|------|------|
| **资源所有者** | Resource Owner | 授权访问自己资源的用户 | 用户本人 |
| **客户端** | Client | 请求访问资源的第三方应用 | 知乎、GitHub 第三方登录 |
| **授权服务器** | Authorization Server | 验证用户身份并颁发令牌 | Google、GitHub |
| **资源服务器** | Resource Server | 存储和保护用户资源 | Google Drive、GitHub API |

### 适用场景

| 场景 | 说明 |
|------|------|
| **第三方登录** | 使用 Google、GitHub 账号登录第三方应用 |
| **API 授权** | 第三方应用访问用户的数据 API |
| **微服务授权** | 微服务之间安全调用 |
| **SSO 单点登录** | 跨应用共享认证状态 |

## 2.2 四种授权方式（Grant Type）

OAuth 2.0 定义了四种授权方式，用于不同场景。

### 2.2.1 Authorization Code（授权码模式）

**最安全、最推荐的方式**，适用于有后端的 Web 应用。

```
┌─────────────────────────────────────────────────────────────────┐
│                 Authorization Code Flow                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  客户端                    授权服务器          资源服务器        │
│     │                         │                   │             │
│     │──── 1. 授权请求 ──────▶│                   │             │
│     │     (client_id,        │                   │             │
│     │      redirect_uri,     │                   │             │
│     │      scope, state)     │                   │             │
│     │                         │                   │             │
│     │◀── 2. 重定向 ──────────│                   │             │
│     │       (code)           │                   │             │
│     │                         │                   │             │
│     │──── 3. 兑换令牌 ──────▶│                   │             │
│     │     (code, client_id,  │                   │             │
│     │      client_secret)    │                   │             │
│     │                         │                   │             │
│     │◀── 4. 返回令牌 ────────│                   │             │
│     │     (access_token,     │                   │             │
│     │      refresh_token)    │                   │             │
│     │                         │                   │             │
│     │                         │──── 5. 访问 ────▶│             │
│     │                         │     (access_token)             │
│     │                         │                   │             │
│     │                         │◀── 6. 资源 ──────│             │
│     │                         │                   │             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**流程详解：**

1. **用户发起授权**：用户点击"使用 Google 登录"
2. **重定向到授权服务器**：携带 client_id、redirect_uri、scope、state
3. **用户登录并授权**：用户在授权服务器登录并同意授权
4. **携带授权码回调**：授权服务器携带授权码重定向回客户端
5. **后端兑换令牌**：客户端使用授权码换取 Access Token
6. **使用令牌访问资源**：客户端使用令牌访问资源服务器

**安全性特点：**
- 授权码在浏览器传输，Access Token 只在后端传输
- 需要 client_secret 验证客户端身份
- 支持 PKCE 增强安全性

### 2.2.2 Implicit（隐式模式）

**已不推荐使用**，适用于纯前端 SPA 应用。

```
┌─────────────────────────────────────────────────────────────────┐
│                      Implicit Flow                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  客户端                    授权服务器                          │
│     │                         │                                │
│     │──── 1. 授权请求 ──────▶│                                │
│     │                         │                                │
│     │◀── 2. 重定向 ──────────│                                │
│     │       (access_token    │                                │
│     │        in fragment)    │                                │
│     │                         │                                │
│                                                                 │
│  风险：                                                          │
│  • Token 暴露在 URL 中                                          │
│  • 无法验证客户端身份                                            │
│  • 无法刷新 Token                                               │
│  • 已不推荐使用                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**为什么被废弃？**
- Access Token 直接暴露在 URL 中
- 无法使用 client_secret 验证客户端
- 无法获取 Refresh Token
- 容易受到 Token 泄露攻击

### 2.2.3 Resource Owner Password Credentials（密码模式）

**仅适用于高度可信的第一方应用**。

```
┌─────────────────────────────────────────────────────────────────┐
│              Password Credentials Flow                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  客户端                    授权服务器                          │
│     │                         │                                │
│     │──── 1. 发送用户名密码 ──▶│                                │
│     │     (username,          │                                │
│     │      password,          │                                │
│     │      client_id)         │                                │
│     │                         │                                │
│     │◀── 2. 返回令牌 ────────│                                │
│     │     (access_token,     │                                │
│     │      refresh_token)    │                                │
│     │                         │                                │
│                                                                 │
│  限制：                                                          │
│  • 仅适用于第一方应用                                            │
│  • 用户需要直接提供密码给客户端                                  │
│  • 无法复用授权服务器的 MFA 等安全措施                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**何时可以使用？**
- 应用是官方第一方应用
- 用户完全信任该应用
- 没有更好的替代方案

### 2.2.4 Client Credentials（客户端凭据模式）

**适用于服务端到服务端的授权**。

```
┌─────────────────────────────────────────────────────────────────┐
│                Client Credentials Flow                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  客户端                    授权服务器                          │
│     │                         │                                │
│     │──── 1. 发送凭据 ──────▶│                                │
│     │     (client_id,        │                                │
│     │      client_secret)    │                                │
│     │                         │                                │
│     │◀── 2. 返回令牌 ────────│                                │
│     │     (access_token)     │                                │
│     │                         │                                │
│                                                                 │
│  场景：                                                          │
│  • 后端服务访问自己的 API                                        │
│  • 机器对机器通信                                                │
│  • 没有用户参与                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 授权方式对比

| 特性 | Authorization Code | Implicit | Password Credentials | Client Credentials |
|------|-------------------|----------|---------------------|-------------------|
| 安全性 | ★★★★★ | ★★☆☆☆ | ★★★☆☆ | ★★★★☆ |
| 适用场景 | Web 后端 | 已废弃 | 第一方应用 | 服务端 |
| Token 位置 | 后端 | 前端 URL | 后端 | 后端 |
| Refresh Token | ✓ | ✗ | ✓ | ✗ |
| 需要用户交互 | ✓ | ✓ | ✓ | ✗ |
| PKCE 支持 | ✓ | ✗ | ✗ | ✗ |

## 2.3 Authorization Code + PKCE 详解

### 什么是 PKCE？

PKCE（Proof Key for Code Exchange，代码交换证明密钥）是对 Authorization Code 流程的扩展，用于防止授权码被截获后冒用。

```
┌─────────────────────────────────────────────────────────────────┐
│                    PKCE 解决的问题                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  攻击场景：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 攻击者诱骗用户访问恶意应用                            │   │
│  │  2. 恶意应用发起授权请求到授权服务器                      │   │
│  │  3. 用户在授权服务器完成登录                              │   │
│  │  4. 授权服务器携带授权码回调到恶意应用                    │   │
│  │  5. 恶意应用使用授权码兑换 Access Token                  │   │
│  │  6. 攻击者窃取了用户的授权码并冒充用户                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  PKCE 解决方案：                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 客户端生成随机 secret：code_verifier                 │   │
│  │  2. 客户端计算 challenge：code_challenge                 │   │
│  │  3. 授权请求时携带 code_challenge                        │   │
│  │  4. 兑换令牌时必须提供 code_verifier                      │   │
│  │  5. 授权服务器验证 code_verifier = f(code_challenge)    │   │
│  │  6. 只有原始请求者才能完成令牌兑换                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### PKCE 流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    PKCE 完整流程                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  客户端                                                      │
│     │                                                          │
│     │  1. 生成 code_verifier (43-128 字符随机字符串)          │
│     │     code_verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r"    │
│     │                                                          │
│     │  2. 计算 code_challenge                                  │
│     │     • S256: base64url(SHA256(code_verifier))            │
│     │     • plain: code_verifier (不推荐)                     │
│     │     code_challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8UR"  │
│     │                                                          │
│     │──── 3. 授权请求 ─────────────────────────────────────▶│  │
│     │     client_id=xxx                                        │  │
│     │     redirect_uri=xxx                                    │  │
│     │     code_challenge=xxx                                  │  │
│     │     code_challenge_method=S256                          │  │
│     │     state=xxx                                           │  │
│     │                                                          │  │
│     │◀── 4. 回调 ──────────────────────────────────────────│  │
│     │     code=xxx&state=xxx                                  │  │
│     │                                                          │  │
│     │──── 5. 兑换令牌 ─────────────────────────────────────▶│  │
│     │     code=xxx                                            │  │
│     │     client_id=xxx                                       │  │
│     │     code_verifier=xxx  ◀── 必须提供此参数               │  │
│     │                                                          │  │
│     │◀── 6. 返回令牌 ───────────────────────────────────────│  │
│     │     access_token=xxx                                    │  │
│     │                                                          │  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### code_challenge 计算示例

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PKCETest {
    
    // 生成 code_verifier
    public static String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    // 使用 S256 方法计算 code_challenge
    public static String generateCodeChallengeS256(String codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
    
    public static void main(String[] args) throws Exception {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallengeS256(codeVerifier);
        
        System.out.println("code_verifier: " + codeVerifier);
        System.out.println("code_challenge: " + codeChallenge);
        System.out.println("code_challenge_method: S256");
    }
}
```

### 为什么 Authorization Code + PKCE 是最佳选择？

```
┌─────────────────────────────────────────────────────────────────┐
│               Authorization Code + PKCE 优势                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 安全性高                                                     │
│     • 即使授权码泄露，攻击者也无法兑换令牌                        │
│     • Token 只在后端传输，不暴露给浏览器                        │
│     • 支持 Proof Key 验证客户端身份                             │
│                                                                 │
│  2. 适用广泛                                                     │
│     • 传统 Web 应用                                             │
│     • SPA (Single Page Application)                            │
│     • 移动应用                                                   │
│     • 服务端应用                                                 │
│                                                                 │
│  3. 标准化                                                       │
│     • RFC 7636 规范                                             │
│     • 所有主流授权服务器支持                                    │
│     • 所有现代客户端库支持                                      │
│                                                                 │
│  4. 现代最佳实践                                                 │
│     • OAuth 2.0 Security Best Current Practice (RFC 9700)      │
│     • 唯一推荐的公开客户端授权方式                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 2.4 Token 类型详解

### 2.4.1 Access Token

**用于访问受保护资源的凭证**。

```
┌─────────────────────────────────────────────────────────────────┐
│                       Access Token                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用途：                                                          │
│  • 访问受保护的 API 资源                                        │
│  • 每次 API 请求都需要携带                                       │
│                                                                 │
│  特点：                                                          │
│  • 短期有效（通常 5 分钟到 1 小时）                              │
│  • 不包含用户信息，只包含授权信息                                │
│  • 需要保密，不应暴露给前端                                      │
│                                                                 │
│  示例请求：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  GET /api/user/profile HTTP/1.1                         │   │
│  │  Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6... │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4.2 Refresh Token

**用于获取新的 Access Token**。

```
┌─────────────────────────────────────────────────────────────────┐
│                      Refresh Token                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用途：                                                          │
│  • 在 Access Token 过期后获取新的 Access Token                  │
│  • 避免用户频繁重新登录                                          │
│                                                                 │
│  特点：                                                          │
│  • 长期有效（通常数天到数周）                                    │
│  • 需要安全存储                                                 │
│  • 可以撤销                                                     │
│                                                                 │
│  刷新流程：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  客户端 ──▶ 授权服务器: POST /token                      │   │
│  │  grant_type=refresh_token&refresh_token=xxx             │   │
│  │                                                           │   │
│  │  授权服务器 ──▶ 客户端:                                   │   │
│  │  {access_token: "xxx", expires_in: 3600}                │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  注意：                                                          │
│  • 密码模式通常不返回 Refresh Token                             │
│  • 客户端凭据模式不返回 Refresh Token                           │
│  • 公开客户端（ SPA、移动端）可以使用 Refresh Token             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4.3 Token 响应示例

```json
{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 3600,
    "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "scope": "read write",
    "token_type": "Bearer"
}
```

## 2.5 OAuth 2.0 安全最佳实践

### 2.5.1 必须使用的安全措施

```
┌─────────────────────────────────────────────────────────────────┐
│                    OAuth 2.0 安全清单                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ☑ 使用 HTTPS                                                   │
│    • 所有 OAuth 通信必须使用 HTTPS                              │
│    • 防止 Token 和凭据被窃听                                    │
│                                                                 │
│  ☑ 使用 Authorization Code + PKCE                              │
│    • 永远不要使用 Implicit 模式                                │
│    • 公开客户端必须使用 PKCE                                   │
│                                                                 │
│  ☑ 验证 redirect_uri                                            │
│    • 严格匹配预注册的回调地址                                   │
│    • 禁止使用开放重定向                                         │
│                                                                 │
│  ☑ 使用 state 参数                                              │
│    • 防止 CSRF 攻击                                             │
│    • 验证回调的来源                                             │
│                                                                 │
│  ☑ 短期 Access Token                                            │
│    • 设置合理的过期时间                                         │
│    • 使用 Refresh Token 续期                                   │
│                                                                 │
│  ☑ 安全的 Token 存储                                            │
│    • 服务器端存储                                               │
│    • 浏览器使用 HttpOnly Cookie                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.5.2 禁止的做法

```
┌─────────────────────────────────────────────────────────────────┐
│                    OAuth 2.0 禁止行为                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✗ 不使用 HTTPS                                                 │
│  ✗ 使用 Implicit 模式                                           │
│  ✗ 在前端存储 Access Token                                      │
│  ✗ 使用 password grant                                          │
│  ✗ 使用 client_secret 处理公开客户端                            │
│  ✗ 允许开放重定向                                               │
│  ✗ 不验证 state 参数                                            │
│  ✗ 使用 long-lived Access Token                                 │
│  ✗ 在 URL 中传递 Token                                          │
│  ✗ 记录 Token 到日志                                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 2.6 本章小结

### 核心要点

```
┌─────────────────────────────────────────────────────────────────┐
│                      本章核心要点                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. OAuth 2.0 核心角色                                          │
│     • Resource Owner：用户                                      │
│     • Client：第三方应用                                        │
│     • Authorization Server：授权服务器                          │
│     • Resource Server：资源服务器                               │
│                                                                 │
│  2. 四种授权方式                                                │
│     • Authorization Code：推荐使用                              │
│     • Implicit：已废弃                                          │
│     • Password Credentials：仅第一方                            │
│     • Client Credentials：服务端到服务端                        │
│                                                                 │
│  3. Authorization Code + PKCE                                   │
│     • 防止授权码被截获后冒用                                     │
│     • code_verifier: 客户端生成的随机字符串                     │
│     • code_challenge: code_verifier 的哈希值                   │
│     • S256 方法是强制要求                                       │
│                                                                 │
│  4. Token 类型                                                  │
│     • Access Token：访问资源                                    │
│     • Refresh Token：刷新访问令牌                               │
│                                                                 │
│  5. 安全最佳实践                                                │
│     • 必须使用 HTTPS                                            │
│     • 使用 Authorization Code + PKCE                            │
│     • 验证 redirect_uri                                         │
│     • 使用 state 参数                                           │
│     • 短期 Access Token                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 课后思考

1. 为什么 Authorization Code 模式比 Implicit 模式更安全？
2. PKCE 如何防止授权码被截获后冒用？
3. 什么场景下应该使用 Refresh Token？
4. 为什么不能在 URL 中传递 Access Token？

## 2.7 实践任务

### 任务 1：使用 Postman 模拟 Authorization Code 流程

```bash
# 1. 发起授权请求
# 将以下 URL 放入浏览器（替换实际参数）
https://authorization-server.com/auth?
  response_type=code&
  client_id=your-client-id&
  redirect_uri=https://your-app.com/callback&
  scope=read write&
  state=random-state-string&
  code_challenge=your-code-challenge&
  code_challenge_method=S256
```

### 任务 2：实现 PKCE 工具类

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE 工具类
 * 参考：RFC 7636
 */
public class PKCEUtil {
    
    private static final String CHARACTERS = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    
    /**
     * 生成 code_verifier
     * 长度：43-128 字符
     */
    public static String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < 64; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
    
    /**
     * 使用 S256 方法生成 code_challenge
     */
    public static String generateCodeChallengeS256(String codeVerifier) 
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
```

## 下章预告

下一章我们将学习 **OpenID Connect (OIDC)**，这是建立在 OAuth 2.0 之上的身份认证协议，用于解决"用户是谁"的问题。我们将深入探讨：
- OIDC 与 OAuth 2.0 的区别
- ID Token 的结构和验证
- OIDC 核心端点
- UserInfo 端点
- Discovery 机制