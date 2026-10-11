# 08-FEATURE 多环境配置加载

## 目标

`zora-sso-muserver` 通过小写环境变量 `app_env` 选择运行环境，避免在启动命令中修改配置文件，
并确保不同环境的用户、客户端和网络安全设置彼此隔离。

## 加载规则

启动时按以下顺序处理：

1. 读取环境变量 `app_env`。
2. 校验环境名只能包含字母、数字、下划线和连字符，且第一个字符必须是字母或数字。
3. 加载 `application-${app_env}.conf`。
4. 使用公共 `application.conf` 补充未定义的配置。
5. 将 `zora-sso.environment` 强制设置为 `app_env` 的值。
6. 执行 `SsoConfiguration` 的安全校验后启动服务。

环境变量缺失、值非法或对应配置不存在时，服务立即拒绝启动。环境配置不能通过声明另一个
`zora-sso.environment` 来绕过生产环境限制。

## 配置边界

`application.conf` 只保存所有环境共享的安全默认值，并默认禁用本地认证：

```hocon
zora-sso {
  environment = "NOT-SET"
  local-auth {
    enabled = false
    production-allowed = false
  }
  users = []
  clients = []
}
```

环境文件只覆盖该环境需要的值。开发环境示例位于 `application-local.conf`：

```hocon
zora-sso {
  local-auth.enabled = true
  server.public-base-url = "http://localhost:9080"
  cookie.secure = false
  users = []
  clients = []
}
```

正式环境仍受已有安全规则约束：本地认证默认禁止在 `production` 环境启动，远程公开地址必须
使用 HTTPS，生产 Cookie 必须启用 Secure。密码和客户端密钥只能保存 PBKDF2 哈希，不得保存
明文。

## 启动示例

Linux：

```bash
export app_env=local
```

PowerShell：

```powershell
$env:app_env = "local"
```

设置变量后，从 IDE 运行 `top.ilovemyhome.zorasso.muserver.App`。

新增环境时创建 `zora-sso-muserver/src/main/resources/application-<环境>.conf`，并在发布前验证
公开地址、Cookie、安全开关、用户哈希、客户端哈希和回调 URI。
