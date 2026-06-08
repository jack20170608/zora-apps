# dag-task Project Guidelines

## HTTP API Testing

### Test Location Convention
所有HTTP API测试用例都统一放在 `dag-task/api-test` 目录下集中管理，而不是分散在各个子模块中。

### Directory Structure
```
dag-task/
├── api-test/                    # HTTP测试用例统一存放目录
│   ├── 01_agent-registry-api.http
│   ├── 02_task-scheduler-api.http
│   └── 03_...
├── dag-scheduler/
├── dag-agent/
├── dag-si/
└── ...
```

### Naming Convention
- 使用数字前缀（01, 02, 03...）确保文件按顺序排列
- 使用kebab-case命名（例如：01_agent-registry-api.http）
- 扩展名使用 `.http`（IntelliJ IDEA和VS Code REST Client都支持）

### Writing HTTP Tests
- 每个测试用例应该有清晰的注释说明测试目的
- 包含正常场景、边界条件、错误处理等各种情况
- 使用 `###` 分隔不同的测试请求
- 提供响应格式参考文档

### Integration with CLAUDE.md
本项目使用CLAUDE.md记录项目特定的开发指引。在根目录的CLAUDE.md中会引用这个文件的约定。

## 其他开发约定
- 遵循zora框架的最佳实践
- 使用JDK 25
- 构建工具使用Maven
- 测试框架使用JUnit 5和Mockito
- 日志框架使用SLF4J
- 代码注释使用英文，项目文档使用中文

## Claude Code 配置

本项目已配置 `dag-task/.claude/settings.json`，允许所有 `mvn` 命令无需确认直接执行。如需调整权限规则，请编辑该文件中的 `permissions.allow` 数组。

## 六边形重构进行中（自 2026-06-07 起）

`dag-scheduler` 子系统正在分 4 步迁移到 Ports & Adapters 架构。**期间旧 `dag-scheduler` / `dag-scheduler-muserver` 模块保留并继续工作**，新模块逐步建立：

| 模块 | 角色 | 阶段 |
|---|---|---|
| `dag-scheduler-domain` | 纯 domain + ports（零基础设施依赖） | 步骤 2 ✅ Domain migrated |
| `dag-scheduler-adapter-persistence-jdbc` | zora-jdbi/Flyway 实现 port.out | 步骤 3 ✅ Adapters migrated |
| `dag-scheduler-adapter-web-muserver` | zora-muserver 实现 port.in | 步骤 1 ✅ 已建骨架（web API 类仍留在 dag-scheduler，新 adapter 待 step 4） |
| `dag-scheduler-app` | 手工 DI 组装 + main | 步骤 1 ✅ 已建骨架（SchedulerContext 待 step 4） |

详见 `docs/superpowers/specs/2026-06-07-dag-scheduler-hexagonal-design.md` 与 `docs/10-ARCHITECTURE-hexagonal-refactor-pilot.md`。

**给后续 Claude 的提示**：
- 新增 dag-scheduler 相关代码请加到 `dag-scheduler-domain` 的 `application/`、`domain/` 或 `port/` 包下，或对应 adapter 模块。
- `dag-scheduler-domain` 的 `HexagonalArchitectureTest` 已启用（3 条规则全绿）；ArchUnit `1.4.2` 是支持 JDK 25 的最低版本。
