# Spring Security 7.0 SSO 研究方案

## 研究目标

在不修改现有 `zora-sso` 和 host-helper 的前提下，验证框架提供的 OAuth2/OIDC 能力，建立可以运行、观察和比较的标准实现基线。

研究问题包括：哪些安全协议细节可以交给成熟框架、两个独立应用如何复用统一登录、中央会话与应用会话如何区分，以及退出能覆盖哪些范围。

## 模块边界

```text
Browser -> App A :9101 -> OpenID Provider :9100
Browser -> App B :9102 -> OpenID Provider :9100

App A/B backend -> token endpoint / discovery / JWKS / UserInfo
```

`authorization-server` 承担 OpenID Provider 和 OAuth2 授权服务器职责。`oidc-client` 通过两个 profile 运行两个 Relying Party。不同应用注册、密钥和 Cookie 独立，不共享本地登录态。

应用间不直接交换密码或自定义认证票据。令牌由后端保管，不交给前端 localStorage。

## 标准登录流程

1. 浏览器访问应用并选择登录。
2. 应用产生 state、nonce、PKCE verifier，将 challenge 随授权请求发送给身份提供方。
3. 身份提供方验证客户端、精确回调及授权参数；无中央会话时展示带 CSRF 的登录表单。
4. 身份提供方认证用户，建立中央 Session，并向已注册的应用回调发送一次性授权码。
5. 应用后端携带客户端认证及 PKCE verifier 兑换令牌。
6. 框架按 OIDC 校验 ID Token，应用建立自己的本地 Session。
7. 浏览器再登录另一个应用时，身份提供方复用中央会话，新的应用仍独立完成自己的授权码交换。

## 与现有自研方案对照

| 研究点 | 现有本地 `zora-sso` 流程 | 本研究项目 |
| --- | --- | --- |
| 授权与兑换接口 | 自定义授权码与 `/sso/token/exchange` | OAuth2 标准授权、令牌端点 |
| 用户身份交付 | 自定义身份响应 | OIDC ID Token、UserInfo |
| 应用登录 | 自研 callback、JWT Cookie | `oauth2Login` 与服务端 Session |
| 安全参数 | 手工维护 state、事务、CSRF 和一次性码 | 框架处理 state、nonce、PKCE、CSRF |
| 发现与公钥 | 自定义接入配置 | discovery、issuer、JWKS |
| 生命周期 | 自研 Session 和退出 | OIDC RP 发起退出，应用 Session 独立 |
| 运行平台 | MuServer 与 Zora | Spring Boot 4.0 / Spring Security 7.0 |

这不是将现有 host-helper 直接迁移到 Spring，也不是宣称现有流程满足完整 OAuth2/OIDC 标准。之后可单独研究 MuServer 应用如何作为标准 OIDC 客户端接入同一个身份提供方。

## 安全与实验边界

- 全部服务仅绑定回环地址，使用三个不同端口，不与原服务端口混用。
- 同主机不同端口仍可能共享 Cookie，因此明确使用三个不同 Cookie 名称。
- 两个客户端都要求 PKCE，密钥独立，回调与退出回调均精确注册。
- 保留框架默认安全响应头和 CSRF；不因调试问题关闭浏览器安全机制。
- 测试用户的密码与客户端密钥从环境变量读取，不提供硬编码运行凭据。客户端密钥在一键脚本中临时生成。
- 内存数据库、临时 RSA 密钥、HTTP 与免授权确认均仅适用于可信第一方本地实验。
- OIDC RP 发起退出清理当前应用及中央会话，不保证其他应用本地 Session 立即失效。
- 客户端启动依赖身份提供方 discovery；必须先启动授权服务器。

Spring Boot 4.0.8 默认使用 JUnit 6；为了遵守仓库 JUnit 5 约定，此项目使用 JUnit 5.14.4，并通过应用生命周期 API 启动集成测试服务，避免不兼容的 SpringExtension。

## 已验证与后续研究

初始版本已验证 Maven 构建、6 项自动测试，以及 Chrome 中的首次登录、第二应用免密登录、PKCE、Cookie 隔离与退出流程。HTTP Client 检查和浏览器检查均有必要，不能相互替代。

后续可按以下顺序扩展：

1. 增加持久化客户端、用户目录、授权与会话存储。
2. 持久化并轮换签名密钥，验证旧令牌在轮换期的行为。
3. 研究应用角色映射、权限不足与受保护资源服务器。
4. 研究 back-channel logout、会话失效、跨应用退出语义。
5. 验证错误 state、nonce、PKCE verifier、过期码和授权码重放等负向流程。
6. 比较本地授权服务器与 Keycloak 等成熟身份产品，评估维护成本。

## 参考资料

- [Spring Security 7.0 授权服务器配置模型](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/authorization-server/configuration-model.html)
- [Spring Security 7.0 OAuth2 登录](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/login/core.html)
- [Spring Security 7.0 OIDC 退出](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/login/logout.html)

