# BashTaskExecution 跨平台支持更新总结

## 概述

这次更新为 `BashTaskExecution` 添加了完整的跨平台支持（Windows、Linux、macOS），使其能够在不同操作系统上自动检测并使用合适的 shell。

## 主要改进

### ✅ 核心功能增强

1. **自动 Shell 检测**
   - 根据操作系统自动选择合适的 shell
   - Windows：`cmd.exe`（可选 `powershell.exe`）
   - Linux/macOS：`bash`（可选 `sh`）

2. **ShellDetector 工具类**
   - 提供跨平���的 shell 管理功能
   - 支持 shell 验证和命令数组构建
   - 支持脚本适配（Unix ↔ Windows）

3. **改进的 BashTaskExecution**
   - 支持显式和隐式 shell 选择
   - 更好的错误处理和日志记录
   - 完整的进程生命周期管理

### ✅ 测试改进

1. **ShellDetectorTest**
   - 20+ 个单元测试
   - 覆盖所有核心功能
   - 验证 shell 检测和命令构建

2. **BashTaskExecutionTest 增强**
   - 平台特定的测试（使用 @EnabledOnWindows / @DisabledOnWindows）
   - 跨平台共享测试
   - 环境变量、工作目录、超时等完整覆盖

### ✅ 文档完备

1. **CROSS_PLATFORM_GUIDE.md**（详细指南）
   - 功能说明和使用方法
   - 各平台具体示例
   - 常见问题解答
   - 扩展和改进建议

2. **QUICK_REFERENCE.md**（快速参考）
   - 命令对照表
   - 参数验证规则
   - 常见命令映射
   - 决策树

3. **TROUBLESHOOTING.md**（故障排查）
   - 9 个主要问题类别
   - 问题诊断和解决方案
   - 调试技巧
   - 性能优化建议

## 文件变更清单

### 新增文件

```
dag-task/dag-agent/src/main/java/.../execution/
├── ShellDetector.java                    [新增] 工具类
└── BashTaskExecution.java               [修改] 核心类

dag-task/dag-agent/src/test/java/.../execution/
├── ShellDetectorTest.java                [新增] 单元测试
└── BashTaskExecutionTest.java            [修改] 集成测试

dag-task/dag-agent/
├── CROSS_PLATFORM_GUIDE.md              [新增] 详细指南
├── QUICK_REFERENCE.md                   [新增] 快速参考
└── TROUBLESHOOTING.md                   [新增] 故障排查
```

## 核心类详解

### ShellDetector.java

**功能**：跨平台 shell 检测和管理

**关键方法**：
- `getDefaultShell()`：返回当前 OS 的默认 shell
- `buildCommandArray(shell, script)`：构建 ProcessBuilder 的命令数组
- `isWindows() / isLinux() / isMac()`：检测操作系统
- `validateShell(shell)`：验证 shell 可用性

**示例**：
```java
// 自动选择 shell
String shell = ShellDetector.getDefaultShell();

// 构建命令
String[] cmd = ShellDetector.buildCommandArray(shell, "echo hello");
// 返回：Windows: ["cmd.exe", "/c", "echo hello"]
//       Unix: ["bash", "-c", "echo hello"]
```

### 改进的 BashTaskExecution.java

**改进点**：
1. 使用 `ShellDetector` 自动检测 shell
2. 日志记录包含操作系统信息
3. 支持显式和隐式 shell 选择
4. 更好的资源管理和错误处理

**使用示例**：
```json
{
  "script": "echo hello",
  "timeoutSeconds": 300
  // shell 不指定则自动选择
}
```

## 测试覆盖

### ShellDetectorTest（20+ 用例）
- 操作系统检测
- Shell 类型判断
- 命令数组构建
- Shell 验证
- 脚本适配

### BashTaskExecutionTest（12+ 用例）
- 基本执行（跨平台）
- 超时处理（平台特定）
- 环境变量（平台特定）
- 工作目录支持
- 错误处理
- 多行脚本

## 测试运行

### 编译

```bash
cd dag-task
mvn clean compile
```

### 运行所有测试

```bash
mvn test
```

### 运行特定测试

```bash
# 运行 ShellDetector 单元测试
mvn test -Dtest=ShellDetectorTest

# 运行 BashTaskExecution 集成测试
mvn test -Dtest=BashTaskExecutionTest
```

## 使用场景

### 场景 1：编写跨平台任务脚本

```java
// 获取适当的脚本
String script = ShellDetector.isWindows() 
    ? "dir" 
    : "ls -la";

TaskInput input = TaskInput.of(
    1L, 
    "list-files", 
    "{\"script\":\"" + script + "\"}"
);

TaskOutput output = new BashTaskExecution().execute(input);
```

### 场景 2：自动选择 shell

```java
// 让系统自动选择
TaskInput input = TaskInput.of(
    1L,
    "auto-detect",
    "{\"script\":\"echo test\"}"
);

TaskOutput output = new BashTaskExecution().execute(input);
```

### 场景 3：显式指定 shell

```java
// 显式使用 PowerShell（仅 Windows）
TaskInput input = TaskInput.of(
    1L,
    "powershell-task",
    "{\"script\":\"Write-Host test\",\"shell\":\"powershell.exe\"}"
);

TaskOutput output = new BashTaskExecution().execute(input);
```

## 预期效果

### Windows 环境

```
✓ 默认使用 cmd.exe
✓ 支持 PowerShell
✓ 环境变量：%VAR%
✓ 命令：dir, echo, type 等
✓ 所有测试通过（包括 @EnabledOnWindows）
✓ @DisabledOnWindows 测试被跳过
```

### Linux/macOS 环境

```
✓ 默认使用 bash
✓ 支持 sh
✓ 环境变量：$VAR
✓ 命令：ls, echo, cat 等
✓ 所有测试通过（包括 @DisabledOnWindows）
✓ @EnabledOnWindows 测试被跳过
```

## 向后兼容性

✅ **完全兼容**：所有现有代码无需修改

```json
// 旧的输入格式仍然有效
{
  "script": "echo hello",
  "timeoutSeconds": 300,
  "workingDirectory": "/tmp",
  "env": {"VAR": "value"},
  "shell": "bash"
}
```

## 性能考虑

- **进程创建**：~10-50ms（取决于系统）
- **流处理**：无阻塞，使用独立线程
- **内存占用**：最小化（进程退出后立即释放）

## 安全建议

⚠️ **重要**：
1. 验证和清理用户输入的脚本
2. 使用 ProcessBuilder 的 environment 隔离环境变量
3. 实施适当的超时和资源限制
4. 记录所有执行日志便于审计

## 已知限制

1. **Shell 兼容性**：
   - 某些高级 shell 功能可能不可用
   - 脚本应遵循 POSIX 标准以确保兼容性

2. **跨平台命令**：
   - 不同 shell 的命令语法差异大
   - 建议为不同平台编写特定的脚本

3. **Windows WSL**：
   - 需要在 Windows 上明确指定 bash 路径
   - 示例：`"C:\\Program Files\\Git\\bin\\bash.exe"`

## 后续改进方向

1. **支持更多 shell**：zsh, fish, PowerShell Core
2. **脚本模板**：内置常��任务模板
3. **并发执行**：任务队列和并行执行
4. **资源限制**：内存和 CPU 限制
5. **条件执行**：基于前一步的结果执行

## 文档导航

| 文档 | 用途 |
|------|------|
| CROSS_PLATFORM_GUIDE.md | 详细功能和使用指南 |
| QUICK_REFERENCE.md | 快速参考和命令对照表 |
| TROUBLESHOOTING.md | 问题诊断和解决方案 |
| README.md（本文件） | 更新总结和快速入门 |

## 快速入门

### 1. 查看核心类

```bash
# 查看 ShellDetector
cat src/main/java/.../ShellDetector.java

# 查看改进的 BashTaskExecution
cat src/main/java/.../BashTaskExecution.java
```

### 2. 运行测试验证

```bash
mvn clean test
```

### 3. 集成到项目

```java
// 就像原来一样使用，但现在支持跨平台
TaskOutput output = new BashTaskExecution().execute(input);
```

### 4. 查阅文档

- 功能详解：CROSS_PLATFORM_GUIDE.md
- 快速参考：QUICK_REFERENCE.md
- 问题排查：TROUBLESHOOTING.md

## 验收标准

✅ **建议检查项**：

- [ ] 代码编译无误
- [ ] 所有单元测试通过
- [ ] 在 Windows 上测试
- [ ] 在 Linux/macOS 上测试
- [ ] 文档清晰易懂
- [ ] 向后兼容性验证

## 支持和反馈

如发现任何问题或有改进建议，可参考：
- TROUBLESHOOTING.md：常见问题解决
- 测试用例：实际使用示例
- 源代码注释：实现细节

