# zora-apps

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

zora-apps 是一个基于 [zora](https://github.com/ilovemyhome/zora) 框架构建的应用程序集合，旨在提供一系列实用、高效、模块化的工具和服务。本项目采用现代化的设计理念，结合最新的 Java 技术，为开发者提供开箱即用的解决方案。

## 设计理念

1. **轻量级**：保持代码简洁和轻量级，避免过度设计和复杂的依赖关系
2. **模块化**：功能模块化设计，允许开发者根据需要选择和使用不同的功能模块
3. **可扩展**：采用开放接口设计，方便进行功能扩展和二次开发
4. **标准化**：遵循统一的项目结构、编码规范和构建配置，降低维护成本

## 项目结构

```
zora-apps/
├── dag-task/                 # 基于DAG的分布式任务调度系统
│   ├── dag-task-si/         # 领域模型接口定义
│   ├── dag-task-core/       # 核心调度算法和执行引擎实现
│   ├── dag-task-server/     # 调度中心服务端
│   └── dag-task-agent/      # 任务执行器客户端
├── host-helper/             # 主机管理工具（规划中）
│   ├── host-helper-si/      # 领域模型接口定义
│   ├── host-helper-core/    # 核心功能实现
│   └── host-helper-server/  # 主机管理服务端
├── docs/                     # 架构和功能设计文档
└── CLAUDE.md                # 开发指引
```

## 现有项目

### [dag-task](./dag-task) - DAG 分布式任务调度系统

一个基于 DAG（有向无环图）的分布式任务调度系统，支持任务依赖关系管理、分布式执行和结果跟踪。

**核心特性：**
- 基于 DAG 的任务依赖描述
- 分布式调度架构（Server + Agent）
- 可视化任务管理和监控
- 失败重试和错误处理
- 弹性扩缩容

**模块说明：**
- `dag-task-si` - DAG 调度系统的领域模型接口定义
- `dag-task-core` - 核心实现，包含 DAG 构建、拓扑排序、调度算法
- `dag-task-server` - 调度中心服务，提供 RESTful API 进行任务管理
- `dag-task-agent` - 任务执行器，负责任务执行并上报结果

详细说明请参考 [dag-task/README.md](./dag-task/README.md)

### host-helper - 主机管理工具

> 开发规划中...

一个主机管理工具，提供主机的注册、查询、监控和维护等功能，支持多种主机类型和协议。

## 技术栈

- **Java 版本**：JDK 25
- **构建工具**：Maven 3.8+
- **依赖管理**：zora-bom 统一版本管理
- **测试框架**：JUnit 5 + Mockito
- **日志框架**：SLF4J + Logback
- **基础框架**：zora 1.0.2+

## 构建

整个项目构建：

```bash
mvn clean install
```

构建单个模块：

```bash
cd dag-task
mvn clean install
```

## 开发指南

### 环境要求

- JDK 25 或更高版本
- Maven 3.8 或更高版本

### 项目约定

- 每个子项目都有独立的 README.md，详细说明该项目的功能和使用方法
- 子项目之间没有依赖关系，可以独立构建和使用
- 所有依赖版本统一由父 POM 的 zora-bom 管理
- 遵循 Maven 标准目录结构
- 每个模块需要包含 `metadata/metadata.json` 文件，存放模块基本信息

详细的开发指引请参考 [CLAUDE.md](./CLAUDE.md)

## 文档

项目相关的架构和功能设计文档存放在 `docs/` 目录：

- `docs/ARCHITECTURE-*.md` - 架构设计文档
- `docs/FEATURE-*.md` - 功能特性文档
- `docs/Test-*.md` - 测试方案和用例文档

## 版本管理

项目版本遵循语义化版本规范：

- **主版本号**：不兼容的 API 修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

当前版本：see [VERSION](./VERSION)

## 贡献

欢迎提交 Issue 和 Pull Request！

## License

MIT License - see [LICENSE](./LICENSE) for details

Copyright © 2026 zora-apps
