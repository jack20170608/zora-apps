# dag-scheduler-domain

`dag-scheduler` 六边形架构的**纯领域 + 端口**模块。

## 职责
- 领域实体、值对象、聚合（`domain` 包）
- 用例编排（`application` 包，application services 实现 inbound ports）
- inbound ports（`port.in` 包，被 web adapter 调用）
- outbound ports（`port.out` 包，被 persistence / 基础设施 adapter 实现）

## 严禁依赖
本模块**不得**引入以下任何依赖：
- MuServer / Servlet API
- Jdbi / `java.sql.*` / `javax.sql.DataSource`
- Jackson / 任何序列化库
- Flyway
- Spring / Micronaut / Guice / Avaje 等任何 DI 框架
- `jakarta.*` / `javax.*`（除标准 JDK 自带）
- **zora-jdbi / zora-muserver / zora-rdb / zora-json / zora-httpclient / zora-config / zora-static**
  （这些 zora 包装也是基础设施，仅允许 adapter 模块使用）

ArchUnit 测试（`src/test/java/.../arch/HexagonalArchitectureTest.java`）在 CI 中强制守护此约束。

## 状态
当前为空骨架，业务代码将在步骤 2 从旧 `dag-scheduler` 模块迁入。
