# BashTaskExecution 快速参考卡

## 快速开始

### 在 Windows 上运行

```json
{
  "script": "echo Hello Windows",
  "shell": "cmd.exe",
  "timeoutSeconds": 300
}
```

### 在 Linux/macOS 上运行

```json
{
  "script": "echo Hello Unix",
  "shell": "bash",
  "timeoutSeconds": 300
}
```

### 自动检测（推荐）

```json
{
  "script": "echo Hello",
  "timeoutSeconds": 300
}
```

## Platform 差异对照表

| 操作 | Windows (cmd.exe) | Linux/macOS (bash) |
|------|------------------|------------------|
| 输出 | `echo text` | `echo text` |
| 列表 | `dir` | `ls -la` |
| 删除 | `del file.txt` | `rm file.txt` |
| 创建目录 | `mkdir dir` | `mkdir -p dir` |
| 连接命令 | `cmd1 & cmd2` | `cmd1 && cmd2` |
| 变量 | `%VAR%` | `$VAR` |
| 循环 | `for /L %i in (1,1,10) do echo %i` | `for i in {1..10}; do echo $i; done` |
| 注释 | `REM comment` | `# comment` |
| 条件 | `if exist file.txt (...)` | `if [ -f file.txt ]; then ...; fi` |

## 环境变量示例

### Windows
```json
{
  "script": "echo %USER_HOME%",
  "env": {"USER_HOME": "C:\\Users\\username"},
  "shell": "cmd.exe"
}
```

### Linux/macOS
```json
{
  "script": "echo $USER_HOME",
  "env": {"USER_HOME": "/home/username"},
  "shell": "bash"
}
```

## 参数验证

| 参数 | 要求 | 示例 |
|------|------|------|
| `script` | 必需，非空 | `"echo hello"` |
| `timeoutSeconds` | 可选，> 0 | `300` |
| `shell` | 可选 | `"bash"` 或 `"cmd.exe"` |
| `workingDirectory` | 可选 | `"/tmp"` 或 `"C:\\Temp"` |
| `env` | 可选 | `{"VAR": "value"}` |

## 错误处理

| 情况 | 结果 |
|------|------|
| 脚本为空 | ❌ `IllegalArgumentException` |
| 超时 | ❌ `timedOut=true` |
| 非零退出码 | ❌ `isSuccess=false` |
| 正常完成 | ✅ `isSuccess=true` |

## 日志输出示例

```
[INFO] Starting shell execution for taskId=1, scriptLength=11, OS=Windows
[INFO] Using shell: cmd.exe
[INFO] [STDOUT] Hello Windows
[INFO] Task completed successfully, exitCode=0
```

## 测试命令

### 运行所有测试
```bash
mvn clean test
```

### 运行特定测试
```bash
mvn test -Dtest=BashTaskExecutionTest
mvn test -Dtest=ShellDetectorTest
```

### 跳过测试
```bash
mvn clean compile -DskipTests
```

## 常见命令对应

### 显示内容
| Windows | Unix |
|---------|------|
| `type file.txt` | `cat file.txt` |
| `more file.txt` | `less file.txt` |
| `echo hello` | `echo hello` |

### 查找文件
| Windows | Unix |
|---------|------|
| `dir /s file.txt` | `find . -name "file.txt"` |
| `findstr "pattern" file.txt` | `grep "pattern" file.txt` |

### 系统信息
| Windows | Unix |
|---------|------|
| `systeminfo` | `uname -a` |
| `ver` | `lsb_release -a` |
| `date /t` | `date` |

## shell 选择决策树

```
Does user specify shell?
├─ 是 → 使用指定的 shell
└─ 否 → 检测操作系统
    ├─ Windows → 使用 cmd.exe
    ├─ Linux → 使用 bash
    └─ macOS → 使用 bash
```

## 返回信息示例

```json
{
  "taskId": 1,
  "isSuccess": true,
  "result": {
    "exitCode": 0,
    "stdout": "Hello World\n",
    "stderr": "",
    "timedOut": false
  }
}
```

## 故障排查速查

| 问题 | 原因 | 解决方案 |
|------|------|--------|
| `命令未找到` | shell 不匹配 | 检查操作系统，使用正确的 shell |
| 环境变量为空 | 语法错误 | Windows: `%VAR%`, Unix: `$VAR` |
| 进程未终止 | 超时未配置 | 设置 `timeoutSeconds` |
| 权限拒绝 | 文件/命令权限 | 使用 `chmod +x` (Unix) 或提升权限 |
| 找不到工作目录 | 路径不存在 | 验证 `workingDirectory` 存在 |

## 代码集成示例

```java
// 检测系统
if (ShellDetector.isWindows()) {
    // Windows 特定逻辑
    script = "dir";
} else {
    // Unix 特定逻辑
    script = "ls";
}

// 构建命令
String[] cmd = ShellDetector.buildCommandArray(null, script);

// 创建输入
TaskInput input = TaskInput.of(1L, "my-task", 
    "{\"script\":\"" + script + "\"}");

// 执行
TaskOutput output = new BashTaskExecution().execute(input);

// 检查结果
if (output.isSuccess()) {
    BashTaskExecution.Result result = 
        (BashTaskExecution.Result) output.output();
    System.out.println(result.stdout());
}
```

## 性能建议

- ✅ 重用 shell 处理大量任务
- ✅ 使用合理的超时时间
- ✅ 避免频繁创建进程
- ❌ 不要在脚本中执行阻塞操作
- ❌ 不要将超时设置过长

## 安全提示

⚠️ **始终验证和清理用户输入！**

```java
// 安全的脚本验证
String script = userInput;
if (script.contains(";") || script.contains("&&") || script.contains("|")) {
    // 可能包含命令注入，审查后再执行
}
```

