# 02-FEATURE zora-sso 认证接入

完整请求参数、响应结构和异常分支参见
[zora-sso 完整认证流程](03-FEATURE-zora-sso完整认证流程.md)。

## 背景与目标

`host-helper-muserver` 原来直接校验本地用户名和密码，并自行签发 JWT。该方式无法共享中央登录
会话，也要求每个业务服务维护密码。现在浏览器认证统一交给 zora-sso；host-helper 只接收一次性
授权码，并在后端兑换经过认证的身份。

## 认证流程

1. 用户访问 `/hosthelper/index.html`。
2. 未认证请求跳转到 `/auth/sso/login`。
3. host-helper 生成 256 位随机 `state`，在内存中保存五分钟，然后跳转到 zora-sso
   `/authorize`。
4. 用户在 zora-sso 完成登录。zora-sso 将浏览器重定向到预注册的
   `/auth/sso/callback`，并携带一次性 `code` 和原 `state`。
5. host-helper 单次消费 `state`，通过后端通道向 `/sso/token/exchange` 提交授权码、客户端 ID、
   客户端密钥和固定回调 URI。
6. host-helper 将 SSO 角色映射为本地角色，签发原有 JWT 格式的 HttpOnly 应用 Cookie，然后
   跳转到首页。已有 REST 过滤器继续验证该 Cookie。

浏览器不会接触客户端密钥或兑换结果。授权码、客户端密钥和应用 JWT 均不会写入日志。

## 配置

基础配置位于 `host-helper-muserver/src/main/resources/config/application.conf`。部署环境必须
覆盖以下值：

| 环境变量 | 说明 |
|---|---|
| `ZORA_SSO_BASE_URI` | zora-sso 的外部 HTTPS 地址 |
| `ZORA_SSO_CLIENT_ID` | 在 zora-sso 注册的客户端 ID |
| `ZORA_SSO_CLIENT_SECRET` | 从密钥管理系统注入的客户端明文密钥 |
| `ZORA_SSO_REDIRECT_URI` | host-helper 的完整回调 URI |
| `HOST_HELPER_SECURE_COOKIE` | 非本机环境必须设为 `true` |

zora-sso 端保存 `ZORA_SSO_CLIENT_SECRET` 的 PBKDF2 哈希，而不是明文。使用
`zora-sso-core` 的 `PasswordHashCli` 生成哈希，并注册如下客户端：

```hocon
{
  client-id = "host-helper"
  client-secret-hash = "pbkdf2-sha256$..."
  redirect-uris = ["https://host-helper.example.com/auth/sso/callback"]
  enabled = true
}
```

`redirect-uris` 必须与 `ZORA_SSO_REDIRECT_URI` 完全一致。客户端密钥不得写入 HOCON、Git、
启动参数或普通部署日志。

默认角色映射如下：

| SSO 角色 | host-helper 角色 |
|---|---|
| `USER` | `read` |
| `ADMIN` | `admin`、`read`、`write` |

未配置映射或最终没有任何本地角色的身份会被拒绝。可在外部 HOCON 的
`sso.role-mappings` 中覆盖映射。

## 安全边界

- `state` 使用 `SecureRandom` 生成、限时保存并单次消费，阻止登录 CSRF 和回调重放。
- 待处理事务数量有上限，过期事务会清理，避免匿名请求无限占用内存。
- 回调 URI 来自服务端配置，不接受请求提供的地址，避免开放重定向。
- 非本机的 SSO 地址和回调地址必须使用 HTTPS；非本机回调强制启用 Secure Cookie。
- 后端兑换设置连接和请求超时，拒绝重定向，限制响应大小并要求 JSON 内容类型。
- SSO 不可用、响应格式错误、未知角色或会话签发失败时均默认拒绝。
- 应用 Cookie 使用 `HttpOnly`、`SameSite=Lax` 和固定路径；有效期不超过本地 JWT TTL。

当前 zora-sso 是本地认证实现，身份数据由授权码兑换接口返回。未来切换标准 OIDC Provider 时，
应按 zora-sso 的目标架构增加 Discovery、PKCE、ID Token 签名及 `iss`、`aud`、`nonce` 校验，
不能把当前内部兑换响应当作标准 OIDC Token。
