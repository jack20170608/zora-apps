# BashTaskExecution 实战示例集

这个文件展示了如何在实际项目中使用 BashTaskExecution 的各种场景。

## 示例 1：基本的 Echo 命令（跨平台）

```java
import top.ilovemyhome.dagtask.agent.execution.ShellTaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

public class BasicEchoExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        // 自动选择适当的 shell
        String inputJson = """
            {
                "script": "echo Hello from BashTaskExecution",
                "timeoutSeconds": 300
            }
            """;
        
        TaskInput input = TaskInput.of(1L, "echo-task", inputJson);
        TaskOutput output = execution.execute(input);
        
        if (output.isSuccess()) {
            BashTaskExecution.Result result = 
                (BashTaskExecution.Result) output.output();
            System.out.println("Exit Code: " + result.exitCode());
            System.out.println("Output: " + result.stdout());
        } else {
            System.out.println("Error: " + output.message());
        }
    }
}
```

## 示例 2：显式 Shell 选择

```java
public class ExplicitShellExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        // 在 Windows 上使用 PowerShell
        String inputJson = """
            {
                "script": "Write-Host 'Hello from PowerShell'",
                "timeoutSeconds": 300,
                "shell": "powershell.exe"
            }
            """;
        
        TaskInput input = TaskInput.of(1L, "powershell-task", inputJson);
        TaskOutput output = execution.execute(input);
        
        handleResult(output);
    }
    
    private static void handleResult(TaskOutput output) {
        if (output.isSuccess()) {
            BashTaskExecution.Result result = 
                (BashTaskExecution.Result) output.output();
            System.out.println("Output: " + result.stdout());
        } else {
            System.out.println("Failed: " + output.message());
        }
    }
}
```

## 示例 3：环境变量和工作目录

```java
public class EnvironmentAndWorkdirExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        // 在 Unix 上
        String unixScript = """
            {
                "script": "echo Current directory: $(pwd) with var $MY_VAR",
                "workingDirectory": "/tmp",
                "env": {"MY_VAR": "CustomValue"},
                "shell": "bash",
                "timeoutSeconds": 300
            }
            """;
        
        // 在 Windows 上
        String windowsScript = """
            {
                "script": "echo Current directory: %cd% with var %MY_VAR%",
                "workingDirectory": "C:\\\\Temp",
                "env": {"MY_VAR": "CustomValue"},
                "shell": "cmd.exe",
                "timeoutSeconds": 300
            }
            """;
        
        String script = isWindows() ? windowsScript : unixScript;
        TaskInput input = TaskInput.of(1L, "env-test", script);
        TaskOutput output = execution.execute(input);
        
        handleResult(output);
    }
    
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    
    private static void handleResult(TaskOutput output) {
        // ... 处理结果
    }
}
```

## 示例 4：跨平台文件操作

```java
public class CrossPlatformFileOperationExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        String taskName = "file-operation-task";
        String script = buildPlatformSpecificScript();
        
        String inputJson = """
            {
                "script": "%s",
                "timeoutSeconds": 300
            }
            """.formatted(script);
        
        TaskInput input = TaskInput.of(1L, taskName, inputJson);
        TaskOutput output = execution.execute(input);
        
        if (output.isSuccess()) {
            BashTaskExecution.Result result = 
                (BashTaskExecution.Result) output.output();
            System.out.println("操作成功:");
            System.out.println(result.stdout());
        } else {
            System.out.println("操作失败: " + output.message());
        }
    }
    
    private static String buildPlatformSpecificScript() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        
        if (isWindows) {
            return """
                echo Creating directory...
                mkdir test_dir
                echo Creating file...
                echo hello > test_dir\\\\test.txt
                echo Listing files...
                dir test_dir
                """.replaceAll("\n", " & ");
        } else {
            return """
                echo Creating directory...
                mkdir -p test_dir
                echo Creating file...
                echo hello > test_dir/test.txt
                echo Listing files...
                ls -la test_dir
                """;
        }
    }
}
```

## 示例 5：超时处理

```java
public class TimeoutHandlingExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        // 设置很短的超时时间
        String inputJson = """
            {
                "script": "sleep 10",
                "timeoutSeconds": 2
            }
            """;
        
        TaskInput input = TaskInput.of(1L, "timeout-test", inputJson);
        TaskOutput output = execution.execute(input);
        
        if (!output.isSuccess()) {
            BashTaskExecution.Result result = 
                (BashTaskExecution.Result) output.output();
            
            if (result.timedOut()) {
                System.out.println("任务因超时被终止");
            } else {
                System.out.println("任务执行失败，退出码: " + result.exitCode());
            }
        }
    }
}
```

## 示例 6：错误码处理

```java
public class ExitCodeHandlingExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        // 执行一个会失败的命令
        String inputJson = """
            {
                "script": "ls /nonexistent",
                "timeoutSeconds": 300
            }
            """;
        
        TaskInput input = TaskInput.of(1L, "error-test", inputJson);
        TaskOutput output = execution.execute(input);
        
        BashTaskExecution.Result result = 
            (BashTaskExecution.Result) output.output();
        
        switch (result.exitCode()) {
            case 0:
                System.out.println("成功执行");
                System.out.println(result.stdout());
                break;
            case 1:
            case 2:
                System.out.println("文件或目录不存在");
                System.out.println("错误信息: " + result.stderr());
                break;
            default:
                System.out.println("未知错误，退出码: " + result.exitCode());
                break;
        }
    }
}
```

## 示例 7：使用 ShellDetector 的高级用法

```java
import top.ilovemyhome.dagtask.agent.execution.ShellDetector;

public class ShellDetectorAdvancedExample {
    public static void main(String[] args) {
        // 1. 检测操作系统
        System.out.println("OS: " + ShellDetector.getOsName());
        System.out.println("Is Windows: " + ShellDetector.isWindows());
        System.out.println("Is Linux: " + ShellDetector.isLinux());
        
        // 2. 获取合适的 shell
        String shell = ShellDetector.getDefaultShell();
        System.out.println("Default Shell: " + shell);
        
        // 3. 构建命令
        String[] cmd = ShellDetector.buildCommandArray(shell, "echo hello");
        System.out.println("Command Array: " + java.util.Arrays.toString(cmd));
        
        // 4. 验证 shell
        ShellDetector.ShellValidationResult validation = 
            ShellDetector.validateShell("bash");
        System.out.println("Validation: " + validation.getMessage());
        
        // 5. 检查 shell 类型
        if (ShellDetector.isWindowsCmd(shell)) {
            System.out.println("Using Windows cmd");
        } else if (ShellDetector.isPowerShell(shell)) {
            System.out.println("Using PowerShell");
        } else {
            System.out.println("Using Unix-like shell");
        }
    }
}
```

## 示例 8：批量任务执行

```java
public class BatchTaskExecutionExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        String[] scripts = {
            "echo Task 1",
            "echo Task 2",
            "echo Task 3"
        };
        
        for (int i = 0; i < scripts.length; i++) {
            String inputJson = """
                {
                    "script": "%s",
                    "timeoutSeconds": 300
                }
                """.formatted(scripts[i]);
            
            TaskInput input = TaskInput.of(
                (long) i + 1,
                "batch-task-" + (i + 1),
                inputJson
            );
            
            TaskOutput output = execution.execute(input);
            
            if (output.isSuccess()) {
                System.out.println("✓ Task " + (i + 1) + " completed");
            } else {
                System.out.println("✗ Task " + (i + 1) + " failed");
            }
        }
    }
}
```

## 示例 9：条件执行（基于操作系统）

```java
public class ConditionalExecutionExample {
    public static void main(String[] args) {
        BashTaskExecution execution = new BashTaskExecution();
        
        String script;
        String shell;
        
        if (ShellDetector.isWindows()) {
            script = "dir && echo Windows directory listing complete";
            shell = "cmd.exe";
        } else if (ShellDetector.isLinux()) {
            script = "ls -la && echo Linux directory listing complete";
            shell = "bash";
        } else if (ShellDetector.isMac()) {
            script = "ls -la && echo macOS directory listing complete";
            shell = "bash";
        } else {
            script = "echo Unknown OS";
            shell = ShellDetector.getDefaultShell();
        }
        
        String inputJson = """
            {
                "script": "%s",
                "shell": "%s",
                "timeoutSeconds": 300
            }
            """.formatted(script.replaceAll("\"", "\\\\\""), shell);
        
        TaskInput input = TaskInput.of(1L, "conditional-execution", inputJson);
        TaskOutput output = execution.execute(input);
        
        if (output.isSuccess()) {
            BashTaskExecution.Result result = 
                (BashTaskExecution.Result) output.output();
            System.out.println(result.stdout());
        }
    }
}
```

## 示例 10：日志处理和完整错误管理

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComprehensiveExample {
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveExample.class);
    
    public static void main(String[] args) {
        executeTask(
            1L,
            "data-processing",
            getBuildCommand(),
            buildDataProcessingScript(),
            300
        );
    }
    
    private static void executeTask(
        Long taskId,
        String taskName,
        String shell,
        String script,
        int timeoutSeconds
    ) {
        BashTaskExecution execution = new BashTaskExecution();
        
        logger.info("Starting task: {}", taskName);
        logger.debug("Shell: {}, Script length: {}", shell, script.length());
        
        String inputJson = """
            {
                "script": "%s",
                "shell": "%s",
                "timeoutSeconds": %d
            }
            """.formatted(
                script.replaceAll("\"", "\\\\\""),
                shell,
                timeoutSeconds
            );
        
        TaskInput input = TaskInput.of(taskId, taskName, inputJson);
        
        try {
            TaskOutput output = execution.execute(input);
            
            if (output.isSuccess()) {
                BashTaskExecution.Result result = 
                    (BashTaskExecution.Result) output.output();
                logger.info("Task succeeded with exit code: {}", result.exitCode());
                logger.info("Output:\n{}", result.stdout());
                if (!result.stderr().isEmpty()) {
                    logger.warn("Stderr:\n{}", result.stderr());
                }
            } else {
                BashTaskExecution.Result result = 
                    (BashTaskExecution.Result) output.output();
                logger.error("Task failed: {}", output.message());
                if (result != null && result.timedOut()) {
                    logger.error("Task timed out after {} seconds", timeoutSeconds);
                }
            }
        } catch (Exception e) {
            logger.error("Task execution error", e);
        }
    }
    
    private static String getBuildCommand() {
        return ShellDetector.isWindows() ? "cmd.exe" : "bash";
    }
    
    private static String buildDataProcessingScript() {
        if (ShellDetector.isWindows()) {
            return """
                echo Processing data...
                for /L %%i in (1,1,5) do (
                  echo Processing item %%i
                )
                echo Data processing complete
                """;
        } else {
            return """
                echo Processing data...
                for i in {1..5}; do
                  echo Processing item $i
                done
                echo Data processing complete
                """;
        }
    }
}
```

## 编译和运行示例

```bash
# 编译
javac -cp ".:dag-si.jar:dag-agent.jar" *.java

# 运行示例
java -cp ".:dag-si.jar:dag-agent.jar" BasicEchoExample

# 或使用 Maven
mvn exec:java -Dexec.mainClass="BasicEchoExample"
```

## 快速测试命令

```bash
# 验证 ShellDetector 工作正常
mvn test -Dtest=ShellDetectorTest

# 验证 BashTaskExecution 工作正常
mvn test -Dtest=BashTaskExecutionTest

# 直接运行示例（需要正确配置 classpath）
mvn compile exec:java -Dexec.mainClass="path.to.BasicEchoExample"
```

## 注意事项

1. **脚本转义**：JSON 中的脚本需要正确转义特殊字符
2. **跨平台命令**：确保使用平台相应的命令
3. **超时设置**：根据脚本复杂度设置合理的超时时间
4. **资源释放**：TaskExecution 会自动释放资源
5. **错误处理**：始终检查 output.isSuccess() 和错误信息

## 最佳实践

1. ✅ 使用 ShellDetector 进行平台检测
2. ✅ 为不同平台编写特定的脚本
3. ✅ 设置合理的超时时间
4. ✅ 记录和监控任务执行
5. ✅ 对用户输入进行验证和清理
6. ❌ 不要硬编码 shell 路径
7. ❌ 不要执行未验证的脚本
8. ❌ 不要忽略错误信息

