# OIDC 客户端研究应用

同一套代码以 `app-a` 和 `app-b` 两个 profile 启动，形成独立的 OIDC 客户端、应用会话和端口，用于验证中央身份会话的复用。

## 两份实例

| Profile | 端口 | client_id | 密钥环境变量 |
| --- | --- | --- | --- |
| `app-a` | 9101 | `app-a` | `SSO_CLIENT_A_SECRET` |
| `app-b` | 9102 | `app-b` | `SSO_CLIENT_B_SECRET` |

默认 profile 为 `app-a`。两个实例使用不同的 Session Cookie 名称，避免同一主机不同端口间覆盖 Cookie。

## 核心实现

- `ClientSecurityConfiguration` 使用 `oauth2Login`，显式为保密客户端启用 PKCE S256。
- `/oauth2/authorization/lab` 发起 OIDC 登录，`/login/oauth2/code/lab` 由框架处理回调、state 校验及后端令牌兑换。
- 框架验证 ID Token 的签名、issuer、受众及适用的 nonce 等标准约束。
- 首页 `/` 公开；`/api/me` 要求登录，只输出应用名称、subject 和 issuer。
- Thymeleaf 为 POST 退出表单加入 CSRF 字段，退出成功处理器发起 OIDC 退出。

## 启动与退出边界

参见[项目快速启动](../README.md)。授权服务器必须先运行，客户端启动时会进行 OIDC discovery。

退出应用 A 会结束 A 的本地会话及中央身份会话，但不会自动删除应用 B 已有的本地会话。本项目不实现跨应用退出通知。

