# Micronaut OAuth2 SSO 研究方案

## 目标与边界

验证 Micronaut 应用通过标准 OIDC 接入成熟身份提供方的方案，不改造现有自研认证服务，也不重复实现授权服务器。

Keycloak 负责认证、中央会话、授权码和签名令牌；Micronaut Security OAuth2 负责授权请求、回调及令牌验证，Session 模块保存应用会话。两个应用共享的是中央身份，而不是本地 Session。

## 拓扑与流程

```text
Browser -> Micronaut A :9201 -> Keycloak :9200
Browser -> Micronaut B :9202 -> Keycloak :9200

Micronaut backend -> Keycloak token endpoint / discovery / JWKS
```

1. 应用通过 `/oauth/login/keycloak` 发起请求，框架生成 state、nonce 与 PKCE challenge。
2. Keycloak 检查客户端及精确注册的回调，在无中央会话时要求用户登录。
3. 登录后，Keycloak 将一次性授权码返回 `/oauth/callback/keycloak`。
4. Micronaut 后端使用客户端凭据与 PKCE verifier 兑换令牌，框架完成 OIDC 验证。
5. 应用建立自己的 Session；另一应用随后经过同样流程，但可以复用中央登录，不再要求密码。

## 实现取舍

- 使用一个 Maven 应用的两个运行环境，避免复制相同业务代码。
- 使用 Micronaut 平台 BOM，不与 Spring Boot 或 Zora 的版本管理混合。
- 显式依赖 OAuth2、JWT、Session 和 CSRF 模块。JWT 模块用于验证 ID Token；浏览器使用 Session Cookie。
- 客户端回调精确注册，Keycloak 强制 PKCE S256，不启用密码授权和服务账号授权。
- A/B 的 Session、state、nonce Cookie 分别命名。相同主机的不同端口不是 Cookie 隔离边界。
- 使用服务端同步令牌保护 POST 本地退出，不开放 GET 本地退出。
- 身份接口只输出应用名和当前用户名，不返回 Authentication 的完整 attributes，因为其中可能含有令牌。
- 正常运行使用 INFO 日志，避免开启可能记录协议参数的调试日志。

## 三种方案对照

| 方案 | 身份提供方 | 应用侧协议实现 | 研究重点 |
| --- | --- | --- | --- |
| 现有 `zora-sso` | 自研本地认证服务 | MuServer 自定义回调和兑换 | 自定义协议、安全边界及维护成本 |
| `spring-security-sso` | Spring Security 授权服务器 | Spring Security OAuth2 Login | 自建标准授权服务器与标准客户端 |
| 本项目 | Keycloak | Micronaut Security OAuth2 | 轻量客户端接入成熟身份产品 |

本项目不将 Micronaut 客户端库描述为授权服务器，也不声称它与自研接口可以直接互换。后续迁移需要使用标准 issuer、客户端注册与 OIDC 端点。

## 退出语义

当前 POST `/logout` 只清理一个应用的本地认证。Keycloak 会话和其他应用会话保持有效，所以再次登录仍可能免密。

自动 end-session 路由在本实验中关闭。后续若加入中央退出，应确认 Keycloak 版本、标准退出参数、CSRF、退出回调与其他应用会话失效策略，不能简单启用 GET 本地退出绕过校验。

## 验证与限制

- 4 项自动测试验证页面会话、未登录身份保护、缺少 CSRF 的拒绝和 GET 退出限制。
- 真实 Keycloak 与 Chrome 验证双应用 SSO、PKCE、nonce、HttpOnly/SameSite Cookie、本地退出及应用间隔离。
- Docker 启动分支未在当前无 Docker 的环境运行；端到端验证使用临时目录中的官方 Keycloak 26.8.1 发行包。
- HTTP、回环地址、内存 Session、Keycloak 开发模式及临时数据只适用于实验。生产部署必须重新设计 HTTPS、持久化、会话、凭据、权限及审计。

## 后续研究

1. 映射 realm/client 角色与应用权限，验证已认证但无权限的行为。
2. 添加错误 state、nonce、PKCE、过期授权码和重放等负向协议测试。
3. 研究共享会话存储、会话固定攻击防护及多实例扩容。
4. 实现并验证 RP 发起退出与 back-channel logout。
5. 让不同框架的标准 OIDC 客户端接入同一个 Keycloak，比较接入成本。

## 参考资料

- [Micronaut Security 4.15.2 指南](https://micronaut-projects.github.io/micronaut-security/4.15.2/guide/)
- [Keycloak realm 导入与导出](https://www.keycloak.org/server/importExport)
- [Keycloak 开发模式容器启动](https://www.keycloak.org/server/containers)

