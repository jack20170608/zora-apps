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

`package-*` 仅是 Maven profile 名；归档采用 `${artifactId}-${version}-${classifier}.tar.gz`：

```text
host-helper-dist-1.0.0-SNAPSHOT-source.tar.gz
host-helper-dist-1.0.0-SNAPSHOT-test.tar.gz
host-helper-dist-1.0.0-SNAPSHOT-deploy.tar.gz
host-helper-dist-1.0.0-SNAPSHOT-service.tar.gz
```

`package-test` 的 Surefire XML 可能记录 JVM 属性与测试输出，只应上传到访问受控的 CI 制品库；请勿在带真实凭据的构建进程中运行测试。必须先 `clean` 且不设置 `-DskipTests`，避免空报告或旧报告。

服务归档只包含版本目录，由部署方选择运行根目录；`bin/` 下五个脚本直接从 `zora-bin` JAR 提取（不是自写脚本）：

```text
1.0.0-SNAPSHOT/
├── app.jar
├── lib/
├── bin/{deploy,lifecycle,start,stop,status}.sh
└── README.md
```

`env.tag`、`active` 软链、`logs/` 和 `run/` 属于运行目录：前两者在部署时配置/激活，后两者由生命周期脚本按需创建。`config/` 属于版本配置，需由可信部署流程提供，不能把密钥打入归档。`app.jar` 的 manifest 包含 Main-Class 和 `lib/` Class-Path；**这不是 systemd 服务包**。完整的部署后目录树见源码中的 `host-helper-dist/deploy/README.md`，也随独立的 `-deploy.tar.gz` 提供。

```bash
mkdir -p /opt/hosthelper
tar -xzf host-helper-dist-1.0.0-SNAPSHOT-service.tar.gz -C /opt/hosthelper
export APP_HOME=/opt/hosthelper
version=1.0.0-SNAPSHOT
mkdir -p "$APP_HOME/$version/config"
cp -a /etc/host-helper/release-config/. "$APP_HOME/$version/config/"
cp /etc/host-helper/config/application-prod.conf "$APP_HOME/$version/config/"
bash "$APP_HOME/$version/bin/deploy.sh" --activate "$version"
bash "$APP_HOME/active/bin/start.sh"
bash "$APP_HOME/active/bin/status.sh"
bash "$APP_HOME/active/bin/stop.sh"
```

调用前须准备 `APP_HOME/env.tag`（或设置 `APP_ENV`）、可信的 `config/setenv` 和外部 HOCON 配置；命令需以应用运行账户执行，详见 `deploy/README.md`。`APP_ENV` 优先于原有的 `env` 变量，`PORT` 覆盖默认端口 8000。服务包不会下载或内嵌 JDK，也不提供私钥或环境配置。`local`、`sit` 配置是开发示例，不可直接用于生产。

