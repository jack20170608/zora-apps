# dag-scheduler-app

`dag-scheduler` 六边形架构的**组装入口**。手工 DI 把 domain + 两个 adapter 串起来。

## 职责
- `SchedulerContext`：手工 DI 容器（构造函数注入，无反射、无注解）
- `SchedulerBootstrap` / `main()`
- `SchedulerConfig` 加载（基于 zora-config 读取 yaml / env）

## 约束
- 这是**唯一**允许同时依赖 domain + 所有 adapter 的模块。
- 不引入 Spring / Guice / Avaje。命名与构造函数注入风格按"Avaje 友好"组织，将来如需升级 DI 容器，业务代码零修改。

## 状态
当前为空骨架，组装代码将在步骤 4 与 `dag-allinone` 切换同步落地。
