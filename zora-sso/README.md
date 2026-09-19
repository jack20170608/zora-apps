# zora-sso

`zora-sso` 为 Zora 应用提供统一身份接入、中央会话和权限映射能力。

当前版本是**仅用于开发环境的本地认证 POC**：用户和客户端从启动配置加载，Session、授权事务和一次性 Code 全部保存在内存中。它不实现或声明兼容 OAuth 2.0/OIDC，也不签发 JWT。

## 模块

- `zora-sso-si`：稳定领域模型和服务端口。
- `zora-sso-core`：本地认证和内存 SSO 核心实现。
- `zora-sso-muserver`：MuServer HTTP 接口和登录页面。

## 构建

```bash
mvn clean verify
```

## 本地配置

默认配置不会创建用户或客户端。先构建项目，再用交互式工具分别生成用户密码和客户端密钥哈希：

```powershell
java -cp .\zora-sso-core\target\classes top.ilovemyhome.zorasso.core.PasswordHashCli
```

将哈希写入 `zora-sso-muserver\src\main\resources\application.conf` 的 `users` 和 `clients` 列表后启动 `top.ilovemyhome.zorasso.muserver.App`。配置示例及客户端调用流程见 `docs\02-FEATURE-本地登录与授权流程.md`。

## 安全限制

本地认证必须显式启用，生产环境默认拒绝启动。配置只接受 PBKDF2 密码哈希和客户端密钥哈希，不接受明文凭据。正式环境应替换为 Keycloak 等标准 OIDC Provider。

详细设计见 `docs` 目录。
