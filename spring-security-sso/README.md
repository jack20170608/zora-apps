# Spring Security 7.0 SSO 研究项目

独立于现有 `zora-sso` 的本地实验项目，使用 Spring Security 的 OAuth2/OIDC 标准实现，研究单点登录、客户端会话、PKCE、ID Token 校验与退出流程。仅限本机研究，不是生产部署模板。

## 技术与结构

- JDK 25、Maven 3.8+。
- Spring Boot 4.0.8，默认管理 Spring Security 7.0.7、Spring Framework 7.0.9。
- JUnit Jupiter 5.14.4、Mockito、SLF4J/Logback。
- 本项目使用 Spring Boot BOM 保持其框架依赖兼容，不混入其他子项目的 BOM 或自研认证协议。

```text
spring-security-sso/
├── authorization-server/  # OAuth2 authorization server + OpenID Provider
├── oidc-client/           # One implementation, two independent application instances
├── api-test/              # JetBrains HTTP Client protocol checks
├── docs/                  # Architecture and comparison notes
└── run.sh                 # Build and start all three processes
```

| 服务 | 地址 | 会话 Cookie |
| --- | --- | --- |
| 统一身份提供方 | <http://localhost:9100> | `SSO_LAB_AUTH_SESSION` |
| 应用 A | <http://localhost:9101> | `SSO_LAB_APP_A_SESSION` |
| 应用 B | <http://localhost:9102> | `SSO_LAB_APP_B_SESSION` |

Cookie 不按端口隔离，因此三个服务显式使用不同 Cookie 名称。访问时统一使用 `localhost`，不要与 `127.0.0.1` 混用。

## 一键启动

在 Windows Git Bash 中，从仓库根目录运行：

```bash
bash spring-security-sso/run.sh
```

脚本要求 `java`、`mvn`、`curl` 和 `openssl` 在 PATH 中。它会：

1. 提示输入研究账号 `researcher` 的密码，至少 16 个字符；输入不回显。
2. 临时生成两个不同的客户端密钥，不写入源码或配置文件。
3. 执行测试并构建，然后先启动授权服务器，再启动两个应用。
4. 将日志放到已忽略的 `.run/` 目录；按 Ctrl+C 停止脚本启动的服务。

如需非交互启动，预先设置 `SSO_DEMO_PASSWORD`。可选设置 `SSO_CLIENT_A_SECRET` 和 `SSO_CLIENT_B_SECRET`，每项同样至少 16 个字符。不要在终端历史或仓库文件中保存真实生产凭据。

## 验证单点登录

1. 打开新的无痕窗口，访问[应用 A](http://localhost:9101/)。
2. 选择“通过统一身份提供方登录”，输入用户名 `researcher` 和启动时设置的密码。
3. 返回应用 A 后，选择“查看当前应用身份”，确认 `/api/me` 返回当前用户。
4. 在同一个无痕窗口新开标签页，访问[应用 B](http://localhost:9102/)，再次选择登录。
5. 应用 B 应跳转经过身份提供方，但不再要求输入密码，并建立自己的本地会话。
6. 在开发者工具的 Network 中启用 Preserve log，观察 `state`、`nonce`、`code_challenge_method=S256` 与授权码回调。不要分享完整令牌、授权码、Cookie 或密钥。

本项目采用 confidential client + Authorization Code + PKCE。浏览器只获得应用会话 Cookie；授权码由应用后端兑换，令牌不会展示在页面或 `/api/me` 中。

## 验证退出

已登录页面的退出按钮提交带 CSRF 的 POST `/logout`，清除当前应用会话，再调用 OIDC `end_session_endpoint` 结束统一身份会话，并回到当前应用首页。

这不是完整的跨应用单点退出：应用 B 已建立的本地会话可以继续存在。尚未实现 back-channel/front-channel logout 通知，也未将每次业务请求与中央会话在线校验绑定。不要把“统一身份会话已退出”误认为“所有应用本地会话立即失效”。

## 单独构建或启动

```bash
mvn -f spring-security-sso/pom.xml clean verify
```

在三个终端中设置相同的研究环境变量后，可分别启动：

```bash
java -jar spring-security-sso/authorization-server/target/authorization-server-0.1.0-SNAPSHOT.jar
java -jar spring-security-sso/oidc-client/target/oidc-client-0.1.0-SNAPSHOT.jar --spring.profiles.active=app-a
java -jar spring-security-sso/oidc-client/target/oidc-client-0.1.0-SNAPSHOT.jar --spring.profiles.active=app-b
```

上述三条命令分别占用各自终端。必须先启动授权服务器，因为客户端启动时会获取 discovery 元数据。IDE 中可以将此目录的 `pom.xml` 作为独立 Maven 项目导入。

## 测试与已知限制

- 自动测试共 6 项，覆盖 discovery/JWKS、登录 CSRF、非法回调拒绝、独立客户端与 PKCE 设置、最小身份输出。
- 已用真实无头 Chrome 验证应用 A 登录、应用 B 免密登录、三个 Cookie 隔离、PKCE S256/nonce 和 RP 发起退出。浏览器检查工具未加入项目依赖。
- HTTP 请求示例见 [SSO 协议检查](api-test/sso-research.http)。HTTP Client 的 Cookie jar 与浏览器不共享，HTTP 通过不能代替浏览器检查。
- Spring Framework 7.0.9 的 SpringExtension 使用 JUnit 6 API。为了遵守仓库 JUnit 5 约定，服务测试直接通过 `SpringApplication` 管理真实服务器，不使用 `@SpringBootTest`。
- 用户、客户端、授权和会话均在内存，RSA 签名密钥每次启动重新生成；重启后应重新登录，不能复用旧授权码或令牌。
- 仅绑定本机，使用 HTTP，Cookie 没有 Secure 属性；生产环境必须改为 HTTPS、持久化密钥与数据库注册、可靠会话、密钥管理和用户目录。
- 两个研究客户端默认免授权确认，只适用于此处预注册的可信第一方应用。

## 研究资料

- [架构、协议及与现有 SSO 的对照](docs/01-ARCHITECTURE-Spring-Security-7-SSO研究方案.md)。
- [授权服务器模块说明](authorization-server/README.md)。
- [OIDC 客户端模块说明](oidc-client/README.md)。
- [Spring Security 授权服务器配置文档](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/authorization-server/configuration-model.html)。
- [Spring Security OAuth2 登录文档](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/login/core.html)。
