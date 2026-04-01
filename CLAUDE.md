# CLAUDE.md

提供给CLAUDE的开发指引。

## General Guidelines
- 这个项目下面会包括好几个子项目，每个子项目都应该有自己的README.md文件，详细说明该子项目的功能、使用方法和开发指南。
- 子项目之间没有依赖关系
- 采用的JDK版本都是jdk25, 采用的构建工具都是maven，采用的测试框架都是junit5,mockito, 采用的日志框架都是slf4j
- 每次主要的功能或者架构调整，都在docs目录下写一份文档，文档名称请按照序号`01,02,03`加上前缀`ARCHITECTURE`或者`FEATURE`来命名，方便后续查阅。
- 代码提交时，请在commit message中注明本次提交的主要内容和目的，例如：`feat: add user authentication module` 或者 `refactor: optimize database connection handling
- 代码中请保持良好的注释习惯，尤其是在复杂的逻辑或者算法部分，确保其他开发者能够理解代码的意图和实现细节。
- 代码注释，包括但不限于：类注释、方法注释、重要逻辑的行内注释等，应该清晰、简洁，并且与代码保持同步更新，而且语言采用英文
- 项目开发生成的文档采用中文
- 初始化子模块的时候，同步创建`main/resource`和`test/resource`目录，方便后续存放配置文件和测试资源。
- 这里的项目都基于zora这个开发框架，开发过程中请参考zora的相关文档和示例代码，确保遵循zora的最佳实践和设计原则。
- 为每一个子模块都生成`metadata/metadata.json`文件，包含模块的基本信息、功能描述等内容，并把metadata放到resource里面，enable filter，方便后续的维护和管理,metadata文件的格式可以参考以下示例.
```json
{
  "groupId": "@project.groupId@",
  "artifactId": "@project.artifactId@",
  "description": "@project.description@",
  "version": "@project.version@",
  "scmUrl": "@project.scmUrl@"
}
```

- 为每一个子模块都生成README.md文件，详细说明该子模块的功能、使用方法和开发指南，确保其他开发者能够快速上手和理解该模块的作用和实现细节。

## dag-task
这个子项目，主要负责实现一个基于DAG（有向无环图）的任务调度系统。该系统将支持任务的定义、依赖关系管理、调度执行和结果跟踪等功能。里面包括4个子模块，
- dag-task-si 这个是用来实现DAG调度系统的领域模型
- dag-task-core 这个是用来实现DAG调度系统的核心模块，包含任务定义、依赖关系管理、调度算法和执行引擎等功能。
- dag-task-server这个是基于MuServer的任何调度中心，提供Restful的接口用来对任务进行管理
- dag-task-agent这个是基于MuServer的任何调度执行器，负责执行调度中心分配的任务，并将执行结果反馈给调度中心。

这个项目的坐标是`top.ilovemyhome.dagtask`，版本号是`1.0.0`，后续的版本号会根据功能的增加和修复进行更新。


## host-helper
这个子项目，主要用来实现一个主机管理工具，提供主机的注册、查询、监控和维护等功能。该工具将支持多种主机类型和协议，方便用户对主机进行统一管理和操作。里面包括2个子模块，
- host-helper-si 这个是用来实现主机管理工具的领域模型
- host-helper-core 核心实现类
- host-helper-server 基于MuServer的主机管理中心，提供Restful的接口用来对主机进行管理

这个项目的坐标是`top.ilovemyhome.hosthelper`，版本号是`1.0.0`，后续的版本号会根据功能的增加和修复进行更新。

## pom的模板
项目的parent pom.xml中已经定义了一些常用的依赖版本，开发过程中请尽量使用这些版本，避免引入不兼容的依赖版本导致构建失败或者运行时错误。如果需要引入新的依赖，请先检查是否与现有的依赖版本兼容，并且在引入之前进行充分的测试和验证。

parent pom.xml的模板
```xml
<!-- 依赖zora-bom的1.0.1版本-->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.ilovemyhome.zora</groupId>
            <artifactId>zora-bom</artifactId>
            <version>1.0.1</version>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<build>
<plugins>
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>flatten-maven-plugin</artifactId>
        <version>1.6.0</version>
        <configuration>
            <updatePomFile>true</updatePomFile>
            <flattenMode>resolveCiFriendliesOnly</flattenMode>
        </configuration>
        <executions>
            <execution>
                <id>flatten</id>
                <phase>process-resources</phase>
                <goals>
                    <goal>flatten</goal>
                </goals>
            </execution>
            <execution>
                <id>flatten-clean</id>
                <phase>clean</phase>
                <goals>
                    <goal>clean</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.14.0</version>
            <configuration>
                <source>25</source>
                <target>25</target>
                <release>25</release>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.4</version>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.7.1</version>
        </plugin>
    </plugins>
</pluginManagement>
</build>
```

子模块的pom.xml模板如下
```xml
        <!-- SLF4J -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
```

