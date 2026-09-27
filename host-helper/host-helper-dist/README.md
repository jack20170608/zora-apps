# host-helper-dist

分发模块（Maven `pom` packaging），依赖 `host-helper-muserver` 保证位于 reactor 末尾。正常 `mvn package` 不产生附加分发包。四个按用途区分的 profile 在本模块 `target/` 生成带 classifier 的 `tar.gz` 包。

| Profile | 包内容 |
| --- | --- |
| `package-source` | 根及四个模块的源码、构建文件、README 和部署源文件；不包含构建产物 |
| `package-test` | 三个业务模块的 JUnit/Surefire XML、文本报告；必须运行测试，不要使用 `-DskipTests` |
| `package-deploy` | Ansible playbook 和部署说明，复用 `zora-bin` 发布脚本；不包含应用二进制和环境变量文件 |
| `package-service` | `app.jar`、运行期 `lib/` 及直接取自 `zora-bin` 的 `bin/{deploy,start,stop,status,lifecycle}.sh`；不包含 systemd 单元、测试依赖、开发环境配置或密钥 |

在 `host-helper` 根目录运行（建议一次只选一个 profile）：

```bash
mvn clean package -Ppackage-source
mvn clean package -Ppackage-test
mvn clean package -Ppackage-deploy -DskipTests
mvn clean package -Ppackage-service -DskipTests
```

文件名示例：`host-helper-dist-1.0.0-SNAPSHOT-package-service.tar.gz`。

`package-test` 的 Surefire XML 可能记录 JVM 属性与测试输出，只应上传到访问受控的 CI 制品库；请勿在带真实凭据的构建进程中运行测试。必须先 `clean` 且不设置 `-DskipTests`，避免空报告或旧报告。

`app.jar` 带有 Main-Class 和 `lib/` 的 manifest Class-Path，可用 `java -jar` 启动。Linux 上推荐用 `zora-bin` 的 `deploy.sh` 构建 `APP_HOME/<version>`、`APP_HOME/active`，然后以 `active/bin/start.sh`、`stop.sh`、`status.sh` 管理进程；**这不是 systemd 服务包**。`HOST_HELPER_CONFIG_DIR` 是包含 `application-<env>.conf` 的外部目录，配置中的 JWT 密钥应指向外部受保护文件。具体操作见部署包说明。

```bash
tar -xzf host-helper-dist-1.0.0-SNAPSHOT-package-service.tar.gz
export APP_HOME=/opt/host-helper
version=1.0.0-SNAPSHOT
bash "host-helper-service-$version/bin/deploy.sh" "$version" "host-helper-service-$version/app.jar" "host-helper-service-$version/lib" /etc/host-helper/release-config
bash "$APP_HOME/active/bin/start.sh"
bash "$APP_HOME/active/bin/status.sh"
bash "$APP_HOME/active/bin/stop.sh"
```

在调用前需准备 `APP_HOME`、`APP_HOME/env.tag`、`/etc/host-helper/release-config/setenv` 和外部配置，详见 `deploy/README.md`。`APP_ENV`（由 `env.tag` 设置）优先于原有的 `env` 变量，`PORT` 覆盖默认端口 8000。服务包不会下载或内嵌 JDK，也不提供私钥或环境配置。`local`、`sit` 配置是开发示例，不可直接用于生产。

