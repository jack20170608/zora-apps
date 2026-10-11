# 09-FEATURE 注册客户端查询接口

## 目标

`zora-sso-muserver` 提供注册客户端只读查询接口，供受信任的管理员查看当前配置中的客户端。
接口不得泄露客户端密钥或密钥哈希，也不得允许未认证用户枚举系统接入方。

## 接口

请求：

```http
GET /sso/clients
Cookie: ZORA_SSO_SESSION=<session-id>
```

调用者必须持有有效的 SSO Session，并包含 `ADMIN` 角色。Session 缺失或失效时返回 `401`，
角色不足时返回 `403`。

成功响应使用统一的 `RestResponse<T>`：

```json
{
  "success": true,
  "data": [
    {
      "clientId": "host-helper",
      "redirectUris": [
        "https://host-helper.example/sso/callback"
      ],
      "enabled": true
    }
  ],
  "message": null
}
```

客户端按照 `clientId` 排序，回调地址也采用稳定排序。响应只包含客户端 ID、已注册回调地址
和启用状态，不包含 `clientSecretHash`。

## 安全边界

- Web 层通过 `SsoService.requireIdentity` 验证 Session，不直接信任 Cookie 内容。
- Session 校验沿用已有的空闲超时、绝对超时和滑动续期规则。
- 权限校验默认拒绝，仅 `ADMIN` 角色可以查询。
- 未认证响应不会包含任何客户端元数据。
- 该接口是当前本地认证 POC 的管理能力，不替代生产环境身份提供方的客户端管理接口。
