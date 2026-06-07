# dag-scheduler 六边形重构 — 步骤 1：模块骨架

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不动现有 `dag-scheduler` / `dag-scheduler-muserver` 任何代码的前提下，建好 4 个新模块的空骨架，配齐 pom、metadata、README、ArchUnit 守护规则，为后续 3 个迁移步骤奠定基础。

**Architecture:** Ports & Adapters（六边形）。新建 `dag-scheduler-domain`（纯领域+ports，无任何框架依赖）、`dag-scheduler-adapter-persistence-jdbc`、`dag-scheduler-adapter-web-muserver`、`dag-scheduler-app`（手工 DI 组装入口）四个 Maven 模块。本步骤只建骨架与守护测试，不搬业务代码。

**Tech Stack:** Java 25 / Maven / JUnit 5 / AssertJ / ArchUnit 1.3.0 / SLF4J。Adapter 层遵循 zora 框架约定：persistence 用 `zora-jdbi` + `zora-rdb` + `zora-json` + Flyway；web 用 `zora-muserver` + `zora-common` + `mu-server` + Jackson。**Domain 层零依赖**——只允许 `dag-si` + SLF4J，连 zora-* 也不要。

**关键决策（执行时锁定）：** 经与人类伙伴确认，adapter 模块**沿用现有 zora 包装**而非直接依赖 raw Jdbi/MuServer/Flyway，避免与项目约定脱节，降低后续步骤 3 代码迁移成本。Domain 模块保持纯净。

**前置阅读：** `dag-task/docs/superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md` —— 本计划严格按该 spec 的 §2、§4.1、§5（步骤 1）执行。

**验收标准（本步骤结束时）：**
1. `mvn -pl dag-task/dag-scheduler -am clean verify` 全绿
2. `dag-allinone` 不受影响（旧模块未改）
3. 新 4 个模块各自能编译，各自有 1 个占位单元测试通过
4. `dag-scheduler-domain` 模块内的 ArchUnit 测试存在但**当前只覆盖空目录**（暂以 `@Disabled` 标记，注释说明"待步骤 2 搬入 domain 代码后启用"）

---

## File Structure

新建以下文件（所有路径相对仓库根 `D:/project/zora-apps/`）：

**根 pom 修改：**
- Modify: `dag-task/pom.xml` — `<modules>` 段增加 4 个新模块；`<dependencyManagement>` 段增加 4 个新模块的 GAV

**dag-scheduler-domain 模块：**
- Create: `dag-task/dag-scheduler-domain/pom.xml`
- Create: `dag-task/dag-scheduler-domain/README.md`
- Create: `dag-task/dag-scheduler-domain/metadata/metadata.json`
- Create: `dag-task/dag-scheduler-domain/src/main/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-domain/src/test/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/domain/.gitkeep`
- Create: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/application/.gitkeep`
- Create: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/in/.gitkeep`
- Create: `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/.gitkeep`
- Create: `dag-task/dag-scheduler-domain/src/test/java/top/ilovemyhome/dagtask/scheduler/SmokeTest.java`
- Create: `dag-task/dag-scheduler-domain/src/test/java/top/ilovemyhome/dagtask/scheduler/arch/HexagonalArchitectureTest.java`

**dag-scheduler-adapter-persistence-jdbc 模块：**
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/pom.xml`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/README.md`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/metadata/metadata.json`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/src/main/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/src/test/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/persistence/jdbc/.gitkeep`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/persistence/jdbc/SmokeTest.java`

**dag-scheduler-adapter-web-muserver 模块：**
- Create: `dag-task/dag-scheduler-adapter-web-muserver/pom.xml`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/README.md`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/metadata/metadata.json`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/main/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/test/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/.gitkeep`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/SmokeTest.java`

**dag-scheduler-app 模块：**
- Create: `dag-task/dag-scheduler-app/pom.xml`
- Create: `dag-task/dag-scheduler-app/README.md`
- Create: `dag-task/dag-scheduler-app/metadata/metadata.json`
- Create: `dag-task/dag-scheduler-app/src/main/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-app/src/test/resources/.gitkeep`
- Create: `dag-task/dag-scheduler-app/src/main/java/top/ilovemyhome/dagtask/scheduler/app/.gitkeep`
- Create: `dag-task/dag-scheduler-app/src/test/java/top/ilovemyhome/dagtask/scheduler/app/SmokeTest.java`

---

## Task 1：在根 pom 注册 4 个新模块

**Files:**
- Modify: `dag-task/pom.xml`（`<modules>` 段、`<dependencyManagement>` 段）

- [ ] **Step 1：读取当前 pom**

Run: 在编辑器打开 `dag-task/pom.xml`，定位 `<modules>` 段（约 14-25 行）以及 `<dependencyManagement>` 段中 dag-scheduler 相关条目。

- [ ] **Step 2：在 `<modules>` 段中 `dag-scheduler-muserver` 之后插入 4 行**

替换原 `<modules>` 段，目标内容为：

```xml
<modules>
    <module>dag-si</module>
    <module>dag-scheduler</module>
    <module>dag-scheduler-muserver</module>
    <module>dag-scheduler-domain</module>
    <module>dag-scheduler-adapter-persistence-jdbc</module>
    <module>dag-scheduler-adapter-web-muserver</module>
    <module>dag-scheduler-app</module>
    <module>dag-admin</module>
    <module>dag-admin-muserver</module>
    <module>dag-agent</module>
    <module>dag-agent-muserver</module>
    <module>dag-agent-cli</module>
    <module>dag-allinone</module>
    <module>dag-allinone-muserver</module>
</modules>
```

- [ ] **Step 3：在 `<dependencyManagement>` 段中现有 `dag-scheduler` GAV 之后插入 4 条**

在 `dag-scheduler-muserver` 的 `<dependency>` 块之后插入：

```xml
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-scheduler-domain</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-scheduler-adapter-persistence-jdbc</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-scheduler-adapter-web-muserver</artifactId>
    <version>${revision}</version>
</dependency>
<dependency>
    <groupId>top.ilovemyhome.dagtask</groupId>
    <artifactId>dag-scheduler-app</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 4：验证根 pom 仍然可解析**

Run: `mvn -f dag-task/pom.xml -N help:effective-pom -q | tail -5`
Expected: 命令成功（exit code 0），输出包含 `</project>`。**此时**会报模块目录不存在，是正常的，**等所有模块骨架创建后再 verify**。先继续后续 task。

- [ ] **Step 5：暂不 commit。** 等所有模块骨架建好再统一 commit（Task 9）。

---

## Task 2：创建 dag-scheduler-domain 模块

**Files:**
- Create: `dag-task/dag-scheduler-domain/pom.xml`
- Create: `dag-task/dag-scheduler-domain/README.md`
- Create: `dag-task/dag-scheduler-domain/metadata/metadata.json`
- Create: 4 个 `.gitkeep` 占位 + main/test resources `.gitkeep`

**注意：** `dag-scheduler-domain` 是**零基础设施依赖**的模块。pom 中**不允许**出现 jdbi / muserver / flyway / jackson / spring / micronaut / jakarta 等依赖。仅允许 `dag-si`、SLF4J、测试相关（含 ArchUnit）。

- [ ] **Step 1：写 pom.xml**

Create `dag-task/dag-scheduler-domain/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>dag-scheduler-domain</artifactId>
    <name>dag-scheduler-domain</name>
    <description>Pure domain model and ports for dag-scheduler (zero infrastructure dependencies).</description>

    <dependencies>
        <!-- 仅允许：领域共享类型 -->
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-si</artifactId>
        </dependency>

        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- ArchUnit: 守护六边形依赖方向，仅 test scope -->
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>1.3.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2：写 README.md**

Create `dag-task/dag-scheduler-domain/README.md`：

```markdown
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

ArchUnit 测试（`src/test/java/.../arch/HexagonalArchitectureTest.java`）在 CI 中强制守护此约束。

## 状态
当前为空骨架，业务代码将在步骤 2 从旧 `dag-scheduler` 模块迁入。
```

- [ ] **Step 3：写 metadata.json**

Create `dag-task/dag-scheduler-domain/metadata/metadata.json`：

```json
{
  "groupId": "@project.groupId@",
  "artifactId": "@project.artifactId@",
  "description": "@project.description@",
  "version": "@project.version@",
  "scmUrl": "@project.scmUrl@"
}
```

- [ ] **Step 4：在 pom.xml 中启用 metadata 过滤**

修改刚写的 `dag-task/dag-scheduler-domain/pom.xml`，在 `</dependencies>` 之后插入：

```xml
<build>
    <resources>
        <resource>
            <directory>metadata</directory>
            <filtering>true</filtering>
            <targetPath>metadata</targetPath>
        </resource>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>false</filtering>
        </resource>
    </resources>
</build>
```

- [ ] **Step 5：建占位目录**

Create 以下空文件（用于占位 git 跟踪）：
- `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/domain/.gitkeep`
- `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/application/.gitkeep`
- `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/in/.gitkeep`
- `dag-task/dag-scheduler-domain/src/main/java/top/ilovemyhome/dagtask/scheduler/port/out/.gitkeep`
- `dag-task/dag-scheduler-domain/src/main/resources/.gitkeep`
- `dag-task/dag-scheduler-domain/src/test/resources/.gitkeep`

每个文件内容为单行：`# placeholder — replaced when real code lands`

- [ ] **Step 6：写一个最小冒烟测试（先让模块能 verify）**

Create `dag-task/dag-scheduler-domain/src/test/java/top/ilovemyhome/dagtask/scheduler/SmokeTest.java`：

```java
package top.ilovemyhome.dagtask.scheduler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test ensuring the empty module compiles and JUnit + AssertJ are wired correctly.
 * Will be removed once real domain code arrives in step 2.
 */
class SmokeTest {
    @Test
    void module_compiles_and_test_framework_runs() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
```

- [ ] **Step 7：编译该模块**

Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-domain -am compile -q`
Expected: BUILD SUCCESS。如果失败，先看 `dag-si` 是否已 install（如未，`mvn -f dag-task/pom.xml -pl dag-si -am install -q -DskipTests`）。

---

## Task 3：创建 dag-scheduler-adapter-persistence-jdbc 模块

**Files:**
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/pom.xml`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/README.md`
- Create: `dag-task/dag-scheduler-adapter-persistence-jdbc/metadata/metadata.json`
- Create: 占位 `.gitkeep` 若干 + SmokeTest

**依赖范围：** `dag-scheduler-domain` + zora 持久化栈（`zora-jdbi` + `zora-rdb` + `zora-json`）+ Flyway。沿用现有项目约定，不直接引用 raw `jdbi3-core` / `HikariCP`。

- [ ] **Step 1：写 pom.xml**

Create `dag-task/dag-scheduler-adapter-persistence-jdbc/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>dag-scheduler-adapter-persistence-jdbc</artifactId>
    <name>dag-scheduler-adapter-persistence-jdbc</name>
    <description>JDBC/Jdbi persistence adapter implementing dag-scheduler-domain outbound ports.</description>

    <dependencies>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-domain</artifactId>
        </dependency>

        <!-- zora persistence stack (sticks to project convention) -->
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-jdbi</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-rdb</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-json</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-common</artifactId>
        </dependency>

        <!-- Flyway (project already uses these explicitly in dag-scheduler-muserver) -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- PostgreSQL driver (will be moved to runtime once real DAOs land) -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Embedded PostgreSQL for integration tests (matches legacy module) -->
        <dependency>
            <groupId>io.zonky.test</groupId>
            <artifactId>embedded-postgres</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>metadata</directory>
                <filtering>true</filtering>
                <targetPath>metadata</targetPath>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
        </resources>
    </build>
</project>
```

**注意：** 所有依赖版本都由父 BOM (`zora-bom`) 或父 `dag-task/pom.xml` 管理（参照旧 `dag-scheduler/pom.xml` 与 `dag-scheduler-muserver/pom.xml` 同款）。如果 verify 报 missing version，回退到从旧 pom 复制 `<version>` 声明。

- [ ] **Step 2：写 README.md**

Create `dag-task/dag-scheduler-adapter-persistence-jdbc/README.md`：

```markdown
# dag-scheduler-adapter-persistence-jdbc

实现 `dag-scheduler-domain` 中 `port.out` 下定义的 Repository / UnitOfWork 接口，基于 Jdbi + Flyway + HikariCP。

## 职责
- Repository 实现（`adapter.persistence.jdbc` 包）
- `UnitOfWork` 的 Jdbi 实现（真事务）
- Flyway 迁移文件（`src/main/resources/db/migration/`）
- 在 adapter 边界把 `JdbiException` 翻译为 `port.out` 定义的 `PersistenceException` / `OptimisticLockException`

## 状态
当前为空骨架，dao 实现与迁移文件将在步骤 3 从旧 `dag-scheduler` 模块迁入。
```

- [ ] **Step 3：写 metadata.json**

同 Task 2 Step 3 的内容，路径改为 `dag-task/dag-scheduler-adapter-persistence-jdbc/metadata/metadata.json`：

```json
{
  "groupId": "@project.groupId@",
  "artifactId": "@project.artifactId@",
  "description": "@project.description@",
  "version": "@project.version@",
  "scmUrl": "@project.scmUrl@"
}
```

- [ ] **Step 4：建占位目录**

Create 以下 `.gitkeep`，内容均为 `# placeholder — replaced when real code lands`：
- `dag-task/dag-scheduler-adapter-persistence-jdbc/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/persistence/jdbc/.gitkeep`
- `dag-task/dag-scheduler-adapter-persistence-jdbc/src/main/resources/.gitkeep`
- `dag-task/dag-scheduler-adapter-persistence-jdbc/src/test/resources/.gitkeep`

- [ ] **Step 5：写 SmokeTest**

Create `dag-task/dag-scheduler-adapter-persistence-jdbc/src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/persistence/jdbc/SmokeTest.java`：

```java
package top.ilovemyhome.dagtask.scheduler.adapter.persistence.jdbc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test ensuring the empty module compiles and the test framework runs.
 * Real Repository + UnitOfWork integration tests arrive in step 3.
 */
class SmokeTest {
    @Test
    void module_compiles_and_test_framework_runs() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
```

- [ ] **Step 6：编译该模块**

Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-adapter-persistence-jdbc -am compile -q`
Expected: BUILD SUCCESS。如失败，按 Step 1 末尾的提示处理 version 问题。

---

## Task 4：创建 dag-scheduler-adapter-web-muserver 模块

**Files:**
- Create: `dag-task/dag-scheduler-adapter-web-muserver/pom.xml`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/README.md`
- Create: `dag-task/dag-scheduler-adapter-web-muserver/metadata/metadata.json`
- Create: 占位 `.gitkeep` + SmokeTest

**依赖范围：** `dag-scheduler-domain` + zora web 栈（`zora-muserver` + `zora-common` + `zora-json`）+ raw `mu-server` + Jackson。沿用现有项目约定。

- [ ] **Step 1：写 pom.xml**

Create `dag-task/dag-scheduler-adapter-web-muserver/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>dag-scheduler-adapter-web-muserver</artifactId>
    <name>dag-scheduler-adapter-web-muserver</name>
    <description>MuServer-based web adapter exposing dag-scheduler-domain inbound ports as HTTP endpoints.</description>

    <dependencies>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-domain</artifactId>
        </dependency>

        <!-- zora web stack (matches legacy dag-scheduler-muserver) -->
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-muserver</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-common</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-json</artifactId>
        </dependency>

        <!-- raw MuServer + Jackson (legacy module declares them explicitly too) -->
        <dependency>
            <groupId>io.muserver</groupId>
            <artifactId>mu-server</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>metadata</directory>
                <filtering>true</filtering>
                <targetPath>metadata</targetPath>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
        </resources>
    </build>
</project>
```

如出现 mu-server 版本缺失，参照 `dag-task/dag-scheduler-muserver/pom.xml` 复制 `<version>` 声明。

- [ ] **Step 2：写 README.md**

Create `dag-task/dag-scheduler-adapter-web-muserver/README.md`：

```markdown
# dag-scheduler-adapter-web-muserver

调用 `dag-scheduler-domain` 中 `port.in` 下定义的 UseCase，把 HTTP 请求转换为 Command/Result 对象。

## 职责
- 路由注册与 MuServer Handler 实现（`adapter.web.muserver` 包）
- HTTP DTO ↔ Command/Result 映射
- `DomainException` → HTTP 状态码映射（`ExceptionMapper`）
- JWT / Cookie 鉴权适配

## 状态
当前为空骨架，控制器将在步骤 3 从旧 `dag-scheduler-muserver` 模块迁入。
```

- [ ] **Step 3：写 metadata.json**

同 Task 2 Step 3，路径改为 `dag-task/dag-scheduler-adapter-web-muserver/metadata/metadata.json`。

- [ ] **Step 4：建占位目录**

Create 以下 `.gitkeep`：
- `dag-task/dag-scheduler-adapter-web-muserver/src/main/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/.gitkeep`
- `dag-task/dag-scheduler-adapter-web-muserver/src/main/resources/.gitkeep`
- `dag-task/dag-scheduler-adapter-web-muserver/src/test/resources/.gitkeep`

- [ ] **Step 5：写 SmokeTest**

Create `dag-task/dag-scheduler-adapter-web-muserver/src/test/java/top/ilovemyhome/dagtask/scheduler/adapter/web/muserver/SmokeTest.java`：

```java
package top.ilovemyhome.dagtask.scheduler.adapter.web.muserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test ensuring the empty module compiles and the test framework runs.
 * Real HTTP handler tests arrive in step 3.
 */
class SmokeTest {
    @Test
    void module_compiles_and_test_framework_runs() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
```

- [ ] **Step 6：编译该模块**

Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-adapter-web-muserver -am compile -q`
Expected: BUILD SUCCESS。

---

## Task 5：创建 dag-scheduler-app 模块

**Files:**
- Create: `dag-task/dag-scheduler-app/pom.xml`
- Create: `dag-task/dag-scheduler-app/README.md`
- Create: `dag-task/dag-scheduler-app/metadata/metadata.json`
- Create: 占位 `.gitkeep` + SmokeTest

**依赖范围：** `dag-scheduler-domain` + 两个 adapter。**唯一**允许同时依赖所有 3 个模块的地方。

- [ ] **Step 1：写 pom.xml**

Create `dag-task/dag-scheduler-app/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>top.ilovemyhome.dagtask</groupId>
        <artifactId>dag-task</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>dag-scheduler-app</artifactId>
    <name>dag-scheduler-app</name>
    <description>Assembly entry point for dag-scheduler (manual DI wiring + main).</description>

    <dependencies>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-adapter-persistence-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>top.ilovemyhome.dagtask</groupId>
            <artifactId>dag-scheduler-adapter-web-muserver</artifactId>
        </dependency>

        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>metadata</directory>
                <filtering>true</filtering>
                <targetPath>metadata</targetPath>
            </resource>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
        </resources>
    </build>
</project>
```

- [ ] **Step 2：写 README.md**

Create `dag-task/dag-scheduler-app/README.md`：

```markdown
# dag-scheduler-app

`dag-scheduler` 六边形架构的**组装入口**。手工 DI 把 domain + 两个 adapter 串起来。

## 职责
- `SchedulerContext`：手工 DI 容器（构造函数注入，无反射、无注解）
- `SchedulerBootstrap` / `main()`
- `SchedulerConfig` 加载（yaml / env 读取）

## 约束
- 这是**唯一**允许同时依赖 domain + 所有 adapter 的模块。
- 不引入 Spring / Guice / Avaje。命名与构造函数注入风格按"Avaje 友好"组织，将来如需升级 DI 容器，业务代码零修改。

## 状态
当前为空骨架，组装代码将在步骤 4 与 `dag-allinone` 切换同步落地。
```

- [ ] **Step 3：写 metadata.json**

同 Task 2 Step 3，路径改为 `dag-task/dag-scheduler-app/metadata/metadata.json`。

- [ ] **Step 4：建占位目录**

Create 以下 `.gitkeep`：
- `dag-task/dag-scheduler-app/src/main/java/top/ilovemyhome/dagtask/scheduler/app/.gitkeep`
- `dag-task/dag-scheduler-app/src/main/resources/.gitkeep`
- `dag-task/dag-scheduler-app/src/test/resources/.gitkeep`

- [ ] **Step 5：写 SmokeTest**

Create `dag-task/dag-scheduler-app/src/test/java/top/ilovemyhome/dagtask/scheduler/app/SmokeTest.java`：

```java
package top.ilovemyhome.dagtask.scheduler.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test ensuring the empty module compiles and the test framework runs.
 * Real wiring tests arrive in step 4.
 */
class SmokeTest {
    @Test
    void module_compiles_and_test_framework_runs() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
```

- [ ] **Step 6：编译该模块**

Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-app -am compile -q`
Expected: BUILD SUCCESS。

---

## Task 6：写 ArchUnit 守护测试（先标 `@Disabled`，等步骤 2 启用）

**Files:**
- Create: `dag-task/dag-scheduler-domain/src/test/java/top/ilovemyhome/dagtask/scheduler/arch/HexagonalArchitectureTest.java`

ArchUnit 的规则在 step 1 不能 fail：因为 domain 包当前是空的，规则跑起来也没匹配，但**我们要让它存在并能编译**，等步骤 2 搬入 domain 代码后去掉 `@Disabled`。

- [ ] **Step 1：先写"会失败的状态"的测试断言（TDD 顺序）**

Create `dag-task/dag-scheduler-domain/src/test/java/top/ilovemyhome/dagtask/scheduler/arch/HexagonalArchitectureTest.java`：

```java
package top.ilovemyhome.dagtask.scheduler.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Hexagonal architecture guard rails for dag-scheduler-domain.
 * <p>
 * Currently {@link Disabled @Disabled} because the module is empty (skeleton step 1).
 * Enable in step 2 after real domain / port code is moved in from the legacy
 * dag-scheduler module. The rules are written now so the contract is visible
 * and the enabling change in step 2 is a single annotation removal.
 */
@Disabled("Enable in step 2 after domain code lands; rules pre-written for visibility.")
class HexagonalArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("top.ilovemyhome.dagtask.scheduler");

    @Test
    void domain_application_and_ports_must_not_depend_on_any_framework() {
        noClasses().that().resideInAnyPackage(
                "..scheduler.domain..",
                "..scheduler.application..",
                "..scheduler.port..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "io.muserver..",
                "org.jdbi..",
                "org.springframework..",
                "io.micronaut..",
                "jakarta..",
                "javax.servlet..",
                "javax.sql..",
                "java.sql..",
                "com.fasterxml.jackson..",
                "org.flywaydb..",
                "com.zaxxer.hikari..",
                // zora wrappers are infrastructure too — adapters may use them, domain may not
                "top.ilovemyhome.zora.jdbi..",
                "top.ilovemyhome.zora.rdb..",
                "top.ilovemyhome.zora.muserver..",
                "top.ilovemyhome.zora.json..",
                "top.ilovemyhome.zora.httpclient..",
                "top.ilovemyhome.zora.config..",
                "top.ilovemyhome.zora.static..")
            .because("dag-scheduler-domain must remain zero-infrastructure (spec §1)")
            .check(CLASSES);
    }

    @Test
    void application_layer_must_not_depend_on_inbound_ports() {
        // application services IMPLEMENT inbound ports — but must not USE other inbound ports.
        // Cross-use case orchestration happens via outbound ports only.
        noClasses().that().resideInAPackage("..scheduler.application..")
            .should().dependOnClassesThat().resideInAPackage("..scheduler.port.in..")
            .because("application services may implement, but not consume, inbound ports")
            .check(CLASSES);
    }

    @Test
    void domain_layer_must_not_depend_on_ports() {
        noClasses().that().resideInAPackage("..scheduler.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..scheduler.port..",
                "..scheduler.application..")
            .because("pure domain has no knowledge of orchestration or ports (spec §1)")
            .check(CLASSES);
    }
}
```

- [ ] **Step 2：编译并跑测试，确认 `@Disabled` 生效**

Run: `mvn -f dag-task/pom.xml -pl dag-scheduler-domain test -q`
Expected: BUILD SUCCESS。`SmokeTest` 跑一个；`HexagonalArchitectureTest` 显示为 skipped。

---

## Task 7：根目录 verify，确认所有新模块都能 build

- [ ] **Step 1：先 install dag-si（其他模块依赖）**

Run: `mvn -f dag-task/pom.xml -pl dag-si -am install -q -DskipTests`
Expected: BUILD SUCCESS。

- [ ] **Step 2：单独 verify 4 个新模块（带 -am 把 dag-si 拉进来）**

Run:
```bash
mvn -f dag-task/pom.xml -pl dag-scheduler-domain,dag-scheduler-adapter-persistence-jdbc,dag-scheduler-adapter-web-muserver,dag-scheduler-app -am verify
```
Expected: BUILD SUCCESS。4 个新模块各跑 1 个 SmokeTest，HexagonalArchitectureTest skipped。

如果任何模块报 "missing version"：到旧对应模块（`dag-scheduler` 或 `dag-scheduler-muserver`）的 pom.xml 中找到该依赖的 `<version>` 声明，复制到新模块 pom 的同一 `<dependency>` 块里，重试。

- [ ] **Step 3：跑全量 verify，确认未破坏旧模块**

Run: `mvn -f dag-task/pom.xml clean verify -DskipTests=false`
Expected: BUILD SUCCESS。所有 14 个模块都 OK，包括旧 `dag-scheduler` / `dag-scheduler-muserver` / `dag-allinone`。

**如果 `dag-allinone` 启动整测耗时太长**，允许临时执行：
```bash
mvn -f dag-task/pom.xml clean verify -pl '!dag-allinone-muserver'
```
并在 commit message 里注明"allinone integration 单独验证待 step 2"。

---

## Task 8：更新 dag-task/CLAUDE.md 与 docs 编号文档

**Files:**
- Modify: `dag-task/CLAUDE.md`（增加模块列表小节）
- Create: `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`

- [ ] **Step 1：读取 `dag-task/CLAUDE.md` 当前内容**

Run: 打开 `dag-task/CLAUDE.md`，确认现有结构。

- [ ] **Step 2：在文件末尾追加一节**

Append to `dag-task/CLAUDE.md`：

```markdown

## 六边形重构进行中（自 2026-06-07 起）

`dag-scheduler` 子系统正在分 4 步迁移到 Ports & Adapters 架构。**期间旧 `dag-scheduler` / `dag-scheduler-muserver` 模块保留并继续工作**，新模块逐步建立：

| 模块 | 角色 | 阶段 |
|---|---|---|
| `dag-scheduler-domain` | 纯 domain + ports（零基础设施依赖） | 步骤 1 建骨架 |
| `dag-scheduler-adapter-persistence-jdbc` | Jdbi/Flyway 实现 port.out | 步骤 1 建骨架 |
| `dag-scheduler-adapter-web-muserver` | MuServer 实现 port.in | 步骤 1 建骨架 |
| `dag-scheduler-app` | 手工 DI 组装 + main | 步骤 1 建骨架 |

详见 `docs/superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md` 与 `docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`。

**给后续 Claude 的提示**：在此重构完成前，新增 dag-scheduler 相关代码请加到**新模块**，而不是旧 `dag-scheduler` / `dag-scheduler-muserver`。
```

- [ ] **Step 3：写编号文档**

Create `dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`：

```markdown
# 10 - 架构演进：dag-scheduler 六边形重构试点

- 启动日期：2026-06-07
- 设计文档：[`superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md`](./superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md)
- 试点范围：仅 `dag-scheduler` 子系统
- 状态：进行中

## 背景
项目长期演进可能切换 Web 框架（Spring Web / Micronaut）或持久化技术（本地文件 / KV）。为避免架构被锁死，本次重构在 `dag-scheduler` 试点 Ports & Adapters 架构，验证模式后再推广到 `dag-agent` / `dag-admin`。

## 迁移步骤
| 步骤 | 目标 | 状态 |
|---|---|---|
| 1 | 建 4 个新模块骨架 + ArchUnit 守护（不动旧代码） | 进行中 |
| 2 | 搬 domain：领域类、UseCase、ports 从旧 core 迁入 `dag-scheduler-domain` | 待办 |
| 3 | 搬 adapters：dao 迁入 persistence-jdbc；控制器迁入 web-muserver | 待办 |
| 4 | 切 allinone，删旧模块，归档总结 | 待办 |

## 新模块结构
（详见设计文档 §2）

## 验收标准
（详见设计文档 §8）

## 总结
（步骤 4 完成时回填：实际工时、踩坑记录、推广建议）
```

---

## Task 9：阶段 commit

**Files:** 全部新建/修改的文件。

- [ ] **Step 1：检查 git 状态**

Run: `git -C D:/project/zora-apps/dag-task status`
Expected: 看到 4 个新模块目录 + 根 pom 修改 + CLAUDE.md 修改 + docs/10-* 新文件。无意外修改。

- [ ] **Step 2：stage 所有变更**

Run:
```bash
cd D:/project/zora-apps && git add dag-task/pom.xml dag-task/CLAUDE.md dag-task/docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md dag-task/dag-scheduler-domain dag-task/dag-scheduler-adapter-persistence-jdbc dag-task/dag-scheduler-adapter-web-muserver dag-task/dag-scheduler-app
```
Expected: 无报错。

- [ ] **Step 3：再次 verify**

Run: `mvn -f dag-task/pom.xml clean verify -DskipTests=false`
Expected: BUILD SUCCESS。

- [ ] **Step 4：**

⚠️ **不要 commit、不要 push。** 按项目 CLAUDE.md 约定（"所有生成的代码，请不要直接 commit 以及 push 到远程代码仓库，所有代码的提交都由人工完成"），仅给用户准备好暂存区，并提示用户执行：

```bash
git commit -m "feat(scheduler): scaffold hexagonal module skeleton (step 1/4)

- Add dag-scheduler-domain (pure domain + ports, no framework deps)
- Add dag-scheduler-adapter-persistence-jdbc skeleton
- Add dag-scheduler-adapter-web-muserver skeleton
- Add dag-scheduler-app skeleton (manual DI assembly entry)
- Pre-write ArchUnit hexagonal guard rules (disabled until step 2)
- Update CLAUDE.md and docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md

Legacy dag-scheduler and dag-scheduler-muserver remain untouched and operational.
Refs: docs/superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md"
```

---

## Self-Review

**Spec coverage：**
- spec §1（原则） → Task 2 pom + Task 6 ArchUnit ✅
- spec §2.1（模块）→ Task 1-5 全部覆盖 ✅
- spec §2.2（ArchUnit）→ Task 6 ✅
- spec §4.1 Avaje 友好留口 → Task 5 README 中声明（实际代码本步骤未涉及，符合范围）✅
- spec §5 步骤 1 → 本计划全部 ✅
- spec §6 风险 "JDK 25 + ArchUnit / Testcontainers 兼容性" → Task 7 Step 3 全量 verify 触发 ArchUnit 编译（Testcontainers 留到步骤 3）✅

**Placeholder scan：** 已检查，所有 pom / metadata / 测试代码均为完整内容，无 TODO / TBD / "fill in later"。

**Type consistency：** 本步骤未涉及业务类型，仅 SmokeTest（每个都是独立类、独立 package）。

**遗漏检查：** spec §6 提到 "Flyway 迁移文件位置变了"—— 本步骤 persistence-jdbc 模块的 pom 已声明 `src/main/resources` 为 resources 目录（默认即可），未来在该路径下建 `db/migration` 即可，无需本步骤额外配置。
