# OAuth 2.0 授权

## 1. OAuth 解决什么问题

OAuth 2.0 让客户端在不获得用户密码的前提下，取得访问资源服务器的受限权限。它回答的是“客户端能否代表资源所有者访问某资源”，不是“当前用户是谁”。

核心角色：

| 角色 | 职责 |
|---|---|
| Resource Owner | 授予访问权限，常见情况下是用户 |
| Client | 请求权限的应用 |
| Authorization Server | 认证用户、获取授权并签发 Token |
| Resource Server | 接收 Access Token 并保护 API（Application Programming Interface，应用程序编程接口） |

OAuth 本身不是登录协议。只有请求 `openid` Scope 并按 OpenID Connect 规则验证 ID Token，才能把流程用于身份认证。仅取得 Access Token，不能证明用户已经登录当前应用。

## 2. Authorization Code + PKCE

现代浏览器、原生应用和服务端 Web 应用的共同主线是授权码模式，并使用 PKCE：

```mermaid
sequenceDiagram
    participant U as 用户浏览器
    participant C as Client
    participant AS as Authorization Server
    U->>C: 发起登录或授权
    C->>C: 生成 state、code_verifier
    C-->>U: 跳转，携带 code_challenge
    U->>AS: 用户认证与授权
    AS-->>U: 返回 code + state
    U->>C: 回调
    C->>C: 校验 state
    C->>AS: code + code_verifier
    AS-->>C: Access Token 等
```

- `code`：短期、一次性，不能直接访问 API。
- `state`：把发起请求与回调关联起来，防止授权 CSRF（Cross-Site Request Forgery，跨站请求伪造）和响应注入。
- `code_verifier`：仅客户端保存。
- `code_challenge`：由 `code_verifier` 派生，授权请求时发送；攻击者只截获 `code` 也无法兑换。

### 2.1 PKCE 到底是什么

PKCE 的全称是 **Proof Key for Code Exchange**，读作“pixy”。它让发起授权的 Client 在兑换 Authorization Code 时证明：“我仍持有这次请求开始前生成的随机秘密。”

| 名称 | 含义 |
|---|---|
| `code_verifier` | Client 用密码学安全随机源生成的高熵字符串，只保存在本地授权事务中 |
| `code_challenge` | 从 `code_verifier` 计算出的公开摘要，随授权请求发送 |
| `S256` | PKCE 转换方式，表示使用 SHA-256 计算摘要 |

使用 `S256` 时，计算关系是：

```text
code_challenge = BASE64URL(SHA256(ASCII(code_verifier)))
```

- **SHA-256**：Secure Hash Algorithm 256-bit，把输入转换成固定长度摘要，不能从摘要直接还原原文。
- **Base64URL**：适合放进 URL 的 Base64 编码变体，使用 `-` 和 `_`，PKCE 中省略末尾的 `=` 填充。

按照 RFC 7636，`code_verifier` 长度为 43～128 个字符，只使用英文字母、数字、`-`、`.`、`_` 和 `~`。不要使用时间戳、递增编号或普通伪随机数生成器。

PKCE 分两段工作：

1. 发起授权时，Client 保存 `code_verifier`，只把 `code_challenge` 和 `S256` 发给 Authorization Server。
2. 兑换 Token 时，Client 提交原始 `code_verifier`；服务器重新计算摘要，并与先前的 `code_challenge` 比较。

即使攻击者截获 Authorization Code，没有 `code_verifier` 也无法兑换 Token。PKCE 不能替代 `state`、精确 Redirect URI 或 Confidential Client 的身份认证，它们防御的是不同风险。

## 3. 客户端类型

- **Confidential Client**：能在后端安全保存凭据，例如传统服务端 Web 应用。
- **Public Client**：无法可靠保存凭据，例如 SPA（Single-Page Application，单页应用）、桌面和移动应用。

不要把发布到浏览器或安装包中的 `client_secret` 当作秘密。Public Client 必须依赖 PKCE、严格回调地址等控制。

## 4. Scope 与最小权限

Scope 表达客户端请求的权限边界，例如 `orders.read`。授权服务器签发的权限不应超过：

1. 客户端被允许申请的范围；
2. 用户或策略允许授予的范围；
3. 当前资源服务器认可的范围。

Scope 不是业务权限模型的全部。资源服务器仍需按租户、资源归属和操作执行授权。

## 5. 工程案例：从 Google Drive 读取文件列表

这是一个常见的真实工程需求：Alice 在 Zora 文档中心选择“连接 Google Drive”，授权应用读取她的文件元数据。应用不获取 Alice 的 Google 密码，也不能修改或删除她的文件。

本案例只讨论 OAuth 2.0 委托授权，不把 Google 账号当作 Zora 登录方式。Zora 文档中心仍使用自己的登录会话；Google Access Token 只用于访问 Google Drive API。

### 5.1 参与者和工程配置

| OAuth 角色 | 本案例中的组件 |
|---|---|
| Resource Owner | Alice |
| Client | Zora 文档中心服务端 Web 应用 |
| Authorization Server | Google OAuth 2.0 Authorization Server |
| Resource Server | Google Drive API |

文档中心是 Confidential Client，需要先在 Google Cloud Console 创建 OAuth Client，并精确注册回调地址。以下 Client ID 仅为示例：

```text
authorization_endpoint: https://accounts.google.com/o/oauth2/v2/auth
token_endpoint: https://oauth2.googleapis.com/token
client_id: 123456789-example.apps.googleusercontent.com
client_authentication_method: client_secret_post
client_secret: 从 Secret Manager 注入，不写入仓库
redirect_uri: https://docs.zora.example/oauth/google/callback
scope: https://www.googleapis.com/auth/drive.metadata.readonly
```

`drive.metadata.readonly` 允许读取文件名、类型等元数据，但不能读取文件内容。若产品只需要展示文件列表，就不应申请权限更大的 `drive.readonly` 或完整 Drive Scope。

### 5.2 完整交互

```mermaid
sequenceDiagram
    autonumber
    actor U as Alice
    participant B as 浏览器
    participant C as Zora 文档中心
    participant G as Google 授权服务器
    participant D as Google Drive API

    U->>B: 点击“连接 Google Drive”
    B->>C: GET /integrations/google/connect
    C->>C: 生成 state 和 PKCE verifier
    C-->>B: 302 跳转到 Google
    B->>G: Authorization Request
    G->>U: 登录并展示授权范围
    U->>G: 同意读取文件元数据
    G-->>B: 302 回调 code + state
    B->>C: GET /oauth/google/callback
    C->>C: 校验并单次消费 state
    C->>G: code + code_verifier 兑换 Token
    G-->>C: Access Token
    C->>D: Bearer Access Token
    D->>D: 校验 Token、Scope 与文件权限
    D-->>C: Alice 可访问的文件元数据
    C-->>B: 返回文件选择页面
```

#### 第一步：客户端发起授权请求

Alice 点击连接按钮后，文档中心生成高熵随机的 `state` 和 `code_verifier`，保存到短期、一次性的服务端授权事务中，再计算：

```text
code_challenge = BASE64URL(SHA256(code_verifier))
```

浏览器收到 302，并跳转到 Google。为便于阅读，下面将 URL 分行展示；实际请求是一个完整 URL：

```http
GET /o/oauth2/v2/auth
    ?response_type=code
    &client_id=123456789-example.apps.googleusercontent.com
    &redirect_uri=https%3A%2F%2Fdocs.zora.example%2Foauth%2Fgoogle%2Fcallback
    &scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.metadata.readonly
    &state=mV9Z2Qp7xK4n8R3cT6w1
    &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
    &code_challenge_method=S256 HTTP/1.1
Host: accounts.google.com
```

请求中没有 `openid` Scope，因为这里不是用 Google 完成登录，而是申请访问 Drive API 的权限。这也说明 OAuth Access Token 不等于用户登录凭证。

#### 第二步：用户同意后返回授权码

Google 向 Alice 展示应用名称和申请的权限。Alice 同意后，Google 不会把 Access Token 放进浏览器 URL，而是携带短期、一次性的授权码返回精确注册的回调地址：

```http
HTTP/1.1 302 Found
Location: https://docs.zora.example/oauth/google/callback?code=4%2F0AbUR2VExample&state=mV9Z2Qp7xK4n8R3cT6w1
```

文档中心必须先恒定时间比较 `state`，再原子地消费授权事务。缺失、不匹配、过期或已经使用过的 `state` 都应终止流程，不能继续兑换 Token。

如果 Alice 拒绝授权，Google 会回调 `error=access_denied&state=...`。文档中心仍须校验并消费 `state`，但不能调用 Token Endpoint，也不应把拒绝展示成系统故障。

#### 第三步：后端兑换 Access Token

文档中心后端直接调用 Google Token Endpoint。浏览器不参与这个请求，也不接触 Client Secret 或 `code_verifier`：

```http
POST /token HTTP/1.1
Host: oauth2.googleapis.com
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&
code=4%2F0AbUR2VExample&
client_id=123456789-example.apps.googleusercontent.com&
client_secret=<client-secret>&
redirect_uri=https%3A%2F%2Fdocs.zora.example%2Foauth%2Fgoogle%2Fcallback&
code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

Google 会校验 Code、客户端身份、原始 Redirect URI 和 PKCE。任意一项不匹配都会拒绝兑换。示例中的凭据和 Token 均为占位值，真实值不得写入文档、URL 或日志。

简化后的成功响应如下：

```json
{
  "access_token": "<access-token>",
  "token_type": "Bearer",
  "expires_in": 3599,
  "scope": "https://www.googleapis.com/auth/drive.metadata.readonly"
}
```

授权服务器签发的实际 Scope 可能少于请求值，客户端必须以响应和 Token 中的最终授权范围为准，不能假定请求过的权限一定获批。

#### 第四步：使用 Access Token 调用 Drive API

文档中心把 Access Token 放在 Authorization Header 中，而不是查询参数中。它应把 Access Token 当作不透明凭据，不依赖其内部格式：

```http
GET /drive/v3/files?pageSize=10&fields=files(id,name,mimeType) HTTP/1.1
Host: www.googleapis.com
Authorization: Bearer <access-token>
Accept: application/json
```

Google Drive API 验证 Token、Scope 和 Alice 对每个文件的访问权限。文档中心不自行解析 Token，也不能把它发送给其他 API。Token 过期或无效时，API 会拒绝请求；Scope 不足时，应用应提示用户重新授权所需范围。

成功响应只包含 Alice 当前有权访问、且该 Scope 允许读取的文件元数据。即使 Token 有效，Google Drive 的共享关系和文件权限仍可能阻止访问某个文件。

### 5.3 每个安全参数究竟保护什么

| 参数或检查 | 防御目标 | 缺失时的典型风险 |
|---|---|---|
| `state` | 绑定浏览器发起的授权事务与回调 | 授权 CSRF、回调串线 |
| PKCE | 证明兑换方持有原始 `code_verifier` | 授权码被截获后兑换 |
| 精确 Redirect URI | 把 Code 只交给已注册回调 | Code 被送到攻击者控制的地址 |
| Client Authentication | 证明兑换方是 Confidential Client | 客户端被冒充 |
| `drive.metadata.readonly` | 只允许读取 Drive 文件元数据 | 应用获得读取内容或修改文件的过大权限 |
| Drive 文件权限 | 限制 Alice 实际可以访问的文件 | 仅凭 Scope 绕过资源所有权和共享规则 |
| Token 仅发送给 Drive API | 限制凭据的使用位置 | Token 泄漏或被错误发送给其他服务 |

这条链路说明了 OAuth 的边界：Google 授予 Zora 文档中心“代表 Alice 读取 Drive 文件元数据”的有限能力，Drive API 仍保留对具体文件的最终授权决定权。

如果改为访问 Zora 自己的 API，资源服务器还需要执行 [Token 与校验](05-Token与校验.md) 中的完整验证，并在 Scope 之外继续检查租户和资源归属。

## 6. 不再推荐的模式

- **Implicit Grant**：Token 经浏览器前通道返回，现代实践应改用 Authorization Code + PKCE。
- **Resource Owner Password Credentials**：客户端直接收集用户密码，破坏身份提供方边界，不应使用。
- **仅靠 Client Credentials 表示用户**：该模式表示客户端自身，不代表终端用户。

## 7. 最常见的误区

- 用 Access Token 证明“用户已登录”；
- 回调地址使用通配符或前缀匹配；
- 认为有 PKCE 就可以省略 `state`；
- 在 URL、日志或浏览器存储中长期保存 Token；
- API 只解析 JWT（JSON Web Token），不验证签名和 Claims。

## 8. 术语与缩写附录

正文在术语首次出现时给出解释，本表用于集中查阅。

### 8.1 常见缩写

| 术语 | 英文全称 | 含义 |
|---|---|---|
| OAuth 2.0 | OAuth 2.0 Authorization Framework | 委托授权框架；OAuth 是协议名称，不必按字母拆解 |
| PKCE | Proof Key for Code Exchange | 授权码交换证明，防止截获的授权码被他人兑换 |
| API | Application Programming Interface | 应用程序编程接口，例如 Google Drive API |
| CSRF | Cross-Site Request Forgery | 跨站请求伪造；攻击者诱导浏览器完成非用户本意的请求 |
| SPA | Single-Page Application | 单页应用，代码主要运行在浏览器中，不能安全保存 Client Secret |
| OIDC | OpenID Connect | 建立在 OAuth 2.0 之上的身份认证协议，回答“用户是谁” |
| JWT | JSON Web Token | 一种可签名的 Token 表达格式；Access Token 不一定是 JWT |
| HTTP / HTTPS | Hypertext Transfer Protocol / HTTP Secure | 浏览器和服务端使用的通信协议；生产 OAuth 回调应使用 HTTPS |
| URI / URL | Uniform Resource Identifier / Uniform Resource Locator | URI 是资源标识；URL 是包含访问位置的 URI，Redirect URI 通常表现为 URL |

### 8.2 流程术语

| 术语 | 解释 |
|---|---|
| Authorization Code | 授权码；短期、一次性的中间凭据，只能用于兑换 Token |
| Access Token | 访问令牌；Client 调用 Resource Server API 时提交的凭据 |
| Bearer Token | 持有者令牌；任何拿到它的人都可能使用，因此必须防止泄漏 |
| Scope | 权限范围；描述 Client 申请的操作类别，例如只读文件元数据 |
| Client ID | Client 的公开标识，用于说明“哪个应用正在申请授权” |
| Client Secret | Confidential Client 的机密凭据，只能保存在可信后端 |
| Redirect URI | 授权完成后的回调地址，必须与预注册值精确匹配 |
| Authorization Endpoint | 浏览器前往的授权端点，负责用户交互并返回 Authorization Code |
| Token Endpoint | Client 后端兑换 Token 的端点，不应由浏览器直接携带 Secret 调用 |
| `state` | Client 生成的随机关联值，用于绑定发起请求与回调，防御授权 CSRF 和响应注入 |
| Claim | Token 中关于签发者、接收方、有效期或主体的一项声明 |

## 9. 检查点

能展开 PKCE 的英文全称，说明 `code_verifier`、`code_challenge` 和 `S256` 的关系，并准确解释 `state`、PKCE、Scope、Redirect URI 和资源级授权各自解决的问题。

[下一章：OpenID Connect 身份认证](03-OpenID-Connect-身份认证.md)
