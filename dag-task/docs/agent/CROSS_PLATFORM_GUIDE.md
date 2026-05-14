# BashTaskExecution 跨平台使用指南

## 概述

`BashTaskExecution` 是一个支持跨平台（Windows、Linux、macOS）的任务执行器，能够自动检测操作系统并选择合适的 shell 来执行脚本。

## 核心特性

✅ **自动 Shell 检测**：根据操作系统自动选择合适的 shell
- **Windows**：默认使用 `cmd.exe`，也支持 `powershell.exe`
- **Linux/macOS**：默认使用 `bash`，也支持 `sh`

✅ **显式 Shell 指定**：支持在输入参数中显式指定 shell

✅ **跨平台脚本支持**：支持不同平台的语法差异

✅ **完整的资源管理**：正确处理进程超时、销毁和资源清理

✅ **环境变量隔离**：支持为每个任务设置独立的环境变量

✅ **工作目录支持**：支持指定任务执行的工作目录

## 返回信息

| 属性 | 说明 |
|------|------|
| `exitCode` | 进程退出码 |
| `stdout` | 标准输出内容 |
| `stderr` | 标准错误内容 |
| `timedOut` | 是否超时 |

## 在不同平台上使用

### Windows 环境

#### 使用 cmd.exe（默认）

```json
{
  "script": "echo Hello from Windows",
  "timeoutSeconds": 300,
  "shell": "cmd.exe"
}
```

#### 使用 PowerShell

```json
{
  "script": "Write-Host 'Hello from PowerShell'",
  "timeoutSeconds": 300,
  "shell": "powershell.exe"
}
```

#### 环境变量示例

```json
{
  "script": "echo %MY_VAR%",
  "env": {"MY_VAR": "Hello"},
  "shell": "cmd.exe"
}
```

#### 列出目录（cmd.exe 命令）

```json
{
  "script": "dir",
  "shell": "cmd.exe"
}
```

### Linux/macOS环境

#### 使用 bash（默认）

```json
{
  "script": "echo Hello from Unix",
  "timeoutSeconds": 300,
  "shell": "bash"
}
```

#### 使用 sh

```json
{
  "script": "echo Hello from sh",
  "timeoutSeconds": 300,
  "shell": "sh"
}
```

#### 环境变量示例

```json
{
  "script": "echo $MY_VAR",
  "env": {"MY_VAR": "Hello"},
  "shell": "bash"
}
```

#### 列出目录（bash 命令）

```json
{
  "script": "ls -la",
  "shell": "bash"
}
```

## 自动 Shell 选择

如果不指定 `shell` 参数，系统会根据操作系统自动选择：

```json
{
  "script": "echo auto-selected",
  "timeoutSeconds": 300
}
```

在 Windows 上将使用 `cmd.exe`，在 Linux/macOS 上将使用 `bash`。

## 脚本示例

### 示例 1：数据处理（跨平台）

```bash
# Unix/Linux
script: 'echo "Processing data"; for i in 1 2 3; do echo $i; done'

# Windows
script: 'echo Processing data & for /L %i in (1,1,3) do echo %i'
```

### 示例 2：参数验证

```json
{
  "script": "echo hello",
  "timeoutSeconds": 300,
  "workingDirectory": "/tmp",
  "env": {"DEBUG": "true"},
  "shell": "bash"
}
```

### 示例 3：超时处理

```json
{
  "script": "sleep 10",
  "timeoutSeconds": 1,
  "shell": "bash"
}
```
结果：任务在 1 秒后超时，进程被强制终止。

## 测试方法

### 编译和运行单元测试

```bash
# 从项目根目录
cd dag-task

# 运行所有测试
mvn clean test

# 运行特定测试类
mvn test -Dtest=ShellDetectorTest
mvn test -Dtest=BashTaskExecutionTest
```

### 在 Windows 上运行

```bash
# 编译
mvn clean compile

# 运行测试（包括 Windows 特定的测试）
mvn test

# 结果：
# - 跨平台测试（Echo、超时等）：✓ 通过
# - Windows 特定测试：✓ 通过
# - Unix 特定测试：⊘ 跳过（标记为 @DisabledOnWindows）
```

### 在 Linux/macOS 上运行

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 结果：
# - 跨平台测试：✓ 通过
# - Unix 特定测试：✓ 通过
# - Windows 特定测试：⊘ 跳过（标记为 @EnabledOnWindows）
```

## 关键类说明

### ShellDetector 工具类

这是一个工具类，提供了以下功能：

| 方法 | 说明 |
|-----|------|
| `getDefaultShell()` | 返回当前操作系统的默认 shell |
| `buildCommandArray(shell, script)` | 为指定 shell 构建命令数组 |
| `isWindowsCmd(shell)` | 检查是否为 Windows cmd.exe |
| `isPowerShell(shell)` | 检查是否为 PowerShell |
| `isWindows()` / `isLinux()` / `isMac()` | 检查当前操作系统 |
| `adaptScriptForShell(script, shell)` | 转换脚本语法以适配目标 shell |
| `validateShell(shell)` | 验证 shell 是否可用 |

### 使用示例

```java
// 获取默认 shell
String shell = ShellDetector.getDefaultShell();  // 返回 "cmd.exe" 或 "bash"

// 构建命令数组
String[] cmd = ShellDetector.buildCommandArray("bash", "echo hello");
// 返回 ["bash", "-c", "echo hello"]

// 检查操作系统
if (ShellDetector.isWindows()) {
    // 在 Windows 上执行
}

// 验证 shell
ShellDetector.ShellValidationResult result = ShellDetector.validateShell("bash");
if (result.isValid()) {
    System.out.println(result.getMessage());
}
```

## 常见问题

### Q1：在 Windows 上运行 Unix 命令时出现错误

**A**：使用正确的 shell 和命令语法。例如：
- 错误：使用 `ls` 在 Windows cmd.exe 上
- 正确：使用 `dir` 在 Windows cmd.exe 上

### Q2：环境变量在 Windows 上不起作用

**A**：检查环境变量名称和引用方式：
- Unix：`$MY_VAR` 或 `${MY_VAR}`
- Windows：`%MY_VAR%`

### Q3：如何在 Windows 上使用 bash？

**A**：需要安装 Git Bash 或 WSL，然后明确指定：
```json
{
  "script": "echo hello",
  "shell": "C:\\Program Files\\Git\\bin\\bash.exe"
}
```

### Q4：超时不起作用

**A**：确保 `timeoutSeconds` 不为空且大于 0。系统会：
1. 先尝试正常终止进程（destroy）
2. 等待 5 秒
3. 如果仍未终止，强制终止（destroyForcibly）

### Q5：如何自适应地编写脚本？

**A**：使用 `ShellDetector` 来检测系统并提供相应脚本：
```java
String script;
if (ShellDetector.isWindows()) {
    script = "dir && echo done";
} else {
    script = "ls && echo done";
}
```

## 性能考虑

- **进程创建**：每次执行都会创建新进程，有一定的开销
- **超时检查**：采用轮询方式，性能开销较小
- **流处理**：使用独立线程读取 stdout/stderr，避免死锁

## 安全建议

⚠️ **重要**：直接执行用户输入的脚本存在安全风险！

建议：
1. 对脚本内容进行验证和清理
2. 使用沙箱环境（如 Docker）
3. 限制可执行的命令范围
4. 记录所有脚本执行日志
5. 实施适当的访问控制

## 扩展和改进

未来可能的改进方向：
- 支持更多 shell（zsh, fish, PowerShell Core）
- 脚本预编译和缓存
- 并发执行管理
- 更多的资源限制选项（内存、CPU 等）
- 条件执行（基于前一个命令的结果）

## 技术细节

### 命令数组构建

根据 shell 类型，构建不同的命令数组：

| Shell | 命令数组格式 |
|-------|-----------|
| bash/sh/zsh | `["shell", "-c", "script"]` |
| cmd.exe | `["cmd.exe", "/c", "script"]` |
| PowerShell | `["powershell.exe", "-Command", "script"]` |

### 进程生命周期

1. **创建**：`ProcessBuilder.start()`
2. **监控**：两个线程分别读取 stdout 和 stderr
3. **等待**：主线程等待进程完成或超时
4. **清理**：
   - 如果超时：先 destroy，再 destroyForcibly
   - 最后等待线程完成
5. **返回结果**：包含 exitCode、输出和超时信息

## 相关文件

- `BashTaskExecution.java`：核心执行类
- `ShellDetector.java`：Shell 检测和命令构建工具
- `ShellDetectorTest.java`：ShellDetector 单元测试
- `BashTaskExecutionTest.java`：BashTaskExecution 集成测试

## 参考资源

- Java ProcessBuilder 文档
- OS-specific shell 命令参考
- JUnit 5 条件测试文档

