# 08-FEATURE 多环境配置加载

## 目标

`zora-sso-muserver` 使用与 host-helper-server 一致的配置加载方式，通过 `APP_ENV` 选择运行
环境，并兼容旧变量 `env`。部署环境可通过 `ZORA_SSO_CONFIG_DIR` 从外部目录加载配置，确保
不同环境的用户、客户端和网络安全设置彼此隔离。

## 加载规则

启动时按以下顺序处理：

1. 优先读取环境变量 `APP_ENV`；值为空时读取兼容变量 `env`。
2. 校验环境名只能包含字母、数字、下划线和连字符，且第一个字符必须是字母或数字。
3. 如果设置了 `ZORA_SSO_CONFIG_DIR`，加载该目录下的 `application-${APP_ENV}.conf`；否则
   加载 classpath 中的同名资源。
4. 使用公共 `application.conf` 补充未定义的配置。
5. 将 `zora-sso.environment` 强制设置为最终选择的环境值。
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
export APP_ENV=local
export ZORA_SSO_CONFIG_DIR=/etc/zora-sso
```

PowerShell：

```powershell
$env:APP_ENV = "local"
$env:ZORA_SSO_CONFIG_DIR = "C:\zora-sso\config"
```

本地开发可以不设置 `ZORA_SSO_CONFIG_DIR`，直接使用
`zora-sso-muserver/src/main/resources/application-local.conf`。新增部署环境时，在受控外部配置
目录中创建 `application-<环境>.conf`，并在发布前验证公开地址、Cookie、安全开关、用户哈希、
客户端哈希和回调 URI。
