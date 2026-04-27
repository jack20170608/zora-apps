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
