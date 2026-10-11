# host-helper

主机管理工具，提供主机的注册、查询、监控和维护等功能，支持多种主机类型和协议，方便用户对主机进行统一管理和操作。

## 项目结构

```
host-helper
├── host-helper-si          # 领域模型层，定义主机管理工具的领域模型和接口
├── host-helper-core        # 核心实现层，实现主机注册、查询、监控和维护等核心功能
├── host-helper-muserver    # 基于MuServer的主机管理中心，提供RESTful API接口
├── host-helper-dist        # 源码、测试、部署和服务分发包
└── pom.xml                 # 父POM文件，定义项目依赖和版本管理
```

## 模块说明

### host-helper-si

领域模型层，包含：
- 主机实体定义
- 协议接口定义
- 公共数据结构

### host-helper-core

核心实现层，包含：
- 主机注册和管理逻辑
- 主机连接和健康检查
- 主机监控功能实现
- 维护操作功能

### host-helper-muserver

基于MuServer的REST服务，包含：
- RESTful API端点
- 主机管理HTTP接口
- 静态资源服务
- 配置管理
- 通过 zora-sso 统一认证，并在服务端建立短期应用会话

## zora-sso 认证

`host-helper-muserver` 使用 zora-sso 授权码流程。部署前必须在 zora-sso 注册
`host-helper` 客户端，并通过受控环境变量提供配置：

```bash
export ZORA_SSO_BASE_URI="https://sso.example.com"
export ZORA_SSO_CLIENT_ID="host-helper"
export ZORA_SSO_CLIENT_SECRET="<从密钥管理系统注入>"
export ZORA_SSO_REDIRECT_URI="https://host-helper.example.com/auth/sso/callback"
export HOST_HELPER_SECURE_COOKIE="true"
```

zora-sso 中注册的回调 URI 必须与 `ZORA_SSO_REDIRECT_URI` 完全一致。非本机部署必须使用
HTTPS 和 Secure Cookie；缺少客户端密钥时服务拒绝启动。完整流程、角色映射及安全边界见
[`docs/02-FEATURE-zora-sso认证接入.md`](docs/02-FEATURE-zora-sso认证接入.md)。

## 构建要求

- JDK 25+
- Maven 3.8+

## 构建命令

```bash
cd host-helper
mvn clean install
```

## 分发打包

从本目录运行，四种包均在 `host-helper-dist/target/` 生成 `tar.gz` 文件：

```bash
mvn clean package -Ppackage-source
mvn clean package -Ppackage-test
mvn clean package -Ppackage-deploy -DskipTests
mvn clean package -Ppackage-service -DskipTests
```

分别用于源码交付、测试报告归档、Ansible 部署资产和可执行服务（含运行期 JAR）；服务包不安装 systemd，也不包含示例密钥及环境配置。运行服务需要 JDK 25，部署环境应单独提供可信的配置与凭据。包内容及启动方式见 [`host-helper-dist/README.md`](host-helper-dist/README.md)，完整的部署后目录结构见 [`host-helper-dist/deploy/README.md`](host-helper-dist/deploy/README.md)。

## 一键部署

`.github/release.sh` 读取项目根目录的 `VERSION` 作为 Maven 构建版本，生成 service 归档后调用 Ansible playbook。默认部署到 SIT：

```bash
./.github/release.sh
```

使用 `-e` 选择已有 inventory 对应的环境：

```bash
./.github/release.sh -e uat
```

运行前需要安装 Maven 和 Ansible，并确保 `host-helper-dist/deploy/ansible/inventories/<环境>/hosts.ini` 已正确配置且目标主机可通过 SSH 访问。

## 技术栈

- Java 25
- Maven
- MuServer (HTTP Server)
- Zora Framework
- JDBI
- PostgreSQL
- Jackson (JSON)
- Flyway (Database migration)
- SLF4J + Logback (Logging)
- JUnit 5 + Mockito (Testing)

## 许可证

See the [LICENSE](../LICENSE) file for details.
