# zora-sso-muserver

提供本地认证 POC 的 MuServer HTTP 接口、HTML 登录页面、Cookie 管理和启动配置校验。

该模块只允许在明确启用的开发环境运行；正式环境应接入标准 OIDC Provider。

启动时必须通过小写环境变量 `app_env` 选择环境配置：

```bash
export app_env=local
```

程序将 `application-${app_env}.conf` 合并到公共 `application.conf`。环境变量缺失、格式非法或
对应资源不存在时，服务拒绝启动。设置变量后，可从 IDE 运行
`top.ilovemyhome.zorasso.muserver.App`。

启动配置和接口流程见：

- [本地登录与授权流程](../docs/design/02-FEATURE-本地登录与授权流程.md)
- [多环境配置加载](../docs/design/08-FEATURE-多环境配置加载.md)
