# SSO 单点登录技术学习计划

## 概述

本学习计划面向需要实现单点登录功能的开发人员，从基础概念到实战项目，系统性地介绍 SSO 相关技术。内容按照从浅入深的顺序编排，每个章节都有明确的学习目标和配套资源。

## 技术要点全景图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SSO 技术知识体系                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐        │
│  │   基础概念层     │    │   协议层        │    │   安全专题层     │        │
│  │                 │    │                 │    │                 │        │
│  │  • 认证 vs 授权  │    │  • OAuth 2.0   │    │  • CSRF 防护    │        │
│  │  • Session/Cookie│    │  • OIDC        │    │  • Token 安全   │        │
│  │  • HTTP 重定向   │    │  • PKCE        │    │  • 重放攻击防护  │        │
│  │  • JWT 基础     │    │  • Grant Types │    │  • XSS 防护     │        │
│  └────────┬────────┘    └────────┬────────┘    └────────┬────────┘        │
│           │                       │                       │                 │
│           └───────────────────────┼───────────────────────┘                 │
│                                   ▼                                          │
│                      ┌────────────────────────┐                             │
│                      │       实践层            │                             │
│                      │                        │                             │
│                      │  • Keycloak 部署配置   │                             │
│                      │  • zora-sso 实现      │                             │
│                      │  • 客户端集成          │                             │
│                      └────────────────────────┘                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 技术要点详细说明

### 1. 基础概念层

| 知识点 | 说明 | 重要程度 |
|--------|------|----------|
| 认证 vs 授权 | 认证回答"你是谁"，授权回答"你能做什么" | ★★★★★ |
| Session 机制 | 服务端会话管理，Session ID 存储在 Cookie 中 | ★★★★★ |
| Cookie 机制 | 浏览器存储会话标识的机制，包含安全属性 | ★★★★★ |
| HTTP 重定向 | 302/303 跳转是 OAuth 流程的核心机制 | ★★★★☆ |
| JWT 基础 | JSON Web Token 的结构和 claims | ★★★★★ |
| HTTPS 重要性 | 保护传输层数据安全 | ★★★★★ |

### 2. 协议层

| 知识点 | 说明 | 重要程度 |
|--------|------|----------|
| OAuth 2.0 | 授权框架，理解四种 Grant Type | ★★★★★ |
| Authorization Code Flow | 最安全的授权码流程 | ★★★★★ |
| PKCE | 防止授权码被截获的机制 | ★★★★★ |
| OpenID Connect | 在 OAuth 2.0 上构建的身份认证协议 | ★★★★★ |
| ID Token | 身份令牌，验证登录结果 | ★★★★★ |
| Access Token | 访问令牌，调用 API 的凭证 | ★★★★★ |
| Refresh Token | 刷新令牌，获取新的 Access Token | ★★★★☆ |
| Discovery | OIDC 服务发现，自动获取配置 | ★★★★☆ |
| JWKS | JSON Web Key Set，验证 Token 签名 | ★★★★☆ |
| State 参数 | 防止 CSRF 攻击 | ★★★★★ |
| Nonce 参数 | 防止 Token 重放 | ★★★★☆ |

### 3. 安全专题层

| 知识点 | 说明 | 重要程度 |
|--------|------|----------|
| CSRF 防护 | 使用 state 参数和 CSRF Token | ★★★★★ |
| Token 存储 | 浏览器存储 Access Token 的方式 | ★★★★☆ |
| 重放攻击防护 | Authorization Code 一次性使用 | ★★★★★ |
| Token 窃取 | XSS 防护和 Token 过期策略 | ★★★★☆ |
| 开放重定向 | 防止恶意回调 URL | ★★★★☆ |
| 算法混淆攻击 | 禁止使用 alg=none | ★★★★☆ |
| 密钥轮换 | JWKS 密钥更新策略 | ★★★☆☆ |

### 4. Keycloak 专题

| 知识点 | 说明 | 重要程度 |
|--------|------|----------|
| Realm 概念 | 独立的身份管理空间 | ★★★★☆ |
| Client 配置 | 客户端类型和回调 URL | ★★★★☆ |
| User 管理 | 用户创建、角色分配 | ★★★★☆ |
| Role 和 Scope | 角色定义和权限范围 | ★★★★☆ |
| Token 配置 | Token 过期时间、刷新策略 | ★★★★☆ |
| MFA 配置 | 多因素认证设置 | ★★★☆☆ |
| Admin API | 编程方式管理 Keycloak | ★★★☆☆ |

## 学习计划

### 阶段一：基础入门（约 2-3 天）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  00-前置知识基础                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  学习目标：理解 Web 认证基本机制，为后续学习打下基础                         │
│                                                                             │
│  1.1 认证与授权的区别                                                        │
│      - 认证（Authentication）：验证用户身份                                 │
│      - 授权（Authorization）：决定用户能做什么                              │
│      - 两者关系：先认证后授权                                               │
│                                                                             │
│  1.2 Session 与 Cookie 机制                                                 │
│      - Session：服务端存储用户状态                                          │
│      - Cookie：浏览器存储会话标识                                           │
│      - Session Cookie vs 普通 Cookie                                        │
│      - HttpOnly、Secure、SameSite 属性                                      │
│                                                                             │
│  1.3 HTTP 重定向与状态码                                                     │
│      - 301/302/303/307/308 重定向                                           │
│      - Location 响应头                                                       │
│      - 浏览器如何处理重定向                                                  │
│                                                                             │
│  1.4 HTTPS 基础                                                             │
│      - TLS/SSL 握手过程                                                     │
│      - 证书验证                                                              │
│      - 为什么 OAuth 必须使用 HTTPS                                          │
│                                                                             │
│  推荐资源：                                                                  │
│  - MDN Web Docs: HTTP cookies                       │
│  - RFC 6265: HTTP State Management Mechanism        │
│  - 《HTTP 权威指南》相关章节                          │
│                                                                             │
│  实践任务：                                                                  │
│  - 编写一个简单的 Session 管理示例                                          │
│  - 理解 Cookie 各属性的作用                                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 阶段二：OAuth 2.0 入门（约 2-3 天）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  01-OAuth-2.0-授权框架基础                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  学习目标：理解 OAuth 2.0 的核心概念和授权流程                               │
│                                                                             │
│  2.1 OAuth 2.0 概述                                                         │
│      - 解决的问题：第三方应用访问用户资源                                    │
│      - 核心角色：Resource Owner、Client、Resource Server、Authorization    │
│      - 适用场景：第三方登录、API 授权                                       │
│                                                                             │
│  2.2 四种授权方式（Grant Type）                                             │
│      - Authorization Code：最安全，推荐使用                                 │
│      - Implicit：已不推荐使用                                               │
│      - Resource Owner Password Credentials：不推荐                         │
│      - Client Credentials：服务端到服务端                                   │
│                                                                             │
│  2.3 Authorization Code Flow 详解                                          │
│      ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐          │
│      │  用户   │────▶│ 客户端  │────▶│ 授权服务器│────▶│资源服务器│          │
│      └─────────┘     └─────────┘     └─────────┘     └─────────┘          │
│           │              │               │               │                │
│           │              │               │               │                │
│           ▼              ▼               ▼               ▼                │
│      1.点击登录    2.重定向到     3.用户登录      4.返回授权码             │
│                     授权服务器                                                       │
│                                                                             │
│           │              │               │               │                │
│           │              ▼               ▼               ▼                │
│           │         5.携带授权码    6.验证授权码     7.返回令牌             │
│           │            回调           兑换令牌                          │
│           │              │               │               │                │
│           ▼              ▼               ▼               ▼                │
│      8.携带令牌    9.携带令牌访问   10.验证令牌      11.返回资源           │
│         访问资源      资源服务器       授权                                               │
│                                                                             │
│  2.4 Token 类型                                                             │
│      - Access Token：访问资源的凭证                                         │
│      - Refresh Token：刷新 Access Token                                    │
│      - Token 有效期设置                                                      │
│                                                                             │
│  2.5 为什么要使用 PKCE                                                      │
│      - PKCE 解决的问题：防止授权码被截获后冒用                              │
│      - code_verifier：客户端生成的随机字符串                                │
│      - code_challenge：code_verifier 的哈希值                               │
│      - S256 vs plain 方法                                                   │
│                                                                             │
│  推荐资源：                                                                  │
│  - RFC 6749: OAuth 2.0 Authorization Framework                             │
│  - RFC 7636: PKCE                                                          │
│  - OAuth 2.0 官方文档: https://oauth.net/2/                                │
│                                                                             │
│  实践任务：                                                                  │
│  - 使用 Postman 模拟 Authorization Code 流程                               │
│  - 手动实现 PKCE 流程                                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 阶段三：OpenID Connect 入门（约 2-3 天）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  02-OpenID-Connect-身份认证                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  学习目标：理解 OIDC 如何在 OAuth 2.0 基础上实现身份认证                    │
│                                                                             │
│  3.1 OIDC 概述                                                              │
│      - 为什么需要 OIDC：OAuth 2.0 只解决授权问题                            │
│      - OIDC 在 OAuth 2.0 上的扩展                                          │
│      - 认证 vs 授权：OIDC 回答"你是谁"                                      │
│                                                                             │
│  3.2 ID Token vs Access Token                                              │
│      - ID Token：身份令牌，告诉客户端"用户是谁"                             │
│      - Access Token：访问令牌，告诉 API"用户能访问什么"                    │
│      - 两者不能混用                                                         │
│                                                                             │
│  3.3 ID Token 结构                                                          │
│      - JWT 格式                                                             │
│      - 标准 Claims：iss, sub, aud, exp, iat, auth_time 等                 │
│      - 自定义 Claims                                                        │
│      - 签名验证                                                             │
│                                                                             │
│  3.4 OIDC 核心端点                                                          │
│      - Authorization Endpoint：授权端点                                     │
│      - Token Endpoint：令牌端点                                            │
│      - UserInfo Endpoint：用户信息端点                                      │
│      - JWKS Endpoint：公钥端点                                             │
│      - EndSession Endpoint：登出端点                                        │
│      - Introspection Endpoint：令牌 introspection                          │
│                                                                             │
│  3.5 OIDC Discovery                                                        │
│      - /.well-known/openid-configuration                                   │
│      - 自动获取配置信息                                                     │
│      - issuer、authorization_endpoint、token_endpoint 等                   │
│                                                                             │
│  3.6 实战：使用 OIDC 实现登录                                              │
│      - 登录请求参数：scope、response_type、client_id、redirect_uri         │
│      - 回调处理：授权码兑换 Token                                           │
│      - 验证 ID Token：签名、iss、aud、exp、nonce                           │
│      - 获取用户信息                                                         │
│                                                                             │
│  推荐资源：                                                                  │
│  - OpenID Connect Core 1.0 规范                                            │
│  - OpenID Connect Discovery 1.0                                            │
│  - JWT.io: 在线 JWT 解析和验证                                              │
│                                                                             │
│  实践任务：                                                                  │
│  - 使用 Keycloak 测试完整登录流程                                           │
│  - 解析和验证 ID Token                                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 阶段四：安全专题（约 3-4 天）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  03-SSO-安全机制与最佳实践                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  学习目标：掌握 SSO 实施中的安全要点和常见攻击防护                           │
│                                                                             │
│  4.1 OAuth 2.0 安全威胁                                                     │
│      - 授权码截获攻击                                                       │
│      - 重放攻击                                                             │
│      - CSRF 攻击                                                            │
│      - 开放重定向                                                           │
│      - 密钥混淆攻击                                                         │
│                                                                             │
│  4.2 防御措施                                                               │
│      - 使用 PKCE：防止授权码截获                                            │
│      - 使用 state 参数：防止 CSRF                                          │
│      - 使用 nonce 参数：防止 ID Token 重放                                 │
│      - 验证 redirect_uri：防止开放重定向                                    │
│      - 禁止 alg=none：防止密钥混淆                                          │
│                                                                             │
│  4.3 Token 安全                                                             │
│      - Token 存储：内存中 vs HTTP-only Cookie                              │
│      - Token 传输：仅 HTTPS 传输                                           │
│      - Token 有效期：短期 Access Token                                      │
│      - Token 撤销：支持 revocation                                          │
│                                                                             │
│  4.4 会话安全                                                               │
│      - Session Fixation 防护：登录后更换 Session ID                       │
│      - Session 过期策略：绝对过期 vs 空闲过期                               │
│      - 并发会话控制：多设备登录管理                                          │
│                                                                             │
│  4.5 常见攻击向量                                                           │
│      - XSS 攻击：窃取 Token                                                 │
│      - CSRF 攻击：利用用户会话执行操作                                      │
│      - 中间人攻击：窃听 Token                                               │
│      - 钓鱼攻击：伪造授权页面                                               │
│                                                                             │
│  推荐资源：                                                                  │
│  - RFC 6819: OAuth 2.0 Security Considerations                            │
│  - RFC 7521: OAuth 2.0 Threat Model                                       │
│  - OWASP OAuth 2.0 Cheat Sheet                                            │
│  - OWASP Session Management Cheat Sheet                                   │
│                                                                             │
│  实践任务：                                                                  │
│  - 实现 state、nonce、PKCE 验证                                            │
│  - 模拟常见攻击并验证防护措施                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 阶段五：Keycloak 实战（约 3-4 天）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  04-Keycloak-部署与配置                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  学习目标：掌握 Keycloak 的部署、配置和基本管理                             │
│                                                                             │
│  5.1 Keycloak 简介                                                          │
│      - 开源身份和访问管理解决方案                                           │
│      - 功能特性：SSO、OIDC/SAML 支持、用户管理、MFA                         │
│      - 适用场景：企业级应用、API 保护                                       │
│                                                                             │
│  5.2 Keycloak 部署                                                          │
│      - Docker 部署方式                                                      │
│      - 配置文件：standalone.xml、domain.xml                                │
│      - 启动参数：端口、HTTPS、数据库                                        │
│      - 生产环境部署要点                                                     │
│                                                                             │
│  5.3 Keycloak 核心概念                                                      │
│      - Realm：独立的身份管理空间                                            │
│      - Client：需要认证的应用                                               │
│      - User：最终用户                                                       │
│      - Role：用户角色                                                       │
│      - Scope：权限范围                                                      │
│      - Client Scope：定义 Token 中包含的声明                                │
│                                                                             │
│  5.4 Client 配置详解                                                        │
│      - Client ID 和 Secret                                                 │
│      - Client Protocol：openid-connect                                      │
│      - Access Type：confidential、public、bearer-only                      │
│      - Valid Redirect URIs：回调地址白名单                                  │
│      - Web Origins：CORS 配置                                              │
│      - Client Scope 映射                                                   │
│                                                                             │
│  5.5 Token 配置                                                             │
│      - Access Token 有效期                                                 │
│      - Refresh Token 有效期和轮换策略                                       │
│      - ID Token 有效期                                                     │
│      - 签名算法选择                                                         │
│                                                                             │
│  5.6 用户和角色管理                                                          │
│      - 创建用户、设置密码                                                   │
│      - 角色分配：Realm 角色 vs Client 角色                                 │
│      - 角色映射到 Token 声明                                                │
│      - 用户组管理                                                           │
│                                                                             │
│  5.7 MFA 配置                                                               │
│      - 支持的 MFA 类型：TOTP、WebAuthn、Email                              │
│      - 条件 MFA：基于角色、设备                                             │
│      - 强制 MFA 策略                                                        │
│                                                                             │
│  推荐资源：                                                                  │
│  - Keycloak 官方文档: https://www.keycloak.org/documentation              │
│  - Keycloak Docker 镜像文档                                                │
│  - Keycloak Server Installation and Configuration Guide                   │
│                                                                             │
│  实践任务：                                                                  │
│  - 使用 Docker 部署 Keycloak                                               │
│  - 创建 Realm、Client、User                                                │
│  - 配置 Client 并测试 OIDC 流程                                            │
│  - 配置 MFA 并测试登录                                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 阶段六：实践项目（约 4-5 天）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  05-实战：zora-sso-实现-单点登录                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  学习目标：结合前五阶段知识，实现一个完整的 SSO 集成层                       │
│                                                                             │
│  6.1 项目架构设计                                                           │
│      - 整体架构：应用 -> zora-sso -> Keycloak                              │
│      - 模块划分：si（领域模型）、core（核心逻辑）、muserver（HTTP 层）      │
│      - 核心流程：登录发起 -> 回调处理 -> 会话建立 -> 登出                  │
│                                                                             │
│  6.2 核心功能实现                                                           │
│      - OIDC Discovery：自动获取和缓存 Provider 配置                        │
│      - JWKS 获取和缓存：获取并验证签名公钥                                  │
│      - 登录发起：生成 state、nonce、PKCE，构建授权请求                      │
│      - 回调处理：验证参数、兑换 Token、验证 Token                          │
│      - 会话管理：创建、更新、销毁本地会话                                    │
│      - 登出处理：本地登出和 Keycloak 登出                                  │
│                                                                             │
│  6.3 安全校验实现                                                           │
│      - state 验证：防止 CSRF                                               │
│      - nonce 验证：防止 ID Token 重放                                      │
│      - PKCE 验证：防止授权码截获                                           │
│      - Token 验证：签名、iss、aud、exp、算法                               │
│      - redirect_uri 验证：防止开放重定向                                    │
│                                                                             │
│  6.4 集成 MuServer                                                         │
│      - HTTP Handler：登录、回调、登出、用户信息                            │
│      - 配置管理：环境变量配置加载                                          │
│      - 错误处理：统一错误响应格式                                           │
│      - 安全响应头：CSRF、CORS、X-Frame-Options 等                         │
│                                                                             │
│  6.5 客户端接入示例                                                         │
│      - 服务端 Web 应用接入                                                 │
│      - 获取当前登录用户信息                                                 │
│      - 权限检查：角色和 Scope 映射                                          │
│                                                                             │
│  推荐资源：                                                                  │
│  - zora-sso 项目 PLAN.md 和实现文档                                        │
│  - zora 框架文档                                                            │
│  - OIDC 协议一致性测试工具                                                 │
│                                                                             │
│  实践任务：                                                                  │
│  - 实现完整的 OIDC 登录流程                                                │
│  - 集成 Keycloak 并测试多应用 SSO                                          │
│  - 实现会话管理和登出功能                                                  │
│  - 安全测试：验证各项安全校验                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 阶段七：高级专题（约 3-4 天）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  06-高级主题与生产优化                                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  学习目标：掌握生产环境所需的高级特性和优化措施                             │
│                                                                             │
│  7.1 密钥轮换                                                               │
│      - 密钥轮换的原因：安全性和合规性                                      │
│      - JWKS 密钥版本管理                                                   │
│      - 密钥轮换窗口期处理                                                  │
│      - 密钥恢复策略                                                        │
│                                                                             │
│  7.2 会话高可用                                                             │
│      - 分布式会话存储：Redis、PostgreSQL                                   │
│      - Session 复制 vs 集中存储                                            │
│      - 多节点部署注意事项                                                  │
│                                                                             │
│  7.3 单点登出（SSO Logout）                                                │
│      - Front-Channel Logout：前端跳转                                      │
│      - Back-Channel Logout：后端回调                                       │
│      - 全部登出 vs 单应用登出                                              │
│                                                                             │
│  7.4 性能优化                                                              │
│      - JWKS 缓存策略                                                       │
│      - Token 验证缓存                                                      │
│      - 网络延迟优化                                                        │
│      - 并发请求处理                                                        │
│                                                                             │
│  7.5 监控与审计                                                             │
│      - 登录日志：成功、失败、异常                                          │
│      - Token 验证日志                                                      │
│      - 性能指标：响应时间、错误率                                          │
│      - 安全告警：异常登录、暴力破解                                        │
│                                                                             │
│  7.6 故障处理与灾难恢复                                                     │
│      - Keycloak 不可用时的降级策略                                         │
│      - 会话数据备份与恢复                                                  │
│      - 密钥泄露应急处理                                                    │
│                                                                             │
│  推荐资源：                                                                  │
│  - Keycloak 运维指南                                                       │
│  - OAuth 2.0 安全最佳实践 (RFC 9700)                                      │
│  - OIDC 协议一致性测试套件                                                 │
│                                                                             │
│  实践任务：                                                                  │
│  - 实现密钥轮换处理                                                        │
│  - 配置 Redis 会话存储                                                     │
│  - 实现 Back-Channel Logout                                               │
│  - 配置监控和告警                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 学习路径总览

```
时间线
─────────────────────────────────────────────────────────────────────────────▶

Week 1         Week 2         Week 3         Week 4         Week 5
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ 00 基础  │▶│ 01 OAuth │▶│ 02 OIDC  │▶│ 03 安全  │▶│ 04 Key   │
│ 概念     │  │ 2.0      │  │          │  │ 机制     │  │ cloak   │
└──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
                                                         │
                                                         ▼
                                               ┌──────────────────┐
                                               │  05 实战项目     │
                                               │  zora-sso 实现   │
                                               └──────────────────┘
                                                         │
                                                         ▼
                                               ┌──────────────────┐
                                               │  06 高级专题     │
                                               │  生产优化        │
                                               └──────────────────┘
```

## 各阶段依赖关系

```
00-前置知识基础
    │
    ▼
01-OAuth-2.0-授权框架基础
    │
    ├──▶ 02-OpenID-Connect-身份认证
    │        │
    │        ▼
    │    03-SSO-安全机制与最佳实践
    │        │
    │        ▼
    │    04-Keycloak-部署与配置
    │        │
    │        ▼
    │    05-实战：zora-sso-实现-单点登录
    │        │
    │        ▼
    │    06-高级主题与生产优化
    │
    └───────────────▶ 04-Keycloak-部署与配置
```

## 推荐阅读

### 书籍

1. 《OAuth 2 in Action》- 深入理解 OAuth 2.0 协议
2. 《Identity, Authentication, and Access Management in OpenStack》- 身份管理基础

### 规范文档

1. [RFC 6749](https://tools.ietf.org/html/rfc6749) - OAuth 2.0 Authorization Framework
2. [RFC 7636](https://tools.ietf.org/html/rfc7636) - PKCE
3. [RFC 7519](https://tools.ietf.org/html/rfc7519) - JWT
4. [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
5. [RFC 9700](https://tools.ietf.org/html/rfc9700) - OAuth 2.0 Security Best Current Practice

### 在线资源

1. [OAuth 2.0 官方文档](https://oauth.net/2/)
2. [OpenID Connect 官方文档](https://openid.net/developers/how-connect-works/)
3. [Keycloak 官方文档](https://www.keycloak.org/documentation)
4. [OWASP OAuth 2.0 Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/OAuth2_Cheat_Sheet.html)

## 总结

本学习计划覆盖了 SSO 实现的完整知识体系，从基础概念到生产实践共 7 个阶段。建议按照顺序学习，每阶段都要完成配套实践任务。

关键学习要点：
- 理解认证与授权的区别
- 掌握 OAuth 2.0 + OIDC 协议流程
- 重视安全机制：PKCE、state、nonce、Token 验证
- 通过 Keycloak 实战加深理解
- 关注生产环境的高级特性