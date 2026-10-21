# Micronaut Security OAuth2 SSO 研究项目

这是与 `zora-sso` 和 `spring-security-sso` 并行的本机研究应用。使用 `micronaut-security-oauth2` 接入 Keycloak，同一份代码运行两个独立 OIDC 客户端，验证首次登录和跨应用免密登录。

Micronaut Security OAuth2 是 OAuth2/OIDC 客户端能力，不是完整授权服务器。本项目让 Keycloak 负责用户认证、中央会话、授权码、令牌及公钥，Micronaut 负责协议接入和应用本地会话。

## 版本与结构

- JDK 25、Maven 3.9.11+。
- Micronaut Platform 4.10.21，管理 Micronaut Core 4.10.31 与 Security 4.15.2。
- `micronaut-security-oauth2`、`micronaut-security-jwt`、`micronaut-security-session`、`micronaut-security-csrf`。
- Keycloak 26.8.1、JUnit 5、Netty、Thymeleaf、SLF4J/Logback。
- 依赖由独立 Micronaut 平台管理，不混入其他子项目 BOM。

```text
micronaut-security-sso/
├── src/          # One Micronaut application, two runtime environments
├── provider/     # Keycloak realm import template, no literal credentials
├── api-test/     # HTTP Client protocol checks
├── metadata/     # Filtered build metadata
├── docs/         # Architecture and comparison
└── run.sh        # Optional Docker orchestration and client startup
```

| 服务 | 地址 | 用途 |
| --- | --- | --- |
| Keycloak | <http://localhost:9200> | 统一身份提供方，realm 为 `micronaut-sso` |
| Micronaut A | <http://localhost:9201> | 客户端 `micronaut-app-a` |
| Micronaut B | <http://localhost:9202> | 客户端 `micronaut-app-b`，environment 为 `app-b` |

## 使用 Docker 一键启动

要求 Docker Desktop 使用 Linux containers，且 `java`、`mvn`、`curl`、`openssl` 在 PATH 中。从仓库根目录执行：

```bash
bash micronaut-security-sso/run.sh
```

脚本提示输入账号 `researcher` 的密码，至少 16 个字符，不回显；两个客户端密钥随机生成。然后构建项目、启动临时 Keycloak 容器并导入 realm，最后启动两个 Micronaut 进程。

Keycloak 仅映射回环地址端口，两个应用也只监听回环地址。按 Ctrl+C 清理本脚本启动的进程和容器。默认容器不挂载数据库卷，停止后丢弃研究数据。应用日志在已忽略的 `.run/` 中。

可提前设置 `MICRONAUT_SSO_PASSWORD`、`MICRONAUT_SSO_CLIENT_A_SECRET`、`MICRONAUT_SSO_CLIENT_B_SECRET`；客户端密钥至少 16 个字符。不要将真实凭据写入源码、终端历史或 HTTP 测试文件。

## 不使用 Docker

本机可使用官方 Keycloak 26.8.1 发行包，不需要容器。

1. 在本地设置三个环境变量：研究用户密码和两个客户端密钥。可以通过不回显的终端输入设置密码，通过 `openssl rand -hex 24` 生成密钥。
2. 将 `provider/realm-micronaut-sso.json` 复制到 Keycloak 的 `data/import/` 目录。保留模板中的环境变量引用，Keycloak 导入时解析它们。
3. 在带有相同环境变量的终端中启动官方脚本。Windows 使用 `bin\kc.bat`，Linux/macOS 使用 `bin/kc.sh`，参数为 `start-dev --http-port=9200 --http-host=127.0.0.1 --hostname=http://localhost:9200 --import-realm`。
4. 在另一个终端中设置与导入时相同的两个客户端密钥，再执行：

```bash
bash micronaut-security-sso/run.sh --clients-only
```

`--clients-only` 不启动或停止外部 Keycloak，也不会自动生成新密钥。它只验证 discovery 就绪并启动两个客户端。Keycloak 默认跳过已存在的 realm；仅更改环境变量不会更新已导入用户密码或客户端密钥。

## 验证单点登录

1. 在新的无痕窗口访问[应用 A](http://localhost:9201/)，选择“通过 Keycloak 登录”。
2. 输入用户名 `researcher` 和启动时设置的密码。
3. 返回 A 后，选择“查看当前应用身份”，确认 `/api/me` 返回应用名称及当前身份。
4. 在同一窗口的新标签页访问[应用 B](http://localhost:9202/)，选择登录。
5. B 应复用 Keycloak 的中央会话，不再输入密码，但建立自己的应用 Session。
6. 在开发者工具 Network 中观察 discovery、`state`、`nonce`、`code_challenge_method=S256` 与 `/oauth/callback/keycloak`。不要分享完整授权码、令牌或 Cookie。

Cookie 不按端口隔离。本项目分别配置 A/B 的 Session、OAuth2 state 和 OIDC nonce Cookie 名称，避免同主机的两个客户端互相覆盖。访问时统一使用 `localhost`，不要与 `127.0.0.1` 混用。

## 退出与安全边界

当前版本只实现本地退出：POST `/logout` 使用同步令牌 CSRF 校验，清理当前应用的 Session。已登录请求缺少令牌返回 403；未登录请求可能返回 401。GET `/logout` 不执行退出。

本地退出不会结束 Keycloak 中央会话，也不会退出另一个应用。因此重新选择登录仍可能免密成功，这是预期行为，不是退出失败。

为避免将兼容性未验证的 GET 中央退出入口当作完整单点退出，本实验关闭客户端的自动 end-session 支持。RP 发起退出、front-channel/back-channel logout 和跨应用会话失效属于后续研究内容。

应用不向浏览器展示 ID Token、Access Token 或客户端凭据，也不把它们写入 localStorage。JWT 模块用于 OIDC 令牌验证，不意味着采用浏览器 JWT 登录模式。

## 构建、测试与说明

```bash
mvn -f micronaut-security-sso/pom.xml clean verify
```

- 自动测试 4 项，使用随机端口并关闭外部 OAuth2 接入，不依赖运行中的 Keycloak。
- 已使用真实 Keycloak 26.8.1 发行包和 Chrome 验证 A 登录、B 免密、PKCE S256/nonce、Session Cookie 隔离、已认证请求缺少 CSRF 被拒绝及有效本地退出。
- 当前开发环境未安装 Docker，因此 Docker 启动分支只做脚本检查；实际端到端测试使用官方本地发行包。
- HTTP 示例见 [OAuth2 协议检查](api-test/oauth2-research.http)。HTTP Client 和浏览器的 Cookie jar 不共享。
- IDE 可以将本目录 `pom.xml` 作为独立 Maven 项目导入。
- 内存应用 Session、Keycloak 开发模式和 HTTP 只适用于本机研究。生产环境需要 HTTPS、Secure Cookie、持久化存储、密钥和用户管理、运维及审计设计。

架构、协议及方案对比见[Micronaut SSO 研究方案](docs/01-ARCHITECTURE-Micronaut-OAuth2-SSO研究方案.md)。
