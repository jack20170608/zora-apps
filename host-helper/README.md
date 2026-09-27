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

分别用于可复现源码交付、测试报告归档、Ansible/systemd 部署资产和可执行服务（含运行期 JAR）。运行服务需要 JDK 25；生产环境还必须提供可信配置与凭据，不能直接使用随 JAR 附带的 `local`/`sit` 示例配置。详细内容、启动及部署步骤见 [`host-helper-dist/README.md`](host-helper-dist/README.md)。

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
