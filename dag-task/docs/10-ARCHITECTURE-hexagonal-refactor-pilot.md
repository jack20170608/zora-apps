# 10 - 架构演进：dag-scheduler 六边形重构试点

- 启动日期：2026-06-07
- 设计文档：[`superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md`](./superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md)
- 试点范围：仅 `dag-scheduler` 子系统
- 状态：进行中（步骤 1 / 4 已完成）

## 背景
项目长期演进可能切换 Web 框架（Spring Web / Micronaut）或持久化技术（本地文件 / KV）。为避免架构被锁死，本次重构在 `dag-scheduler` 试点 Ports & Adapters 架构，验证模式后再推广到 `dag-agent` / `dag-admin`。

## 迁移步骤
| 步骤 | 目标 | 状态 |
|---|---|---|
| 1 | 建 4 个新模块骨架 + ArchUnit 守护（不动旧代码） | ✅ 已完成 |
| 2 | 搬 domain：领域类、UseCase、ports 从旧 core 迁入 `dag-scheduler-domain` | 待办 |
| 3 | 搬 adapters：dao 迁入 persistence-jdbc；控制器迁入 web-muserver | 待办 |
| 4 | 切 allinone，删旧模块，归档总结 | 待办 |

## 新模块结构
```
dag-scheduler-domain/                       纯领域 + ports（零基础设施依赖）
├── src/main/java/top/ilovemyhome/dagtask/scheduler/
│   ├── domain/                             (entities, value objects, domain services)
│   ├── application/                        (use case implementations)
│   ├── port/in/                            (inbound ports：UseCase 接口、Command/Result records)
│   └── port/out/                           (outbound ports：Repository / UnitOfWork / Clock / IdGenerator / AgentDispatcher)
└── src/test/java/.../arch/HexagonalArchitectureTest.java   (ArchUnit 守护，@Disabled 待步骤 2 启用)

dag-scheduler-adapter-persistence-jdbc/     zora-jdbi/Flyway 实现 port.out
dag-scheduler-adapter-web-muserver/         zora-muserver 实现 port.in
dag-scheduler-app/                          组装入口（手工 DI + main + SchedulerConfig）
```

## 技术决策记录

### 决策 1：adapter 模块沿用 zora 包装而非 raw 库
- **背景**：原计划在 adapter 模块直接依赖 `jdbi3-core` / `HikariCP` / `mu-server` / `Jackson`。
- **决定**：改为沿用项目现有约定，adapter 模块依赖 `zora-jdbi` / `zora-rdb` / `zora-muserver` / `zora-common` / `zora-json` / `zora-config`。
- **理由**：与项目现有约定一致，迁移代码（步骤 3）零摆平；不需要重写 zora 提供的连接池/事务/默认 mapper/JSON 序列化能力。
- **影响**：ArchUnit 守护规则相应扩展，禁止 `dag-scheduler-domain` 依赖 `top.ilovemyhome.zora.jdbi..` / `..rdb..` / `..muserver..` 等基础设施包装；只允许 adapter 模块使用。

### 决策 2：手工 DI（不引入容器）
- 见设计文档 §4.1。`scheduler-app` 用手工构造函数注入，预留 Avaje Inject 升级路径。

### 决策 3：根 .gitignore 锚定 out/ 规则
- **背景**：根 `.gitignore` 第 40 行原为 `out/`（无锚定），会递归忽略任何名为 `out` 的目录——包括我们的 `port/out/` 包目录。
- **决定**：改为 `/out/`（只匹配根目录下的 IDEA 编译输出目录）。
- **理由**：避免步骤 2 写入 `port.out/DagRunRepository.java` 等文件时被静默忽略（这种 bug 极难发现）。

## 验收标准
（详见设计文档 §8）

### 步骤 1 验收结果
- ✅ 4 个新模块各自能编译，各跑 1 个 SmokeTest 通过
- ✅ `HexagonalArchitectureTest`（3 个规则）已写就，当前 `@Disabled` skip
- ✅ 旧 `dag-scheduler` / `dag-scheduler-muserver` / `dag-admin` / `dag-admin-muserver` 不受影响，全部 BUILD SUCCESS
- ⚠️ `dag-agent` 测试预存在的 jakarta JAX-RS provider + Mockito 配置问题（4 failures + 2 errors），**与本步骤无关**，待后续单独修复

## 总结
（步骤 4 完成时回填：实际工时、踩坑记录、推广建议）
