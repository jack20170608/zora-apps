# dag-scheduler-adapter-web-muserver

调用 `dag-scheduler-domain` 中 `port.in` 下定义的 UseCase，把 HTTP 请求转换为 Command/Result 对象。

## 职责
- 路由注册与 MuServer Handler 实现（`adapter.web.muserver` 包）
- HTTP DTO ↔ Command/Result 映射
- `DomainException` → HTTP 状态码映射（`ExceptionMapper`）
- JWT / Cookie 鉴权适配（沿用 zora-muserver 提供的能力）

## 状态
当前为空骨架，控制器将在步骤 3 从旧 `dag-scheduler-muserver` 模块迁入。
