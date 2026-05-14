# BashTaskExecution 故障排查指南

## 常见问题及解决方案

### 1. 编译错误

#### 问题：`Cannot resolve symbol 'ShellDetector'`

**原因**：ShellDetector.java 文件未被编译或导入路径错误

**解决**：
```bash
# 清理并重新编译
mvn clean compile

# 或者检查 ShellDetector.java 是否在以下路径
D:\project\nas_gogs\zora-apps\dag-task\dag-agent\src\main\java\top\ilovemyhome\dagtask\agent\execution\ShellDetector.java
```

#### 问题：`JSON parsing error`

**原因**：输入 JSON 格式不正确

**解决**：
```java
// ❌ 错误
String inputJson = "{\"script\":\"echo hello\"}";  // 需要转义

// ✅ 正确
String inputJson = """
    {
        "script": "echo hello",
        "timeoutSeconds": 300
    }
    """;
```

### 2. 运行时错误

#### 问题：`ProcessBuilder cannot find shell`

**原因**：指定的 shell 不存在或路径错误

**解决方案**：

**Windows**：
```bash
# 验证 cmd.exe 存在
where cmd.exe
# 或
C:\Windows\System32\cmd.exe

# 验证 PowerShell 存在
where powershell.exe
# 或
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
```

**Linux/macOS**：
```bash
# 验证 bash 存在
which bash
# 应该返回 /bin/bash

# 验证 sh 存在
which sh
# 应该返回 /bin/sh
```

#### 问题：`Exit code 1` 或其他非零退出码

**原因**：脚本执行失败

**解决方案**：
```json
{
  "script": "echo hello && ls /nonexistent",
  "timeoutSeconds": 300
}
```

检查输出中的 stderr，它通常包含错误信息：
```
[STDERR] ls: cannot access '/nonexistent': No such file or directory
```

#### 问题：`Task timed out after X seconds`

**原因**：脚本超过设置的超时时间

**解决**：
1. 增加 `timeoutSeconds`
2. 优化脚本性能
3. 检查脚本是否进入死循环

```json
{
  "script": "sleep 10",
  "timeoutSeconds": 15  // 增加超时
}
```

### 3. 跨平台问题

#### 问题：Windows 上命令 `bash: ls: command not found`

**原因**：在 Windows 的 cmd.exe 上使用 Unix 命令

**解决**：
```json
// ❌ 错误：使用 bash 的命令在 cmd.exe 上
{
  "script": "ls -la"
}

// ✅ 正确：使用 cmd.exe 命令或指定 bash
{
  "script": "dir"  // 如果是 cmd.exe
  // 或使用 bash
  "shell": "bash"  // 需要装 Git Bash 或 WSL
}
```

#### 问题：Linux 上 `cmd.exe: not found`

**原因**：显式指定了 Windows 的 cmd.exe，但系统是 Linux

**解决**：
```java
// ✅ 推荐：使用自动检测
String shell = ShellDetector.getDefaultShell();  // 自动返回适当的 shell

// 或明确检查
if (ShellDetector.isWindows()) {
    shell = "cmd.exe";
} else {
    shell = "bash";
}
```

#### 问题：环境变量在 Windows 上不生效

**原因**：使用了 Unix 风格的变量引用

**解决**：
```json
// ❌ 错误：Unix 风格
{
  "script": "echo $MY_VAR",
  "env": {"MY_VAR": "value"},
  "shell": "cmd.exe"
}

// ✅ 正确：Windows 风格
{
  "script": "echo %MY_VAR%",
  "env": {"MY_VAR": "value"},
  "shell": "cmd.exe"
}
```

#### 问题：工作目录在 Windows/Linux 上路径格式不同

**原因**：路径分隔符不同

**解决**：
```java
// 使用 Java 的跨平台路径
import java.nio.file.Paths;

String workingDir = Paths.get("path", "to", "dir").toString();
// 自动转换为平台特定的路径分隔符

// 或使用 File
String workingDir = new java.io.File("path", "to", "dir").getAbsolutePath();
```

### 4. 测试问题

#### 问题：测试在 Windows 上被跳过

**原因**：测试标记为 `@DisabledOnWindows`

**解决**：
```java
// 这���测试在 Windows 上被跳过（预期行为）
@Test
@DisabledOnWindows
void testUnixSpecificFeature() {
    // 仅在 Unix 上运行
}

// 使用 @EnabledOnWindows 来指定 Windows 特定的测试
@Test
@EnabledOnWindows
void testWindowsSpecificFeature() {
    // 仅在 Windows 上运行
}
```

#### 问题���`testTimeout` 超时

**原因**：系统繁忙或 `sleep` 命令不可用

**解决**：
```bash
# Windows：使用 timeout 命令
# 或修改测试timeout值

# 增加测试超时
<maven.surefire.timeout>60</maven.surefire.timeout>
```

#### 问题：找不到测试���源文件

**原��**：资源文件路径不对

**解决**：
```bash
# 确保资源文件在正确位置
D:\project\nas_gogs\zora-apps\dag-task\dag-agent\src\test\resources

# 在代码中正确引用
ClassLoader.getSystemClassLoader().getResourceAsStream("test-file.txt")
```

### 5. 性能问题

#### 问题：任务执行非常慢

**原因**可能有：
1. 进程创建开销大
2. I/O 操作缓慢
3. 系统资源不足

**优化方案**：
```java
// 1. 批量处理任务而不是逐个执行
List<Task> tasks = getTasks();
tasks.stream().parallel()  // 并行执行
    .map(this::executeTask)
    .collect(Collectors.toList());

// 2. 使用线程池
ExecutorService executor = Executors.newFixedThreadPool(10);
for (Task task : tasks) {
    executor.submit(() -> executeTask(task));
}

// 3. 减少日志输出
logger.setLevel(Level.WARN);
```

#### 问题：内存泄漏或占用过高

**原因**：Process 或 Thread 未正确释放

**解决**：
```java
// 确保 ProcessBuilder 的资源被释放
Process process = null;
try {
    ProcessBuilder pb = new ProcessBuilder(...);
    process = pb.start();
    // ... 处理输出和错误流
} finally {
    if (process != null) {
        process.destroyForcibly();
        process.waitFor();
    }
}
```

### 6. 日志问题

#### ��题：没有看到任何日志输出

**原因**：日志级别未配置正确或 logger 为 null

**解决**：
1. 检查配置文件（logback.xml）
2. 确保 `AbstractTaskExecution.setupLogger()` 被调用

```xml
<!-- 在 src/main/resources/logback.xml 中 -->
<configuration>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

#### 问题：日志太多，输出混乱

**原因**：并发任务同时输出日志

**解决**：
```xml
<!-- 使用异步日志器 -->
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <appender-ref ref="FILE"/>
</appender>
```

### 7. 特定平台问题

#### Windows 特定

##### 问题：`特殊字符显示为乱码`

**原因**：编码问题

**解决**：
```java
// 在处理输出时指定编码
BufferedReader reader = new BufferedReader(
    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
);
```

##### 问题：`长路径问题（超过 260 字符）`

**原因**：Windows 的路径长度限制

**解决**：
```java
// 使用扩展路径前缀
String extendedPath = "\\\\?\\" + absolutePath;
```

#### Linux/macOS 特定

##### 问题：`权限拒绝（Permission denied）`

**原因**：脚本或文件没有执行权限

**解决**：
```bash
# 给脚本添加执行权限
chmod +x script.sh

# 或在脚本中指定
/bin/bash script.sh
```

##### 问题：`找不到命令`

**原因**：命令不在 PATH 中

**解决**：
```bash
# 使用完整路径
/usr/bin/python3 script.py

# 或检查 PATH
echo $PATH
```

### 8. 调试技巧

#### 启用详细日志
```java
// 设置日志级别为 DEBUG
((ch.qos.logback.classic.Logger)logger).setLevel(Level.DEBUG);
```

#### 打印系统信息来验证配置
```java
// 验证系统配置
System.out.println("OS: " + ShellDetector.getOsName());
System.out.println("Default Shell: " + ShellDetector.getDefaultShell());
System.out.println("Is Windows: " + ShellDetector.isWindows());
System.out.println("Is Linux: " + ShellDetector.isLinux());
```

#### 测试单个组件
```java
// 测试 ShellDetector
String[] cmd = ShellDetector.buildCommandArray("bash", "echo test");
System.out.println(Arrays.toString(cmd));

// 测试 ProcessBuilder
ProcessBuilder pb = new ProcessBuilder(cmd);
Process p = pb.start();
int exitCode = p.waitFor();
System.out.println("Exit code: " + exitCode);
```

### 9. 获取帮助

如果以上方法都无法解决问题，请：

1. **收集诊断信息**：
   ```bash
   # 系统信息
   java -version
   mvn -version
   uname -a  # Linux/macOS
   systeminfo  # Windows
   ```

2. **检查完整日志**：
   - 查看任务执行的完整日志输出
   - 包括 stdout 和 stderr

3. **创建最小复现案例**：
   ```json
   {
     "script": "echo hello",
     "timeoutSeconds": 300
   }
   ```

4. **查看源代码**：
   - ShellDetector.java
   - BashTaskExecution.java
   - 相关测试用例

## 常用命令速查

### Maven

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 编译并跳过测试
mvn clean compile -DskipTests

# 特定测试
mvn test -Dtest=ShellDetectorTest

# 打包
mvn package -DskipTests
```

### 系统诊断

```bash
# 检查 shell 可用性
which bash  # Unix
where cmd.exe  # Windows

# 测试脚本执行
bash -c "echo test"
cmd /c "echo test"

# 查看环境变量
env  # Unix
set  # Windows cmd
$env:Path  # PowerShell
```

## 相关文档链接

- ProcessBuilder 文档：
  https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ProcessBuilder.html

- JUnit 5 条件执行：
  https://junit.org/junit5/docs/current/user-guide/#writing-tests-conditional-execution

- Logback 配置：
  https://logback.qos.ch/manual/configuration.html

