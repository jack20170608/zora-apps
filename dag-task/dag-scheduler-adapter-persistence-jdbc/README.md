# dag-scheduler-adapter-persistence-jdbc

实现 `dag-scheduler-domain` 中 `port.out` 下定义的 Repository / UnitOfWork 接口，基于 zora-jdbi + zora-rdb + Flyway。

## 职责
- Repository 实现（`adapter.persistence.jdbc` 包）
- `UnitOfWork` 的 Jdbi 实现（真事务）
- Flyway 迁移文件（`src/main/resources/db/migration/`）
- 在 adapter 边界把 `JdbiException` / `SQLException` 翻译为 `port.out` 定义的 `PersistenceException` / `OptimisticLockException`

## 状态
当前为空骨架，dao 实现与迁移文件将在步骤 3 从旧 `dag-scheduler` 模块迁入。
