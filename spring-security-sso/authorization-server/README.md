# 统一身份提供方

基于 Spring Security 7.0 的 OAuth2 授权服务器及 OpenID Provider，默认仅监听本机端口 9100。

## 核心实现

- `ServerSecurityConfiguration` 定义协议端点与普通表单登录两条安全过滤器链。
- 用户 `researcher` 的密码来自 `SSO_DEMO_PASSWORD`，编码后保存在内存。
- `app-a`、`app-b` 使用不同密钥，只允许各自精确注册的回调与退出回调。
- 授权码模式要求 PKCE，提供 discovery、JWKS、ID Token、UserInfo 和 OIDC 退出端点。
- CSRF 保持启用，协议端点由 Spring Security 按协议需求处理。

## 主要端点

| 路径 | 用途 |
| --- | --- |
| `/.well-known/openid-configuration` | OIDC 发现文档 |
| `/oauth2/authorize` | 发起授权 |
| `/oauth2/token` | 后端兑换令牌 |
| `/oauth2/jwks` | 公开验证公钥 |
| `/userinfo` | 获取标准身份信息 |
| `/connect/logout` | RP 发起的 OIDC 退出 |
| `/login` | 默认登录表单 |
| `/health` | 本地健康检查 |

## 启动与测试

按[项目启动说明](../README.md)设置环境变量并启动。模块测试使用随机端口和测试专用凭据，不依赖运行中的授权服务器。

本模块没有持久化存储；RSA 密钥、中央 Session 和已发放授权均不会跨重启保留。禁止直接作为生产认证服务部署。
