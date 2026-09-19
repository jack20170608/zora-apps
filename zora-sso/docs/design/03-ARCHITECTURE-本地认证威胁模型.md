# 本地认证 POC 威胁模型

**状态：部分风险已控制，剩余风险决定了该方案禁止生产。**

## 1. 资产与信任边界

主要资产是用户密码、Client Secret、中央 Session、授权事务、一次性 Code 和身份结果。浏览器输入、Cookie、URL、代理来源地址以及业务应用提交的数据均不可信。

## 2. 已实现控制

| 威胁 | 当前控制 | 代码位置 |
|---|---|---|
| 开放重定向 | Client 与回调 URI 预注册，URI 完整匹配 | `InMemoryLocalSsoService.validate` |
| 登录 CSRF | 登录事务绑定独立随机 CSRF Token | `prepareLogin` / `login` |
| Session 固定 | 登录成功生成新 256 bit Session ID | `login` |
| Code 重放 | 只存摘要、30 秒有效、原子单次消费 | `exchange` |
| Code 串用 | 绑定 Client、回调 URI、身份和有效 Session | `CodeGrant` |
| 凭据填充 | 用户名与来源地址组合的内存限流 | `isRateLimited` |
| 用户枚举 | 未知、禁用和密码错误返回统一错误 | `LocalIdentityAuthenticator` |
| 密码泄漏 | 只配置 PBKDF2 哈希，使用后清空字符数组 | `Pbkdf2PasswordHasher` |
| Cookie 窃取 | `HttpOnly`、`SameSite=Lax`，生产要求 `Secure` | `SsoWebServer.sessionCookie` |
| XSS / 点击劫持 | HTML 转义、CSP、`X-Frame-Options: DENY` | `SsoWebServer` |
| 敏感缓存 | `Cache-Control: no-store` 与 `Pragma: no-cache` | `secureHeaders` |
| 日志泄漏 | 只记录稳定错误码，不记录秘密 | `error` |
| 误入生产 | 显式启用，生产环境默认拒绝启动 | `SsoConfiguration.from` |

## 3. 已知剩余风险

| 风险 | 后果 | 结论 |
|---|---|---|
| 私有协议而非 OAuth/OIDC | 缺少标准互操作与成熟安全分析 | 不能开放给第三方 |
| 无 PKCE、OIDC nonce 和标准 Token 校验 | 无法满足现代 OIDC 接入基线 | 迁移到标准 Provider |
| 全部状态在单进程内存 | 重启丢失，多节点不一致 | 不能用于高可用 |
| 来源地址直接用于限流 | 代理配置错误会绕过或误伤 | 需要可信代理和集中限流 |
| 无 MFA、恢复和用户生命周期 | 账号接管后的控制能力不足 | 交由成熟 Provider |
| 无集中撤销与风险检测 | 安全事件响应能力不足 | 建立生产会话和审计体系 |
| PBKDF2 参数固定 | 需随硬件和组织基线复评 | 建立参数升级机制 |
| `production-allowed` 配置存在 | 误配置可绕过启动保护 | 运维策略必须禁止使用 |

## 4. 安全验收边界

当前测试证明的是特定不变量，例如完整回调匹配、一次性 Code 和配置拒绝；它不证明协议达到生产安全。任何生产化工作必须采用 [目标架构](04-ARCHITECTURE-生产级OIDC目标架构.md)，并进行独立安全评审。
