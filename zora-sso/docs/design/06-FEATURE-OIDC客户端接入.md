# OIDC 客户端接入

**状态：目标设计，尚未实现。**

## 1. 服务端 Web 应用流程

1. 用户访问受保护资源。
2. 应用创建服务端授权事务，生成 `state`、`nonce`、PKCE verifier。
3. 浏览器跳转到 Keycloak Authorization Endpoint。
4. 回调时应用先验证并单次消费 `state`。
5. 应用后端使用 Code、原 Redirect URI 和 PKCE verifier 调用 Token Endpoint。
6. 完整验证 ID Token，并将 `(iss, sub)` 映射为本地身份。
7. 创建新的应用 Session，再跳回最初页面。

授权事务、Token 和 Session 必须有独立的数据结构及 TTL。

## 2. 接入配置

每个应用至少配置：

```text
issuer
client-id
client-authentication-method
client-secret-reference（仅 Confidential Client）
redirect-uri
post-logout-redirect-uri
scopes
allowed-signing-algorithms
role-mappings
```

`issuer`、回调地址和算法来自部署配置，不从用户请求推断。Secret 配置只保存引用，不保存明文值。

## 3. 身份映射

推荐本地记录：

```text
provider_issuer + provider_subject -> local_user_id
```

首次登录自动建号必须有明确策略。邮箱匹配不能默认合并账号；如果业务需要绑定既有账号，应增加已认证的账号绑定流程和审计记录。

## 4. API 访问

- 浏览器页面优先使用应用 Session Cookie。
- 应用调用资源 API 时使用面向该 API 的 Access Token。
- API 校验签名、Issuer、Audience、时间和 Scope，再执行资源级授权。
- 不把 ID Token 发给业务 API。
- 不在浏览器 URL、日志或错误页面输出 Token。

## 5. 登出

产品必须区分：

- **退出当前应用**：删除应用 Session；
- **退出统一登录**：执行 RP-Initiated Logout 并清理应用 Session；
- **安全事件强制退出**：撤销或终止中央会话，并传播到应用。

如果启用 Back-Channel Logout，接收端必须验证 Logout Token 并实现幂等处理。

## 6. 最小测试矩阵

| 类别 | 用例 |
|---|---|
| 正常流程 | 首次登录、已有中央会话、登出后重登 |
| 事务 | state 缺失/错误/重放，nonce 错误，PKCE verifier 错误 |
| Token | 错签名、错 issuer、错 audience、过期、未来 nbf、未知 kid |
| 映射 | 新用户、禁用用户、主体冲突、未知角色 |
| 故障 | Provider 超时、JWKS 轮换、缓存过期、Session 存储不可用 |
