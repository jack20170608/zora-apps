# 04-SSO-安全机制与最佳实践

## 概述

SSO 系统的安全性至关重要，任何安全漏洞都可能导致用户账号被盗或数据泄露。本章将深入介绍 OAuth 2.0 和 OIDC 实施中的安全威胁、防御措施以及最佳实践。

## 4.1 OAuth 2.0 安全威胁概述

### 主要威胁类型

```
┌─────────────────────────────────────────────────────────────────┐
│                    OAuth 2.0 安全威胁                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  授权相关   │  │  Token 相关 │  │  会话相关   │            │
│  │             │  │             │  │             │            │
│  │• 授权码截获 │  │• Token 泄露 │  │• CSRF 攻击  │            │
│  │• 重放攻击   │  │• Token 伪造 │  │• 会话劫持   │            │
│  │• CSRF 攻击  │  │• Token 泄漏 │  │• 会话固定   │            │
│  │• 开放重定向 │  │• 算法混淆   │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  客户端相关 │  │  服务器相关 │  │  协议相关   │            │
│  │             │  │             │  │             │            │
│  │• 客户端伪装 │  │• MITM 攻击  │  │• 密钥混淆   │            │
│  │• 密钥泄露   │  │• SSRF 攻击  │  │• 令牌滥用   │            │
│  │• 回调 URL  │  │             │  │             │            │
│  │   伪造     │  │             │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 4.2 授权码截获攻击与 PKCE

### 攻击场景

```
┌─────────────────────────────────────────────────────────────────┐
│                    授权码截获攻击                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  攻击者                                                        │
│     │                                                          │
│     │  1. 诱骗用户访问恶意应用                                  │
│     │     (发送钓鱼邮件、聊天消息等)                            │
│     │                                                          │
│     ▼                                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  恶意应用发起授权请求                                       │   │
│  │  redirect_uri 指向攻击者控制的服务器                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│     │                                                          │
│     │  2. 用户在授权服务器登录并授权                           │
│     │                                                          │
│     ▼                                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  授权服务器携带授权码回调到恶意服务器                       │   │
│  │  https://attacker.com/callback?code=xxx                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│     │                                                          │
│     │  3. 攻击者使用授权码兑换 Access Token                   │
│     │                                                          │
│     ▼                                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  攻击者获取了用户授权，可以访问用户资源                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### PKCE 防御原理

```
┌─────────────────────────────────────────────────────────────────┐
│                    PKCE 防御原理                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  正常流程（无 PKCE）：                                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  授权码：xxx  ──────────────────────▶  攻击者            │   │
│  │  攻击者直接用授权码兑换 Token → 成功！                    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  PKCE 流程：                                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 客户端生成 code_verifier (只有客户端知道)            │   │
│  │  2. 计算 code_challenge = SHA256(code_verifier)         │   │
│  │  3. 授权请求携带 code_challenge                          │   │
│  │  4. 授权服务器存储 code_challenge                        │   │
│  │  5. 回调时攻击者只有授权码，没有 code_verifier           │   │
│  │  6. 攻击者尝试兑换 Token → 失败！（缺少 code_verifier）  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  关键点：                                                       │
│  • code_verifier 只有客户端知道                                │
│  • 授权服务器只存储 code_challenge，不存储 code_verifier      │
│  • 兑换 Token 时必须提供 code_verifier                         │
│  • 攻击者即使截获授权码，也无法兑换 Token                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### PKCE 实现

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE 工具类
 * 符合 RFC 7636 规范
 */
public class PKCEUtil {
    
    private static final String CODE_VERIFIER_CHARS = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    
    /**
     * 生成 code_verifier
     * 要求：43-128 个字符，只能包含 [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"
     */
    public static String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(128);
        
        // 生成 32-64 个随机字符
        int length = 32 + random.nextInt(33); // 32-64
        for (int i = 0; i < length; i++) {
            sb.append(CODE_VERIFIER_CHARS.charAt(
                random.nextInt(CODE_VERIFIER_CHARS.length())
            ));
        }
        return sb.toString();
    }
    
    /**
     * 使用 S256 方法生成 code_challenge
     * code_challenge = BASE64URL(SHA256(code_verifier))
     */
    public static String generateCodeChallengeS256(String codeVerifier) 
            throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
    
    /**
     * 验证 code_verifier
     * 授权服务器在兑换 Token 时验证
     */
    public static boolean verifyCodeVerifier(String codeVerifier, 
                                               String codeChallenge, 
                                               String method) throws Exception {
        if ("S256".equals(method)) {
            String expectedChallenge = generateCodeChallengeS256(codeVerifier);
            return expectedChallenge.equals(codeChallenge);
        } else if ("plain".equals(method)) {
            // plain 方法不安全，不推荐使用
            return codeVerifier.equals(codeChallenge);
        }
        return false;
    }
}
```

## 4.3 CSRF 攻击与 state 参数

### 攻击场景

```
┌─────────────────────────────────────────────────────────────────┐
│                      CSRF 攻击场景                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  攻击者想做的事：                                                │
│  让已登录的用户在不知情的情况下，绑定攻击者的账号到受害者的应用  │
│                                                                 │
│  攻击步骤：                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 攻击者在应用 A 注册账号 A1                           │   │
│  │  2. 攻击者发起"绑定第三方账号"请求                       │   │
│  │  3. 应用跳转到授权服务器，等待用户授权                    │   │
│  │  4. 攻击者构造恶意链接，诱骗已登录用户 B 点击             │   │
│  │  5. 用户在授权服务器授权                                  │   │
│  │  6. 授权服务器携带 state 回调到应用                      │   │
│  │  7. 应用验证 state 成功，将用户账号绑定到攻击者账号       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  关键点：                                                       │
│  • 攻击者知道回调 URL 和 state 参数                            │   │
│  • 受害者已经登录应用                                          │   │
│  • 受害者不知情地完成了授权                                    │   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### state 参数防御

```java
import java.security.SecureRandom;
import java.util.Base64;

/**
 * State 参数生成和验证
 */
public class StateUtil {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * 生成 state 参数
     * 长度：128 位（16 字节）随机数
     */
    public static String generateState() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * 验证 state 参数
     * 必须在服务器端存储并验证
     */
    public static class StateValidator {
        private final Map<String, StateStore> stateStore = new ConcurrentHashMap<>();
        
        /**
         * 存储 state
         */
        public void storeState(String state, String originalUrl) {
            StateStore store = new StateStore();
            store.setState(state);
            store.setOriginalUrl(originalUrl);
            store.setCreatedAt(Instant.now());
            store.setUsed(false);
            stateStore.put(state, store);
        }
        
        /**
         * 验证并消费 state（一次性使用）
         */
        public String validateAndConsume(String state) {
            StateStore store = stateStore.get(state);
            if (store == null) {
                throw new SecurityException("Invalid state");
            }
            
            // 检查是否已使用（防止重放）
            if (store.isUsed()) {
                throw new SecurityException("State already used");
            }
            
            // 检查是否过期（建议 10 分钟）
            if (store.isExpired()) {
                throw new SecurityException("State expired");
            }
            
            // 标记为已使用
            store.setUsed(true);
            return store.getOriginalUrl();
        }
    }
}
```

## 4.4 Token 重放攻击与 nonce

### nonce 的作用

```
┌─────────────────────────────────────────────────────────────────┐
│                      nonce 防御原理                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  问题：                                                         │
│  ID Token 可能被截获并重放                                      │
│                                                                 │
│  攻击场景：                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 用户正常登录应用                                      │   │
│  │  2. 攻击者截获了用户的 ID Token                          │   │
│  │  3. 攻击者使用截获的 ID Token 冒充用户登录               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  防御方法：                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 登录请求时生成 nonce 参数                            │   │
│  │  2. 将 nonce 发送给授权服务器                            │   │
│  │  3. 授权服务器将 nonce 放入 ID Token                    │   │
│  │  4. 应用验证 ID Token 中的 nonce 与请求中的 nonce 匹配  │   │
│  │  5. 如果不匹配，拒绝登录                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  关键点：                                                       │
│  • nonce 必须是客户端生成的随机字符串                          │   │
│  • 必须在验证 ID Token 时检查 nonce                           │   │
│  • nonce 可以与 state 相同，也可以独立                        │   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### nonce 验证实现

```java
/**
 * nonce 验证
 */
public class NonceValidator {
    
    /**
     * 验证 ID Token 中的 nonce
     */
    public static void validateNonce(String idToken, String expectedNonce) {
        if (expectedNonce == null || expectedNonce.isEmpty()) {
            // 如果请求中没有 nonce，则跳过验证
            // 但这可能是不安全的
            return;
        }
        
        // 解析 ID Token
        JWT jwt = parseJWT(idToken);
        String tokenNonce = jwt.getClaim("nonce");
        
        // 验证 nonce
        if (tokenNonce == null) {
            throw new SecurityException("ID Token missing nonce claim");
        }
        
        if (!expectedNonce.equals(tokenNonce)) {
            throw new SecurityException("Nonce mismatch");
        }
    }
}
```

## 4.5 开放重定向攻击

### 攻击场景

```
┌─────────────────────────────────────────────────────────────────┐
│                    开放重定向攻击                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  攻击场景：                                                     │
│  应用的回调 URL 没有严格验证，攻击者可以构造恶意回调 URL        │
│                                                                 │
│  攻击步骤：                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 攻击者构造恶意回调 URL：                              │   │
│  │     https://app.com/callback?                           │   │
│  │        code=xxx&                                         │   │
│  │        redirect_uri=https://attacker.com/steal          │   │
│  │  2. 攻击者诱骗用户点击恶意链接                            │   │
│  │  3. 用户完成授权后，应用将授权码发送到攻击者服务器        │   │
│  │  4. 攻击者使用授权码兑换 Token                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  防御方法：                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 严格验证 redirect_uri，必须精确匹配预注册的值        │   │
│  │  2. 禁止使用动态回调 URL                                 │   │
│  │  3. 禁止回调 URL 包含片段（#）                          │   │
│  │  4. 禁止使用开放重定向（允许任意外部 URL）               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### redirect_uri 验证实现

```java
/**
 * redirect_uri 验证
 */
public class RedirectUriValidator {
    
    /**
     * 验证 redirect_uri
     * 必须精确匹配预注册的回调地址
     */
    public static void validateRedirectUri(String requestedUri, 
                                            String registeredUri) {
        if (requestedUri == null || registeredUri == null) {
            throw new SecurityException("Redirect URI is null");
        }
        
        // 1. 精确匹配
        if (requestedUri.equals(registeredUri)) {
            return;
        }
        
        // 2. 不允许其他任何匹配方式
        // 禁止：前缀匹配、模糊匹配、通配符
        throw new SecurityException("Redirect URI mismatch");
    }
    
    /**
     * 禁止的 redirect_uri 模式
     */
    public static boolean isAllowedRedirectUri(String uri) {
        // 禁止使用
        if (uri == null) return false;
        
        // 禁止包含片段
        if (uri.contains("#")) {
            return false;
        }
        
        // 禁止使用 http://（生产环境）
        if (uri.startsWith("http://") && !isLocalhost(uri)) {
            return false;
        }
        
        // 禁止开放重定向
        if (uri.contains("?") && uri.contains("redirect=")) {
            return false;
        }
        
        return true;
    }
    
    private static boolean isLocalhost(String uri) {
        return uri.startsWith("http://localhost") || 
               uri.startsWith("http://127.0.0.1");
    }
}
```

## 4.6 JWT 算法混淆攻击

### 攻击场景

```
┌─────────────────────────────────────────────────────────────────┐
│                    算法混淆攻击                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  攻击原理：                                                     │
│  某些 JWT 库允许 Token 指定算法，如果使用 "alg": "none"        │
│  或将算法改为 "HS256" 而使用对称密钥验证，攻击者可以伪造 Token  │
│                                                                 │
│  攻击方式 1：alg=none                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 攻击者构造 JWT，alg 设置为 "none"                   │   │
│  │  2. 移除签名部分（尾部留空）                             │   │
│  │  3. 发送 Token，应用可能接受这个"未签名"的 Token        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  攻击方式 2：密钥混淆                                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 将 alg 从 RS256 改为 HS256                          │   │
│  │  2. 使用已知的公钥作为 HMAC 密钥                         │   │
│  │  3. 使用 HMAC 签名（公钥作为密钥）                       │   │
│  │  4. 验证时使用公钥作为密钥，签名验证通过                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 防御措施

```java
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;

/**
 * JWT 验证安全配置
 */
public class JWTSecurityConfig {
    
    /**
     * 允许的签名算法
     * 只允许非对称算法
     */
    private static final Set<String> ALLOWED_ALGORITHMS = Set.of(
        "RS256", "RS384", "RS512",
        "ES256", "ES384", "ES512",
        "PS256", "PS384", "PS512",
        "Ed25519", "Ed448"
    );
    
    /**
     * 验证签名算法
     */
    public static void validateAlgorithm(JWSAlgorithm algorithm) {
        if (!ALLOWED_ALGORITHMS.contains(algorithm.getName())) {
            throw new SecurityException(
                "Unsupported algorithm: " + algorithm.getName()
            );
        }
    }
    
    /**
     * 安全的 JWT 解析和验证
     */
    public static JWTClaimsSet validateToken(String token, RSAKey publicKey) {
        try {
            // 1. 解析 JWT
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // 2. 获取签名算法
            JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();
            
            // 3. 验证算法（禁止不安全算法）
            validateAlgorithm(algorithm);
            
            // 4. 验证签名
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new SecurityException("Invalid signature");
            }
            
            // 5. 获取 Claims 并验证
            return signedJWT.getJWTClaimsSet();
            
        } catch (Exception e) {
            throw new SecurityException("Token validation failed", e);
        }
    }
}
```

## 4.7 会话安全

### Session Fixation 攻击

```
┌─────────────────────────────────────────────────────────────────┐
│                    Session Fixation 攻击                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  攻击原理：                                                     │
│  攻击者预先设定一个 Session ID，受害者使用这个 ID 登录后，      │
│  攻击者可以劫持会话                                            │
│                                                                 │
│  攻击步骤：                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  1. 攻击者访问应用，获取一个 Session ID: ATTACK123       │   │
│  │  2. 攻击者构造恶意链接，诱骗受害者使用该 ID 登录         │   │
│  │  3. 受害者在攻击者的 Session ID 下登录                  │   │
│  │  4. 攻击者使用同一 Session ID 访问应用                  │   │
│  │  5. 应用认为攻击者是合法用户                             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  防御方法：                                                     │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  登录成功后，必须创建新的 Session ID                     │   │
│  │  invalidate 旧 Session，创建新 Session                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 防御实现

```java
/**
 * Session 安全工具类
 */
public class SessionSecurityUtil {
    
    /**
     * 登录后更换 Session ID
     * 防止 Session Fixation 攻击
     */
    public static void regenerateSession(HttpSession oldSession) {
        // 1. 保存旧 Session 中的数据
        Map<String, Object> sessionData = new HashMap<>();
        Enumeration<String> attributes = oldSession.getAttributeNames();
        while (attributes.hasMoreElements()) {
            String name = attributes.nextElement();
            sessionData.put(name, oldSession.getAttribute(name));
        }
        
        // 2. Invalidate 旧 Session
        oldSession.invalidate();
        
        // 3. 创建新 Session
        HttpSession newSession = oldSession.getSession(true);
        
        // 4. 恢复数据到新 Session
        sessionData.forEach(newSession::setAttribute);
        
        // 5. 生成新的 Session ID（由容器自动生成）
    }
    
    /**
     * 创建安全的 Session Cookie
     */
    public static Cookie createSecureSessionCookie(String sessionId) {
        Cookie cookie = new Cookie("SESSION_ID", sessionId);
        
        // 安全配置
        cookie.setSecure(true);        // 仅 HTTPS
        cookie.setHttpOnly(true);      // 禁止 JS 访问
        cookie.setSameSite("Lax");     // 防止 CSRF
        cookie.setPath("/");           // 整个站点
        cookie.setMaxAge(3600);        // 1小时
        
        return cookie;
    }
}
```

## 4.8 Token 安全存储与传输

### Token 存储最佳实践

```
┌─────────────────────────────────────────────────────────────────┐
│                    Token 存储最佳实践                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Access Token 存储：                                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  客户端（浏览器）：                                        │   │
│  │  ✗ 避免存储在 localStorage（易受 XSS 攻击）              │   │
│  │  ✓ 内存中存储（仅运行时使用）                             │   │
│  │  ✓ HttpOnly Cookie（需要配合后端）                       │   │
│  │                                                             │   │
│  │  服务端：                                                   │   │
│  │  ✓ 加密存储                                                │   │
│  │  ✓ 限制访问权限                                           │   │
│  │  ✗ 不记录到日志                                            │   │
│  │  ✗ 不返回给前端（除非必要）                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Refresh Token 存储：                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  ✓ 服务端存储（数据库）                                   │   │
│  │  ✓ 加密存储                                               │   │
│  │  ✓ 绑定到用户和客户端                                     │   │
│  │  ✓ 限制有效期和作用域                                    │   │
│  │  ✗ 不存储在客户端（除非安全存储）                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Token 传输安全

```java
/**
 * Token 传输安全配置
 */
public class TokenTransportSecurity {
    
    /**
     * 验证 Token 传输是否安全
     */
    public static void validateTokenTransport(HttpServletRequest request, 
                                                String token) {
        // 1. 验证是否使用 HTTPS
        if (!request.isSecure()) {
            throw new SecurityException("Token must be transmitted over HTTPS");
        }
        
        // 2. 验证 Authorization 头格式
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Invalid Authorization header format");
        }
        
        // 3. 不允许 Token 在 URL 中传输
        if (request.getQueryString() != null && 
            request.getQueryString().contains("token")) {
            throw new SecurityException("Token must not be transmitted in URL");
        }
    }
}
```

## 4.9 安全检查清单

### 授权请求检查

```
┌─────────────────────────────────────────────────────────────────┐
│                    授权请求安全检查清单                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  □ 必须使用 HTTPS                                               │
│  □ 必须使用 Authorization Code + PKCE                          │
│  □ PKCE code_challenge_method 必须是 S256                      │
│  □ 必须生成并验证 state 参数                                    │
│  □ 建议生成并验证 nonce 参数                                    │
│  □ redirect_uri 必须精确匹配预注册值                            │
│  □ redirect_uri 禁止使用通配符                                  │
│  □ redirect_uri 禁止包含片段                                    │
│  □ scope 必须明确，不能过度授权                                 │
│  □ response_type 必须是 code                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Token 验证检查

```
┌─────────────────────────────────────────────────────────────────┐
│                    Token 验证安全检查清单                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  □ 验证签名算法，禁止 alg=none                                  │
│  □ 验证签名算法，只允许非对称算法                               │
│  □ 验证 iss（签发者）精确匹配                                   │
│  □ 验证 aud（受众）包含当前客户端                               │
│  □ 验证 exp（过期时间）未过期                                   │
│  □ 验证 iat（签发时间）合理                                     │
│  □ 验证 nonce（如果请求中提供）                                 │
│  □ 验证 kid（密钥 ID）存在于 JWKS 中                           │
│  □ Authorization Code 必须一次性使用                           │
│  □ Authorization Code 必须有有效期限制                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 会话管理检查

```
┌─────────────────────────────────────────────────────────────────┐
│                    会话管理安全检查清单                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  □ 登录后必须更换 Session ID                                    │
│  □ Session Cookie 必须设置 HttpOnly                            │
│  □ Session Cookie 必须设置 Secure                              │
│  □ Session Cookie 建议设置 SameSite=Lax                        │
│  □ 必须设置 Session 过期时间                                   │
│  □ 必须支持绝对过期和空闲过期                                   │
│  □ 必须支持主动登出                                             │
│  □ 必须支持管理员强制终止会话                                   │
│  □ 登出时必须清除服务器端会话数据                               │
│  □ 登出时建议调用授权服务器登出端点                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 4.10 本章小结

### 核心要点

```
┌─────────────────────────────────────────────────────────────────┐
│                      本章核心要点                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 授权码截获 vs PKCE                                          │
│     • PKCE 防止授权码被截获后冒用                               │
│     • code_verifier 只有客户端知道                             │
│     • code_challenge 是其 SHA256 哈希值                        │
│                                                                 │
│  2. CSRF vs state 参数                                          │
│     • state 防止 CSRF 攻击                                     │
│     • 必须是随机生成的字符串                                   │
│     • 一次性使用，需要服务器端存储                             │
│                                                                 │
│  3. Token 重放 vs nonce                                         │
│     • nonce 防止 ID Token 重放                                 │
│     • 登录请求中生成，验证时比对                                │
│                                                                 │
│  4. 开放重定向                                                  │
│     • redirect_uri 必须精确匹配预注册值                         │
│     • 禁止通配符、片段、开放重定向                              │
│                                                                 │
│  5. 算法混淆攻击                                                │
│     • 禁止使用 alg=none                                         │
│     • 只允许非对称签名算法                                      │
│     • 验证算法必须使用白名单                                    │
│                                                                 │
│  6. 会话安全                                                    │
│     • 登录后必须更换 Session ID                                │
│     • Session Cookie 必须安全配置                              │
│     • 登出时必须清除会话数据                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 课后思考

1. 为什么 PKCE 的 code_verifier 需要客户端生成，而不是授权服务器？
2. state 和 nonce 有什么区别？是否可以合并？
3. 为什么 ID Token 需要验证 nonce，而 Access Token 不需要？
4. Session Fixation 攻击和 Session Hijacking 攻击有什么区别？

## 4.11 实践任务

### 任务 1：实现完整的 PKCE 流程

```java
/**
 * 完整的 PKCE 流程实现
 */
public class PKCEFlowDemo {
    
    // 1. 生成 PKCE 参数
    public static PKCEParams generatePKCE() {
        String codeVerifier = PKCEUtil.generateCodeVerifier();
        String codeChallenge;
        try {
            codeChallenge = PKCEUtil.generateCodeChallengeS256(codeVerifier);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
        
        return new PKCEParams(codeVerifier, codeChallenge, "S256");
    }
    
    // 2. 验证 PKCE 参数
    public static boolean verifyPKCE(String codeVerifier, 
                                      String codeChallenge, 
                                      String method) {
        try {
            return PKCEUtil.verifyCodeVerifier(codeVerifier, codeChallenge, method);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 任务 2：实现 state 管理

```java
/**
 * 登录事务管理
 */
public class LoginTransactionManager {
    
    private final Map<String, LoginTransaction> transactions = new ConcurrentHashMap<>();
    private final Duration EXPIRY = Duration.ofMinutes(10);
    
    /**
     * 创建登录事务
     */
    public LoginTransaction createTransaction(String state, 
                                                String nonce, 
                                                String codeChallenge,
                                                String codeChallengeMethod,
                                                String redirectUri,
                                                String originalUrl) {
        LoginTransaction tx = new LoginTransaction();
        tx.setState(state);
        tx.setNonce(nonce);
        tx.setCodeChallenge(codeChallenge);
        tx.setCodeChallengeMethod(codeChallengeMethod);
        tx.setRedirectUri(redirectUri);
        tx.setOriginalUrl(originalUrl);
        tx.setCreatedAt(Instant.now());
        tx.setUsed(false);
        
        transactions.put(state, tx);
        return tx;
    }
    
    /**
     * 验证并消费登录事务
     */
    public LoginTransaction validateAndConsume(String state, 
                                                 String issuer,
                                                 String clientId) {
        LoginTransaction tx = transactions.get(state);
        
        if (tx == null) {
            throw new SecurityException("Invalid state");
        }
        
        // 检查是否过期
        if (tx.getCreatedAt().plus(EXPIRY).isBefore(Instant.now())) {
            transactions.remove(state);
            throw new SecurityException("Transaction expired");
        }
        
        // 检查是否已使用
        if (tx.isUsed()) {
            throw new SecurityException("Transaction already used");
        }
        
        // 标记为已使用
        tx.setUsed(true);
        
        return tx;
    }
}
```

### 任务 3：实现 ID Token 验证

```java
/**
 * 完整的 ID Token 验证
 */
public class IDTokenValidator {
    
    public static JWTClaimsSet validate(String idToken,
                                         String expectedIssuer,
                                         String expectedAudience,
                                         String expectedNonce,
                                         RSAKey publicKey) throws Exception {
        
        // 1. 解析 JWT
        SignedJWT signedJWT = SignedJWT.parse(idToken);
        
        // 2. 验证签名算法
        JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();
        if (!isAllowedAlgorithm(algorithm)) {
            throw new SecurityException("Unsupported algorithm: " + algorithm);
        }
        
        // 3. 验证签名
        RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Invalid signature");
        }
        
        // 4. 获取 Claims
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        
        // 5. 验证 iss
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new SecurityException("Invalid issuer");
        }
        
        // 6. 验证 aud
        if (!claims.getAudience().contains(expectedAudience)) {
            throw new SecurityException("Invalid audience");
        }
        
        // 7. 验证 exp
        if (claims.getExpirationTime().before(new Date())) {
            throw new SecurityException("Token expired");
        }
        
        // 8. 验证 iat（签发时间合理）
        Date iat = claims.getIssueTime();
        Date now = new Date();
        if (iat.after(now)) {
            throw new SecurityException("Invalid iat");
        }
        // 允许 5 分钟内的时钟偏差
        if (iat.before(new Date(now.getTime() - 5 * 60 * 1000))) {
            throw new SecurityException("Token too old");
        }
        
        // 9. 验证 nonce
        if (expectedNonce != null) {
            String tokenNonce = claims.getClaim("nonce", String.class);
            if (!expectedNonce.equals(tokenNonce)) {
                throw new SecurityException("Invalid nonce");
            }
        }
        
        return claims;
    }
    
    private static boolean isAllowedAlgorithm(JWSAlgorithm algorithm) {
        return algorithm.getName().matches("^RS(256|384|512)$|^ES(256|384|512)$|^PS(256|384|512)$");
    }
}
```

## 下章预告

下一章我们将学习 **Keycloak 部署与配置**，深入了解这个开源身份和访问管理解决方案的安装、配置和管理。我们将详细讲解：
- Keycloak 部署方式（Docker、Standalone）
- Realm、Client、User、Role 概念
- Client 配置详解
- Token 配置
- MFA 配置
- Admin API 使用