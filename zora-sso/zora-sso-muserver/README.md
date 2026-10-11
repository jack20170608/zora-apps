# zora-sso-muserver

提供本地认证 POC 的 MuServer HTTP 接口、HTML 登录页面、Cookie 管理和启动配置校验。

该模块只允许在明确启用的开发环境运行；正式环境应接入标准 OIDC Provider。

配置加载方式与 host-helper-server 保持一致。启动时优先通过 `APP_ENV` 选择环境，
并兼容旧环境变量 `env`：

```bash
export APP_ENV=local
```

默认将 classpath 中的 `application-${APP_ENV}.conf` 合并到公共 `application.conf`。设置
`ZORA_SSO_CONFIG_DIR` 后，改为读取该目录下的环境配置文件。环境变量缺失、格式非法，或者配置
文件不存在或不可读时，服务拒绝启动。

启动配置和接口流程见：

- [本地登录与授权流程](../docs/design/02-FEATURE-本地登录与授权流程.md)
- [多环境配置加载](../docs/design/08-FEATURE-多环境配置加载.md)
