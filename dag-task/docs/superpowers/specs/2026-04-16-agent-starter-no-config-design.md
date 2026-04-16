# 设计：修改 AgentStarter 不依赖配置文件读取

## 背景

当前 `AgentStarter` 依赖 Typesafe Config 库来从配置文件读取配置信息。这种设计存在以下问题：

1. 增加了不必要的依赖耦合，调用方如果已经有了配置对象，仍然需要先从配置文件加载
2. 限制了使用场景，无法支持程序化配置方式
3. 使得单元测试更加复杂，需要依赖配置文件才能测试

为了提高 API 的灵活性并减少不必要的依赖，需要对 `AgentStarter` 进行重构。

## 设计目标

1. 移除 `AgentStarter` 对 Typesafe Config 的直接依赖
2. 提供更灵活的 API，允许调用方直接传入已经构建好的 `AgentConfiguration`
3. 保持向后兼容性不做强制要求，但需要引导用户使用新 API
4. 简化单元测试，不依赖配置文件即可进行测试

## 方案

### 修改内容

1. **移除现有方法**：
   - 移除无参方法 `start()` - 该方法从默认配置文件加载配置
   - 移除 `start(Config config)` 方法 - 该方法接受 Typesafe Config 对象

2. **新增方法**：
   - 新增 `start(AgentConfiguration agentConfiguration)` 方法 - 直接接受预构建的 `AgentConfiguration` 对象

### 代码变化

**修改前：**
```java
public class AgentStarter {
    public void start() {
        // 从默认路径加载 Typesafe Config
        // 解析为 AgentConfiguration
        // 启动 Agent
    }

    public void start(Config config) {
        // 从传入的 Typesafe Config 解析为 AgentConfiguration
        // 启动 Agent
    }
}
```

**修改后：**
```java
public class AgentStarter {
    public void start(AgentConfiguration agentConfiguration) {
        // 直接使用传入的 AgentConfiguration 启动 Agent
    }
}
```

### 依赖变化

- 修改前：`AgentStarter` → `AgentConfiguration` → Typesafe Config
- 修改后：`AgentStarter` 不直接依赖 Typesafe Config，依赖由调用方处理

### 对调用方的影响

如果调用方原来使用配置文件方式，需要做调整：

**原来的用法：**
```java
new AgentStarter().start();
// 或者
Config config = ConfigFactory.load();
new AgentStarter().start(config);
```

**新的用法：**
```java
// 调用方负责从配置文件读取并构建 AgentConfiguration
Config config = ConfigFactory.load();
AgentConfiguration agentConfig = AgentConfigurationParser.parse(config);
new AgentStarter().start(agentConfig);
```

这种方式职责更清晰：
- 调用方负责配置的来源和解析
- `AgentStarter` 只负责启动逻辑

## 优势

1. **解耦**：`AgentStarter` 不再依赖 Typesafe Config，减少了依赖耦合
2. **灵活**：调用方可以通过任何方式构建 `AgentConfiguration`，不限于配置文件
3. **可测试**：单元测试可以方便地直接传入 mock 或者测试用的配置对象，不需要配置文件
4. **清晰**：职责分离更清晰，启动逻辑和配置解析分离

## 劣势

1. **breaking change**：原有代码需要修改才能编译通过，需要调用方调整
2. **多了一行代码**：调用方需要自己处理配置解析步骤

这些劣势在当前项目阶段是可以接受的，因为项目还在活跃开发中，没有大量的外部使用。

## 实施步骤

1. 修改 `AgentStarter` 类，移除旧方法，添加新方法
2. 更新所有使用 `AgentStarter` 的代码，适配新 API
3. 更新单元测试，使用新的 API 进行测试
4. 验证编译通过，所有测试通过

## 参考

- 相关模块：`dag-task-agent`
- 相关类：`AgentStarter`, `AgentConfiguration`
