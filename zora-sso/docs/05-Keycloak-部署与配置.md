# 05-Keycloak-部署与配置

## 概述

Keycloak 是一个开源的身份和访问管理（IAM）解决方案，提供了完整的 OIDC 和 SAML 支持。本章将详细介绍 Keycloak 的部署、配置和基本管理。

## 5.1 Keycloak 简介

### 什么是 Keycloak？

```
┌─────────────────────────────────────────────────────────────────┐
│                       Keycloak 简介                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Keycloak 是 Red Hunt 开发维护的开源身份和访问管理平台           │
│                                                                 │
│  核心特性：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  ✓ 单点登录 (SSO)                                        │   │
│  │  ✓ OpenID Connect / SAML 支持                           │   │
│  │  ✓ 社交登录（Google、GitHub 等）                         │   │
│  │  ✓ 身份代理（LDAP、Active Directory）                   │   │
│  │  ✓ 多因素认证（MFA）                                    │   │
│  │  ✓ 用户管理、角色管理                                    │   │
│  │  ✓ 客户端管理                                            │   │
│  │  ✓ 细粒度授权                                            │   │
│  │  ✓ 主题定制（登录页面）                                  │   │
│  │  ✓ Admin REST API                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  适用场景：                                                      │
│  • 企业应用单点登录                                              │
│  • 微服务身份认证                                                │
│  • API 访问控制                                                  │
│  • 移动应用认证                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Keycloak 架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      Keycloak 架构                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                      用户浏览器                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Keycloak Server                       │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  Login/Admin UI        │  OIDC/SAML Endpoints  │    │   │
│  │  ├─────────────────────────────────────────────────┤    │   │
│  │  │  User Storage          │  Identity Brokering    │    │   │
│  │  ├─────────────────────────────────────────────────┤    │   │
│  │  │  User Federation       │  Token Management      │    │   │
│  │  ├─────────────────────────────────────────────────┤    │   │
│  │  │  Authentication        │  Authorization         │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    数据库（PostgreSQL/MySQL）            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 5.2 Keycloak 部署

### 5.2.1 Docker 部署（推荐）

```bash
# 1. 拉取 Keycloak 镜像
docker pull quay.io/keycloak/keycloak:24.0

# 2. 启动 Keycloak（开发环境）
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0 start-dev

# 3. 生产环境启动（使用外置数据库）
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=keycloak \
  -e KC_HOSTNAME=auth.example.com \
  -e KC_HTTPS_CERTIFICATE_FILE=/etc/x509/https/tls.crt \
  -e KC_HTTPS_CERTIFICATE_KEY_FILE=/etc/x509/https/tls.key \
  quay.io/keycloak/keycloak:24.0 start --optimized
```

### 5.2.2 Docker Compose 部署

```yaml
# docker-compose.yml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    container_name: keycloak
    ports:
      - "8080:8080"
    environment:
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin
      - KC_DB=dev-file
    command: start-dev
    volumes:
      - ./themes:/opt/keycloak/themes
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health/ready"]
      interval: 10s
      timeout: 5s
      retries: 5
```

```bash
# 启动
docker-compose up -d

# 访问管理界面
# http://localhost:8080
# 用户名: admin
# 密码: admin
```

### 5.2.3 外置数据库配置

```yaml
# docker-compose.yml - 使用 PostgreSQL
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: keycloak-db
    environment:
      - POSTGRES_DB=keycloak
      - POSTGRES_USER=keycloak
      - POSTGRES_PASSWORD=keycloak
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U keycloak"]
      interval: 10s
      timeout: 5s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    container_name: keycloak
    ports:
      - "8080:8080"
    environment:
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak
      - KC_DB_USERNAME=keycloak
      - KC_DB_PASSWORD=keycloak
    command: start-dev
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

## 5.3 Keycloak 核心概念

### 5.3.1 Realm（领域）

Realm 是 Keycloak 中的核心概念，用于隔离不同的安全领域。

```
┌─────────────────────────────────────────────────────────────────┐
│                        Realm 概念                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Realm = 独立的身份管理空间                                      │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Master Realm                                            │   │
│  │  • Keycloak 管理员 realm                                 │   │
│  │  • 管理其他 realm                                        │   │
│  │  • 创建管理员用户                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  myrealm (业务 Realm)                                    │   │
│  │  • 独立的用户存储                                        │   │
│  │  • 独立的客户端配置                                      │   │
│  │  • 独立的角色定义                                        │   │
│  │  • 独立的认证流程                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  customer-portal (客户门户 Realm)                        │   │
│  │  • 面向客户的应用程序                                    │   │
│  │  • 独立的用户目录                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  使用建议：                                                      │
│  • 每个环境（dev/staging/prod）使用独立 realm                  │
│  • 每个业务线使用独立 realm                                     │
│  • 避免生产环境和开发环境共享 realm                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3.2 Client（客户端）

Client 是需要向 Keycloak 认证的应用程序。

```
┌─────────────────────────────────────────────────────────────────┐
│                      Client 配置                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  核心配置项：                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Client ID: 客户端唯一标识                               │   │
│  │  Name: 客户端显示名称                                     │   │
│  │  Description: 描述                                       │   │
│  │  Enabled: 是否启用                                        │   │
│  │  Client Protocol: openid-connect                         │   │
│  │  Access Type: 客户端类型                                 │   │
│  │  Valid Redirect URIs: 有效的回调 URL                    │   │
│  │  Web Origins: CORS 允许的源                              │   │
│  │  Client Secret: 客户端密钥（confidential 类型）         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Access Type 类型：                                             │
│  ┌─────────────┬────────────────────────────────────────┐     │
│  │ 类型         │ 说明                                   │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ confidential│ 有后端服务的 Web 应用，需要 client     │     │
│  │             │ secret 或私钥验证                       │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ public      │ 纯前端 SPA 或移动应用，不能存储        │     │
│  │             │ client secret，使用 PKCE               │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ bearer-only │ 仅用于 API 认证，不参与浏览器登录       │     │
│  └─────────────┴────────────────────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3.3 User（用户）

用户是被 Keycloak 管理的身份主体。

```
┌─────────────────────────────────────────────────────────────────┐
│                      User 管理                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用户属性：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Username: 用户名（唯一）                                │   │
│  │  Email: 邮箱                                             │   │
│  │  First Name / Last Name: 姓名                           │   │
│  │  Enabled: 是否启用                                        │   │
│  │  Email Verified: 邮箱是否验证                            │   │
│  │  Groups: 用户组                                          │   │
│  │  Roles: 角色                                             │   │
│  │  Credentials: 凭据（密码）                               │   │
│  │  Attributes: 自定义属性                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  用户状态：                                                      │
│  ┌─────────────┬────────────────────────────────────────┐     │
│  │ 状态         │ 说明                                   │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ enabled     │ 用户是否可登录                          │     │
│  │ disabled    │ 用户被禁用                              │     │
│  │ temp        │ 临时锁定（密码错误过多）                │     │
│  │ expired     │ 密码或账号过期                          │     │
│  └─────────────┴────────────────────────────────────────┘     │
│                                                                 │
│  密码策略：                                                      │
│  • 最小长度                                                      │
│  • 复杂度要求（大写、小写、数字、特殊字符）                    │
│  • 密码历史（不能重复使用）                                     │
│  • 过期时间                                                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3.4 Role（角色）

角色定义用户的权限集合。

```
┌─────────────────────────────────────────────────────────────────┐
│                      Role 管理                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  角色类型：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Realm Roles: 整个 realm 级别的角色                     │   │
│  │  • admin: 管理员                                         │   │
│  │  • offline_access: 离线访问                             │   │
│  │  • uma_authorization: UM A 授权                         │   │
│  │                                                           │   │
│  │  Client Roles: 特定客户端的角色                          │   │
│  │  • myapp-admin: 应用管理员                              │   │
│  │  • myapp-user: 普通用户                                  │   │
│  │  • myapp-guest: 访客                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  角色继承：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  superuser                                               │   │
│  │  ├── admin                                               │   │
│  │  │   ├── user-admin                                      │   │
│  │  │   └── role-admin                                      │   │
│  │  └── manager                                             │   │
│  │      ├── user-viewer                                     │   │
│  │      └── user-editor                                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3.5 Client Scope

Client Scope 定义了 Token 中包含的声明和角色。

```
┌─────────────────────────────────────────────────────────────────┐
│                    Client Scope                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  内置 Scope：                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  profile: 基本信息（name, given_name, family_name 等） │   │
│  │  email: 邮箱信息（email, email_verified）               │   │
│  │  address: 地址信息                                       │   │
│  │  phone: 电话信息                                         │   │
│  │  offline_access: 离线访问（refresh_token）              │   │
│  │  roles: 角色信息                                         │   │
│  │  microprofile-jwt: MP-JWT 声明                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Token 声明映射：                                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Client Scope 配置 → Token 中包含的 Claim              │   │
│  │                                                           │   │
│  │  profile scope:                                          │   │
│  │  ├── name                                                │   │
│  │  ├── given_name                                          │   │
│  │  ├── family_name                                         │   │
│  │  ├── nickname                                            │   │
│  │  ├── picture                                             │   │
│  │  ├── gender                                              │   │
│  │  ├── locale                                              │   │
│  │  └── zoneinfo                                            │   │
│  │                                                           │   │
│  │  roles scope:                                            │   │
│  │  └── realm_access.roles                                  │   │
│  │  └── client_roles.{client-id}                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 5.4 Client 配置详解

### 5.4.1 创建客户端

```bash
# 使用 Admin API 创建客户端
curl -X POST "http://localhost:8080/admin/realms/myrealm/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "my-web-app",
    "name": "My Web Application",
    "enabled": true,
    "clientProtocol": "openid-connect",
    "publicClient": false,
    "standardFlowEnabled": true,
    "implicitFlowEnabled": false,
    "directAccessGrantsEnabled": false,
    "serviceAccountsEnabled": true,
    "authorizationServicesEnabled": false,
    "redirectUris": [
      "https://myapp.com/callback",
      "http://localhost:8080/callback"
    ],
    "webOrigins": [
      "https://myapp.com"
    ],
    "attributes": {
      "client.secret.creation.time": "1699999999",
      "access.token.lifespan": "300",
      "refresh.token.lifespan": "1800"
    }
  }'
```

### 5.4.2 客户端配置界面

```
┌─────────────────────────────────────────────────────────────────┐
│                    Client 配置界面                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  General Settings:                                              │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Client ID: [my-web-app____________]                     │   │
│  │  Name: [My Web Application_________]                    │   │
│  │  Description: [Web application for___]                 │   │
│  │  Enabled: [✓]                                           │   │
│  │  Always Display in Console: [ ]                         │   │
│  │  Client Protocol: [openid-connect ▼]                   │   │
│  │  Access Type: [confidential ▼]                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Login Settings:                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Valid Redirect URIs:                                    │   │
│  │  https://myapp.com/callback                             │   │
│  │  http://localhost:8080/callback                         │   │
│  │                                                           │   │
│  │  Web Origins:                                            │   │
│  │  https://myapp.com                                      │   │
│  │                                                           │   │
│  │  Admin URL: [https://myapp.com/admin________]          │   │
│  │  Web Hooks: [________________________________]         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Capability Config:                                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  ✓ Standard Flow (Authorization Code)                  │   │
│  │  ☐ Implicit Flow                                         │   │
│  │  ☐ Direct Access Grants                                 │   │
│  │  ✓ Service Accounts Enabled                             │   │
│  │  ☐ OAuth 2.0 Device Authorization Grant                 │   │
│  │  ☐ OAuth 2.0 Device Code Grant                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.4.3 Token 配置

```
┌─────────────────────────────────────────────────────────────────┐
│                    Token 配置                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Token Settings:                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Access Token Lifespan: [5 minutes]                     │   │
│  │  Access Token Lifespan For Implicit Flow: [3 minutes]  │   │
│  │  Client Session Idle Timeout: [30 minutes]             │   │
│  │  Client Session Max Lifespan: [1 hours]                │   │
│  │  Client Offline Session Idle Timeout: [1 days]         │   │
│  │  Client Offline Session Max Lifespan: [3 days]         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Session Config:                                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  SSO Session Idle Timeout: [30 minutes]                │   │
│  │  SSO Session Max Lifespan: [10 hours]                  │   │
│  │  Offline Session Idle Timeout: [3 days]               │   │
│  │  Offline Session Max Lifespan: [30 days]               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Refresh Token:                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  ✓ Refresh Token                                        │   │
│  │  ✓ Reuse Refresh Token                                  │   │
│  │  ○ Rotate Refresh Token                                 │   │
│  │  Maximum Refresh Token Lifespan: [1 days]              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 5.5 用户和角色管理

### 5.5.1 创建用户

```bash
# 使用 Admin API 创建用户
curl -X POST "http://localhost:8080/admin/realms/myrealm/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "enabled": true,
    "emailVerified": true,
    "credentials": [
      {
        "type": "password",
        "value": "SecurePassword123!",
        "temporary": false
      }
    ]
  }'
```

### 5.5.2 分配角色

```bash
# 为用户分配 realm 角色
curl -X POST "http://localhost:8080/admin/realms/myrealm/users/{user-id}/role-mappings/realm" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "role-id", "name": "offline_access"},
    {"id": "role-id", "name": "uma_authorization"}
  ]'

# 为用户分配客户端角色
curl -X POST "http://localhost:8080/admin/realms/myrealm/users/{user-id}/role-mappings/clients/{client-id}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"id": "role-id", "name": "myapp-admin"},
    {"id": "role-id", "name": "myapp-user"}
  ]'
```

### 5.5.3 用户组管理

```
┌─────────────────────────────────────────────────────────────────┐
│                    用户组管理                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  组结构示例：                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  /                                                        │   │
│  │  ├── IT Department                                       │   │
│  │  │   ├── Developers                                      │   │
│  │  │   │   ├── Alice                                       │   │
│  │  │   │   └── Bob                                         │   │
│  │  │   └── QA Team                                         │   │
│  │  │       └── Charlie                                     │   │
│  │  │                                                      │   │
│  │  └── Sales Department                                    │   │
│  │      ├── Managers                                        │   │
│  │      └── Representatives                                  │   │
│  │          └── David                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  组配置：                                                        │
│  • 组可以继承父组角色                                           │
│  • 组可以设置默认角色                                           │
│  • 组可以关联属性                                               │
│  • 组可以配置认证流程                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 5.6 MFA 配置

### 5.6.1 支持的 MFA 类型

```
┌─────────────────────────────────────────────────────────────────┐
│                    MFA 类型                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┬────────────────────────────────────────┐     │
│  │ 类型         │ 说明                                   │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ TOTP         │ 时间基于一次性密码                     │     │
│  │              │ Google Authenticator、Authy 等        │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ WebAuthn/FIDO2│ 硬件安全密钥                          │     │
│  │              │ YubiKey、FaceID、指纹等               │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ OTP 邮件     │ 邮件发送验证码                         │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ OTP 短信     │ 短信发送验证码                         │     │
│  ├─────────────┼────────────────────────────────────────┤     │
│  │ 条件 MFA     │ 根据规则决定是否启用 MFA              │     │
│  └─────────────┴────────────────────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.6.2 配置 MFA

```bash
# 配置 Required Action 启用 TOTP
curl -X POST "http://localhost:8080/admin/realms/myrealm/authentication/required-actions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "providerId": "CONFIGURE_TOTP",
    "name": "Configure OTP",
    "enabled": true,
    "defaultAction": false,
    "priority": 10
  }'
```

### 5.6.3 条件 MFA 配置

```
┌─────────────────────────────────────────────────────────────────┐
│                    条件 MFA 配置                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  认证流程：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. Username Password                                    │   │
│  │  2. 条件判断（满足任一条件则执行 MFA）                   │   │
│  │     ├── 角色 = admin                                     │   │
│  │     ├── IP 范围 = 外部网络                               │   │
│  │     └── 时间 = 非工作时间                                │   │
│  │  3. Conditional OTP                                      │   │
│  │  4. 成功                                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  配置界面：                                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Execute conditions:                                    │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  Condition: User role                           │    │   │
│  │  │  Role: admin                                    │    │   │
│  │  │  [ ] Not                                        │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                         │   │
│  │  Condition: User attribute                              │   │
│  │  Attribute: temporary                                   │   │
│  │  Value: true                                            │   │
│  │                                                         │   │
│  │  Alias: main                                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 5.7 Admin API 使用

### 5.7.1 获取 Token

```bash
# 获取 Admin API 访问令牌
curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=admin-cli" \
  -d "client_secret=your-client-secret"
```

### 5.7.2 常用 API 示例

```bash
# 获取所有用户
curl -X GET "http://localhost:8080/admin/realms/myrealm/users" \
  -H "Authorization: Bearer $TOKEN"

# 获取用户详情
curl -X GET "http://localhost:8080/admin/realms/myrealm/users/{user-id}" \
  -H "Authorization: Bearer $TOKEN"

# 更新用户
curl -X PUT "http://localhost:8080/admin/realms/myrealm/users/{user-id}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Jane", "lastName": "Doe"}'

# 删除用户
curl -X DELETE "http://localhost:8080/admin/realms/myrealm/users/{user-id}" \
  -H "Authorization: Bearer $TOKEN"

# 获取所有客户端
curl -X GET "http://localhost:8080/admin/realms/myrealm/clients" \
  -H "Authorization: Bearer $TOKEN"

# 获取客户端角色
curl -X GET "http://localhost:8080/admin/realms/myrealm/clients/{client-id}/roles" \
  -H "Authorization: Bearer $TOKEN"

# 创建客户端
curl -X POST "http://localhost:8080/admin/realms/myrealm/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientId": "new-client", "enabled": true, "publicClient": true}'
```

## 5.8 本章小结

### 核心要点

```
┌─────────────────────────────────────────────────────────────────┐
│                      本章核心要点                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Keycloak 简介                                              │
│     • 开源身份和访问管理解决方案                                │
│     • 支持 OIDC、SAML                                          │
│     • 提供 SSO、MFA、用户管理等功能                            │
│                                                                 │
│  2. 部署方式                                                    │
│     • Docker 部署（推荐）                                      │
│     • Docker Compose 部署                                      │
│     • 支持 PostgreSQL、MySQL 等数据库                          │
│                                                                 │
│  3. 核心概念                                                    │
│     • Realm：独立的身份管理空间                                │
│     • Client：需要认证的应用                                    │
│     • User：被管理的身份主体                                    │
│     • Role：权限集合                                           │
│     • Client Scope：Token 声明范围                             │
│                                                                 │
│  4. Client 配置                                                │
│     • Access Type：confidential / public / bearer-only        │
│     • Valid Redirect URIs：回调地址白名单                      │
│     • Token 过期时间配置                                        │
│                                                                 │
│  5. MFA 配置                                                   │
│     • TOTP、WebAuthn、邮件/短信 OTP                            │
│     • 支持条件 MFA                                             │
│                                                                 │
│  6. Admin API                                                  │
│     • 获取访问令牌                                             │
│     • 用户管理、客户端管理、角色管理                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 课后思考

1. Realm 和 Client 有什么区别？
2. 为什么 public 客户端不能使用 client_secret？
3. Client Scope 和 Role 有什么区别？
4. 条件 MFA 的典型使用场景是什么？

## 5.9 实践任务

### 任务 1：部署 Keycloak

```bash
# 使用 Docker 部署 Keycloak
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0 start-dev
```

### 任务 2：创建 Realm 和 Client

```bash
# 1. 获取 admin token
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=admin-cli" \
  -d "client_secret=your-secret" | jq -r '.access_token')

# 2. 创建 Realm
curl -X POST "http://localhost:8080/admin/realms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "myrealm",
    "enabled": true,
    "displayName": "My Realm"
  }'

# 3. 创建 Client
curl -X POST "http://localhost:8080/admin/realms/myrealm/clients" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "my-web-app",
    "enabled": true,
    "publicClient": false,
    "standardFlowEnabled": true,
    "redirectUris": ["http://localhost:8080/callback"]
  }'
```

### 任务 3：创建用户并测试登录

```bash
# 创建用户
curl -X POST "http://localhost:8080/admin/realms/myrealm/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "enabled": true,
    "credentials": [{
      "type": "password",
      "value": "password123",
      "temporary": false
    }]
  }'

# 测试登录
# 访问以下 URL（浏览器中）
http://localhost:8080/realms/myrealm/protocol/openid-connect/auth?
  response_type=code&
  client_id=my-web-app&
  redirect_uri=http://localhost:8080/callback&
  scope=openid%20profile%20email
```

## 下章预告

下一章我们将进入 **实战：zora-sso 实现单点登录**，结合前几章学习的知识，实现一个完整的 SSO 集成层。我们将详细讲解：
- 项目架构设计
- OIDC Discovery 和 JWKS 获取
- 登录流程实现
- Token 验证和会话管理
- 登出流程实现
- 客户端接入示例