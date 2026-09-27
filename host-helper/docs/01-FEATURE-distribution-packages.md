# 01-FEATURE 四类分发包

## 背景与决策

原有 Maven 构建只生成业务模块 JAR；`host-helper-muserver` 另外把依赖复制到 `target/lib`，但尚无可迁移的运行目录或自动化部署文件。把分发模块 `host-helper-dist` 放在 reactor 最后，保持普通构建不变；仅启用特定 profile 时使用 Maven Assembly Plugin 生成分发归档。

## 包边界

- `package-source`：项目源码和构建所需文件，白名单方式收集，不复制 `target`、私钥与环境文件。
- `package-test`：Surefire XML 和文本报告，不捆绑二进制；必须从 clean 开始并运行测试，避免旧报告污染。
- `package-deploy`：Ansible 部署资产，调用 `zora-bin` 自带的发布/启动脚本；不含应用、密码或密钥，不安装 systemd unit。需与同版本的 service 包配套。
- `package-service`：manifest 指定入口和 `lib/` 类路径的 `app.jar`，所有 runtime scope 依赖 JAR，以及直接从 `zora-bin` 解包的 deploy/start/stop/status/lifecycle 脚本；不是 Linux 系统服务，不附带 JDK、test scope 依赖、示例 JWT 密钥或 `local`/`sit` 环境配置。

每种包使用 classifier 防止覆盖主构件，普通 `mvn package` 不生成以上分发包。发行前应在 CI 对每个包分别执行校验（启动入口、JAR 依赖、报告 XML、资产清单、敏感文件检查）。

## 安全与环境限制

`zora-bin` 从 `APP_HOME/env.tag` 获取 `APP_ENV`，source 已部署版本的 `config/setenv`（受信任 Bash 文件）；应用兼容原有 `env` 变量并在其不存在时读取 `APP_ENV`。应用使用 `HOST_HELPER_CONFIG_DIR/application-${env}.conf`，无外部文件时启动失败，并与 JAR 内的 `application.conf` 合并；`PORT` 可选。`zora-bin` 的 `deploy.sh` 管理 `APP_HOME/<version>`、`APP_HOME/active`、进程关闭和回滚。仓库现有 `application-local.conf` 和 `application-sit.conf` 包含示例用户和环境主机，`application.conf` 中 JWT 密钥路径默认 `NOT-SET`；这些不是可用的生产配置。生产发布需要安全提供外部配置及密钥，不能将密钥加入归档。

`package-deploy` 需要目标机上 Bash、GNU/Linux、JDK 25 和 Ansible；Windows 可通过 Maven 生成归档，但生命周期脚本需在 Linux 环境验证。
