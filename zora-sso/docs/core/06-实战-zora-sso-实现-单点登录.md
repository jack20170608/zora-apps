# 06-实战：zora-sso-实现-单点登录

## 概述

本章将结合前几章学习的知识，实现一个完整的 SSO 集成层 `zora-sso`。我们将详细讲解项目架构、核心功能实现和客户端集成。

## 6.1 项目架构设计

### 6.1.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    zora-sso 整体架构                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                      用户浏览器                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   zora-sso-muserver                      │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  HTTP Handler: /login, /callback, /logout      │    │   │
│  │  │  Session Cookie: HttpOnly, Secure, SameSite    │    │   │
│  │  │  Security Headers: CSP, HSTS, X-Frame-Options  │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│          ┌───────────────────┼───────────────────┐            │
│          ▼                   ▼                   ▼            │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐       │
│  │  登录发起   │    │  回调处理   │    │  会话管理   │       │
│  │  Handler   │    │  Handler   │    │  Handler   │       │
│  └─────────────┘    └─────────────┘    └─────────────┘       │
│          │                   │                   │            │
│          └───────────────────┼───────────────────┘            │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                      zora-sso-core                       │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  OIDC Provider 客户端                            │    │   │
│  │  │  Discovery 加载 / JWKS 获取 / Token 验证        │    │   │
│  │  ├─────────────────────────────────────────────────┤    │   │
│  │  │  认证服务: 登录发起、回调处理、登出              │    │   │
│  │  │  会话服务: 创建、查询、销毁                      │    │   │
│  │  │  Token 服务: 验证、刷新、撤销                    │    │   │
│  │  │  客户端服务: 注册、映射、管理                    │    │   │
│  │  ├─────────────────────────────────────────────────┤    │   │
│  │  │  领域模型: UserIdentity, SecurityContext        │    │   │
│  │  │  值对象: TokenClaims, SessionInfo               │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                      zora-sso-si                         │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  领域模型:                                       │    │   │
│  │  │  • UserIdentity: 用户身份                       │    │   │
│  │  │  • AuthenticatedPrincipal: 认证主体             │    │   │
│  │  │  • SecurityContext: 安全上下文                  │    │   │
│  │  │  • ClientRegistration: 客户端注册               │    │   │
│  │  │  • SessionInfo: 会话信息                        │    │   │
│  │  │  • TokenClaims: Token 声明                      │    │   │
│  │  ├─────────────────────────────────────────────────┤    │   │
│  │  │  服务接口:                                       │    │   │
│  │  │  • AuthenticationService                        │    │   │
│  │  │  • AuthorizationService                         │    │   │
│  │  │  • SessionService                               │    │   │
│  │  │  • TokenValidationService                       │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.1.2 模块职责

```
┌─────────────────────────────────────────────────────────────────┐
│                    模块职责划分                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  zora-sso-si (领域模型)                                         │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • 定义稳定的领域模型和值对象                            │   │
│  │  • 定义服务接口，不依赖具体实现                          │   │
│  │  • 不依赖 MuServer、数据库、Keycloak SDK                │   │
│  │  • 核心类型：UserIdentity, SecurityContext, SessionInfo │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  zora-sso-core (核心实现)                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • 实现 OIDC Provider 客户端                            │   │
│  │  • 实现认证、授权、会话服务                              │   │
│  │  • 实现 Token 验证和 JWKS 缓存                          │   │
│  │  • 实现数据库访问层                                     │   │
│  │  • 核心逻辑不依赖 HTTP 请求对象                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  zora-sso-muserver (HTTP 接入层)                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  • MuServer 启动和生命周期管理                          │   │
│  │  • HTTP Handler: 登录、回调、登出、用户信息             │   │
│  │  • 配置加载和校验                                       │   │
│  │  • Cookie、CSRF、CORS 和安全响应头                      │   │
│  │  • 错误处理和监控端点                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 6.2 核心功能实现

### 6.2.1 OIDC Discovery 客户端

```java
package top.ilovemyhome.zorasso.core.oidc;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.ilovemyhome.zorasso.si.oidc.OIDCProviderMetadata;
import top.ilovemyhome.zorasso.si.oidc.OIDCProviderMetadata.Builder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OIDC Provider Discovery 客户端
 * 
 * 负责从 OIDC Provider 获取服务配置信息
 */
public class OIDCDiscoveryClient {
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    
    // 缓存配置
    private volatile OIDCProviderMetadata cachedMetadata;
    private volatile long metadataCacheTime;
    private static final long CACHE_DURATION_MS = 24 * 60 * 60 * 1000; // 24小时
    
    public OIDCDiscoveryClient(Duration timeout) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        this.objectMapper = new ObjectMapper();
        this.timeout = timeout;
    }
    
    /**
     * 获取 OIDC Provider 元数据
     * 支持缓存，24小时内不重复请求
     */
    public OIDCProviderMetadata getMetadata(String issuer) throws Exception {
        // 检查缓存
        if (cachedMetadata != null && 
            System.currentTimeMillis() - metadataCacheTime < CACHE_DURATION_MS) {
            return cachedMetadata;
        }
        
        // 构建 Discovery URL
        String discoveryUrl = issuer.endsWith("/") 
            ? issuer + ".well-known/openid-configuration"
            : issuer + "/.well-known/openid-configuration";
        
        // 发起请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(discoveryUrl))
                .timeout(timeout)
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new OIDCException("Failed to fetch discovery metadata: " 
                + response.statusCode());
        }
        
        // 解析响应
        OIDCProviderMetadata metadata = parseMetadata(response.body(), issuer);
        
        // 更新缓存
        cachedMetadata = metadata;
        metadataCacheTime = System.currentTimeMillis();
        
        return metadata;
    }
    
    private OIDCProviderMetadata parseMetadata(String json, String expectedIssuer) 
            throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        
        Builder builder = OIDCProviderMetadata.builder()
                .issuer(URI.create(map.get("issuer").toString()))
                .authorizationEndpoint(URI.create(map.get("authorization_endpoint").toString()))
                .tokenEndpoint(URI.create(map.get("token_endpoint").toString()));
        
        // 验证 issuer
        String actualIssuer = map.get("issuer").toString();
        if (!actualIssuer.equals(expectedIssuer)) {
            throw new OIDCException("Issuer mismatch: expected " + expectedIssuer 
                + ", got " + actualIssuer);
        }
        
        // 可选字段
        if (map.containsKey("userinfo_endpoint")) {
            builder.userinfoEndpoint(URI.create(map.get("userinfo_endpoint").toString()));
        }
        if (map.containsKey("jwks_uri")) {
            builder.jwksEndpoint(URI.create(map.get("jwks_uri").toString()));
        }
        if (map.containsKey("end_session_endpoint")) {
            builder.endSessionEndpoint(URI.create(map.get("end_session_endpoint").toString()));
        }
        if (map.containsKey("revocation_endpoint")) {
            builder.revocationEndpoint(URI.create(map.get("revocation_endpoint").toString()));
        }
        
        // 解析支持的 scope
        if (map.containsKey("scopes_supported")) {
            @SuppressWarnings("unchecked")
            List<String> scopes = (List<String>) map.get("scopes_supported");
            builder.supportedScopes(scopes);
        }
        
        // 解析支持的 response type
        if (map.containsKey("response_types_supported")) {
            @SuppressWarnings("unchecked")
            List<String> responseTypes = (List<String>) map.get("response_types_supported");
            builder.supportedResponseTypes(responseTypes);
        }
        
        // 解析 token endpoint auth methods
        if (map.containsKey("token_endpoint_auth_methods_supported")) {
            @SuppressWarnings("unchecked")
            List<String> methods = (List<String>) map.get("token_endpoint_auth_methods_supported");
            builder.supportedTokenEndpointAuthMethods(methods);
        }
        
        return builder.build();
    }
    
    /**
     * 强制刷新缓存
     */
    public OIDCProviderMetadata refreshMetadata(String issuer) throws Exception {
        cachedMetadata = null;
        metadataCacheTime = 0;
        return getMetadata(issuer);
    }
}
```

### 6.2.2 JWKS 客户端

```java
package top.ilovemyhome.zorasso.core.oidc;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWKS 客户端
 * 
 * 负责获取和缓存 OIDC Provider 的公钥
 */
public class JWKSCache {
    
    private final HttpClient httpClient;
    private final Duration timeout;
    private final Duration cacheDuration;
    
    // 缓存：kid -> RSAKey
    private final Map<String, CachedKey> keyCache = new ConcurrentHashMap<>();
    
    private static class CachedKey {
        final RSAKey key;
        final long cachedAt;
        
        CachedKey(RSAKey key) {
            this.key = key;
            this.cachedAt = System.currentTimeMillis();
        }
        
        boolean isExpired(Duration duration) {
            return System.currentTimeMillis() - cachedAt > duration.toMillis();
        }
    }
    
    public JWKSCache(Duration timeout, Duration cacheDuration) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        this.timeout = timeout;
        this.cacheDuration = cacheDuration;
    }
    
    /**
     * 获取指定 kid 的公钥
     * 支持缓存和自动刷新
     */
    public RSAKey getKey(String jwksUri, String kid) throws Exception {
        // 检查缓存
        CachedKey cached = keyCache.get(kid);
        if (cached != null && !cached.isExpired(cacheDuration)) {
            return cached.key;
        }
        
        // 刷新 JWKS
        JWKSet jwkSet = fetchJWKS(jwksUri);
        
        // 查找对应 kid 的密钥
        for (var key : jwkSet.getKeys()) {
            if (key.getKeyID().equals(kid)) {
                if (key instanceof RSAKey rsaKey) {
                    CachedKey newCached = new CachedKey(rsaKey);
                    keyCache.put(kid, newCached);
                    return rsaKey;
                }
            }
        }
        
        throw new OIDCException("Key not found: " + kid);
    }
    
    /**
     * 获取所有密钥
     */
    public JWKSet getAllKeys(String jwksUri) throws Exception {
        // 检查缓存
        JWKSet cached = fetchFromCache();
        if (cached != null) {
            return cached;
        }
        
        return fetchJWKS(jwksUri);
    }
    
    private JWKSet fetchJWKS(String jwksUri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUri))
                .timeout(timeout)
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new OIDCException("Failed to fetch JWKS: " + response.statusCode());
        }
        
        return JWKSet.parse(response.body());
    }
    
    private JWKSet fetchFromCache() {
        // 简化实现：返回缓存的第一个 JWKSet
        if (!keyCache.isEmpty()) {
            CachedKey first = keyCache.values().iterator().next();
            if (!first.isExpired(cacheDuration)) {
                try {
                    return JWKSet.of(keyCache.values().stream()
                            .map(c -> c.key)
                            .toList());
                } catch (Exception e) {
                    // 忽略，尝试重新获取
                }
            }
        }
        return null;
    }
    
    /**
     * 强制刷新缓存
     */
    public void invalidate() {
        keyCache.clear();
    }
}
```

### 6.2.3 登录服务

```java
package top.ilovemyhome.zorasso.core.service;

import top.ilovemyhome.zorasso.si.oidc.OIDCProviderMetadata;
import top.ilovemyhome.zorasso.si.security.LoginTransaction;
import top.ilovemyhome.zorasso.si.exception.AuthenticationException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * 登录服务
 * 
 * 负责生成 OIDC 授权请求参数
 */
public class LoginService {
    
    private final OIDCProviderMetadata metadata;
    private final String clientId;
    private final String redirectUri;
    private final String[] requiredScopes;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 创建登录事务
     * 
     * 生成 state、nonce、code_verifier 等安全参数
     */
    public LoginTransaction createLoginTransaction(String originalUrl) {
        // 生成安全随机数
        String state = generateSecureRandom(32);
        String nonce = generateSecureRandom(32);
        
        // 生成 PKCE 参数
        String codeVerifier = generateCodeVerifier();
        String codeChallenge;
        try {
            codeChallenge = generateCodeChallengeS256(codeVerifier);
        } catch (Exception e) {
            throw new AuthenticationException("Failed to generate code challenge", e);
        }
        
        // 创建登录事务
        LoginTransaction tx = new LoginTransaction();
        tx.setState(state);
        tx.setNonce(nonce);
        tx.setCodeVerifier(codeVerifier);
        tx.setCodeChallenge(codeChallenge);
        tx.setCodeChallengeMethod("S256");
        tx.setClientId(clientId);
        tx.setRedirectUri(redirectUri);
        tx.setIssuer(metadata.getIssuer().toString());
        tx.setOriginalUrl(originalUrl);
        tx.setCreatedAt(Instant.now());
        tx.setExpiresAt(Instant.now().plusSeconds(600)); // 10分钟过期
        tx.setUsed(false);
        
        return tx;
    }
    
    /**
     * 构建授权 URL
     */
    public String buildAuthorizationUrl(LoginTransaction tx) {
        StringBuilder url = new StringBuilder();
        url.append(metadata.getAuthorizationEndpoint().toString());
        url.append("?response_type=code");
        url.append("&client_id=").append(urlEncode(clientId));
        url.append("&redirect_uri=").append(urlEncode(redirectUri));
        url.append("&scope=").append(urlEncode(String.join(" ", requiredScopes)));
        url.append("&state=").append(urlEncode(tx.getState()));
        url.append("&nonce=").append(urlEncode(tx.getNonce()));
        url.append("&code_challenge=").append(urlEncode(tx.getCodeChallenge()));
        url.append("&code_challenge_method=").append(tx.getCodeChallengeMethod());
        
        return url.toString();
    }
    
    private String generateSecureRandom(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    private String generateCodeVerifier() {
        // 43-128 字符
        return generateSecureRandom(32);
    }
    
    private String generateCodeChallengeS256(String codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
    
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AuthenticationException("Failed to encode URL", e);
        }
    }
}
```

### 6.2.4 Token 验证服务

```java
package top.ilovemyhome.zorasso.core.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import top.ilovemyhome.zorasso.si.security.*;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

/**
 * Token 验证服务
 * 
 * 负责验证 ID Token 和 Access Token
 */
public class TokenValidationService {
    
    // 允许的签名算法
    private static final Set<String> ALLOWED_ALGORITHMS = Set.of(
        "RS256", "RS384", "RS512", 
        "ES256", "ES384", "ES512",
        "PS256", "PS384", "PS512"
    );
    
    private final JWKSCache jwksCache;
    private final String expectedIssuer;
    private final String expectedAudience;
    
    /**
     * 验证 ID Token
     * 
     * 验证内容：
     * 1. 签名算法
     * 2. 签名
     * 3. iss (签发者)
     * 4. aud (受众)
     * 5. exp (过期时间)
     * 6. iat (签发时间)
     * 7. nonce
     */
    public TokenClaims validateIDToken(String idToken, 
                                         String expectedNonce,
                                         String jwksUri) throws Exception {
        
        // 1. 解析 JWT
        SignedJWT signedJWT = SignedJWT.parse(idToken);
        
        // 2. 验证签名算法
        JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();
        if (!ALLOWED_ALGORITHMS.contains(algorithm.getName())) {
            throw new TokenValidationException("Unsupported algorithm: " + algorithm);
        }
        
        // 3. 获取密钥 ID
        String kid = signedJWT.getHeader().getKeyID();
        if (kid == null || kid.isEmpty()) {
            throw new TokenValidationException("Missing kid in token header");
        }
        
        // 4. 获取公钥并验证签名
        var rsaKey = jwksCache.getKey(jwksUri, kid);
        RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
        
        if (!signedJWT.verify(verifier)) {
            throw new TokenValidationException("Invalid signature");
        }
        
        // 5. 获取 Claims
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        
        // 6. 验证 iss
        String issuer = claims.getIssuer();
        if (issuer == null || !issuer.equals(expectedIssuer)) {
            throw new TokenValidationException("Invalid issuer: " + issuer);
        }
        
        // 7. 验证 aud
        @SuppressWarnings("unchecked")
        List<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(expectedAudience)) {
            throw new TokenValidationException("Invalid audience");
        }
        
        // 8. 验证 exp
        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            throw new TokenValidationException("Token expired");
        }
        
        // 9. 验证 iat（签发时间合理，不超过 5 分钟）
        Date issuedAt = claims.getIssueTime();
        if (issuedAt == null) {
            throw new TokenValidationException("Missing iat claim");
        }
        
        Date now = new Date();
        if (issuedAt.after(now)) {
            throw new TokenValidationException("Invalid iat: future time");
        }
        // 允许 5 分钟的时钟偏差
        if (issuedAt.getTime() < now.getTime() - 5 * 60 * 1000) {
            throw new TokenValidationException("Token too old");
        }
        
        // 10. 验证 nonce
        if (expectedNonce != null) {
            String tokenNonce = claims.getClaim("nonce", String.class);
            if (!expectedNonce.equals(tokenNonce)) {
                throw new TokenValidationException("Invalid nonce");
            }
        }
        
        // 11. 构建 TokenClaims
        return TokenClaims.builder()
                .subject(claims.getSubject())
                .issuer(issuer)
                .audience(audience)
                .issuedAt(issuedAt.toInstant())
                .expiration(expirationTime.toInstant())
                .authenticationTime(claims.getClaim("auth_time", Date.class)?.toInstant())
                .name(claims.getClaim("name", String.class))
                .givenName(claims.getClaim("given_name", String.class))
                .familyName(claims.getClaim("family_name", String.class))
                .email(claims.getClaim("email", String.class))
                .emailVerified(claims.getClaim("email_verified", Boolean.class))
                .preferredUsername(claims.getClaim("preferred_username", String.class))
                .build();
    }
    
    /**
     * 验证 Access Token
     */
    public void validateAccessToken(String accessToken, 
                                     String expectedAudience,
                                     String jwksUri) throws Exception {
        // 类似于 ID Token 验证，但不验证 nonce
        // 验证 aud 是否包含 Resource Server
        // 验证 scope 是否符合预期
    }
}
```

### 6.2.5 会话管理服务

```java
package top.ilovemyhome.zorasso.core.service;

import top.ilovemyhome.zorasso.si.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务
 * 
 * 负责会话的创建、查询、销毁
 */
public class SessionService {
    
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Duration sessionIdleTimeout;
    private final Duration sessionAbsoluteTimeout;
    
    /**
     * 创建会话
     */
    public SessionInfo createSession(String userId, 
                                       String issuer, 
                                       String subject,
                                       TokenClaims claims) {
        // 生成会话 ID
        String sessionId = generateSessionId();
        
        // 创建会话信息
        SessionInfo session = new SessionInfo();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setIssuer(issuer);
        session.setSubject(subject);
        session.setCreatedAt(Instant.now());
        session.setLastAccessedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(sessionAbsoluteTimeout));
        session.setIdleExpiresAt(Instant.now().plus(sessionIdleTimeout));
        session.setIpAddress(claims.getIpAddress());
        session.setUserAgent(claims.getUserAgent());
        session.setAttributes(new HashMap<>());
        
        // 存储会话
        sessions.put(sessionId, session);
        
        return session;
    }
    
    /**
     * 获取会话
     */
    public SessionInfo getSession(String sessionId) {
        SessionInfo session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        
        // 检查过期
        if (session.isExpired()) {
            invalidateSession(sessionId);
            return null;
        }
        
        // 检查空闲过期
        if (session.isIdleExpired()) {
            invalidateSession(sessionId);
            return null;
        }
        
        // 更新最后访问时间
        session.setLastAccessedAt(Instant.now());
        session.setIdleExpiresAt(Instant.now().plus(sessionIdleTimeout));
        
        return session;
    }
    
    /**
     * 使会话无效
     */
    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    /**
     * 按用户查询会话
     */
    public List<SessionInfo> getSessionsByUser(String userId) {
        return sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .filter(s -> !s.isExpired() && !s.isIdleExpired())
                .toList();
    }
    
    /**
     * 使指定用户的所有会话无效
     */
    public void invalidateAllUserSessions(String userId) {
        List<String> sessionIds = sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .map(SessionInfo::getId)
                .toList();
        
        sessionIds.forEach(sessions::remove);
    }
    
    private String generateSessionId() {
        return UUID.randomUUID().toString() + "-" + System.nanoTime();
    }
}
```

## 6.3 HTTP Handler 实现

### 6.3.1 登录 Handler

```java
package top.ilovemyhome.zorasso.muserver.handler;

import top.ilovemyhome.zorasso.core.service.LoginService;
import top.ilovemyhome.zorasso.core.service.LoginTransactionStore;
import top.ilovemyhome.zorasso.si.security.LoginTransaction;

import java.util.Map;

/**
 * 登录 Handler
 * 
 * 处理登录发起请求
 */
public class LoginHandler {
    
    private final LoginService loginService;
    private final LoginTransactionStore transactionStore;
    
    public LoginHandler(LoginService loginService, 
                        LoginTransactionStore transactionStore) {
        this.loginService = loginService;
        this.transactionStore = transactionStore;
    }
    
    /**
     * 处理登录请求
     * 
     * 1. 获取原始请求 URL
     * 2. 创建登录事务
     * 3. 存储事务
     * 4. 重定向到 OIDC Provider
     */
    public void handleLogin(Request request, Response response) {
        // 1. 获取原始请求 URL
        String originalUrl = request.getParameter("redirect");
        if (originalUrl == null || originalUrl.isEmpty()) {
            originalUrl = "/";
        }
        
        // 验证原始 URL（防止开放重定向）
        if (!isAllowedUrl(originalUrl)) {
            throw new SecurityException("Redirect URL not allowed");
        }
        
        // 2. 创建登录事务
        LoginTransaction tx = loginService.createLoginTransaction(originalUrl);
        
        // 3. 存储事务
        transactionStore.store(tx);
        
        // 4. 构建授权 URL 并重定向
        String authUrl = loginService.buildAuthorizationUrl(tx);
        
        response.redirect(authUrl);
    }
    
    /**
     * 验证 URL 是否允许重定向
     */
    private boolean isAllowedUrl(String url) {
        // 只允许相对路径
        if (url.startsWith("/") && !url.startsWith("//")) {
            return true;
        }
        
        // 禁止绝对 URL（防止开放重定向）
        return false;
    }
}
```

### 6.3.2 回调 Handler

```java
package top.ilovemyhome.zorasso.muserver.handler;

import top.ilovemyhome.zorasso.core.service.*;
import top.ilovemyhome.zorasso.si.security.*;

/**
 * OIDC 回调 Handler
 * 
 * 处理授权服务器回调
 */
public class OIDCCallbackHandler {
    
    private final LoginTransactionStore transactionStore;
    private final TokenExchangeService tokenExchangeService;
    private final TokenValidationService tokenValidationService;
    private final SessionService sessionService;
    
    /**
     * 处理回调请求
     * 
     * 1. 验证 state
     * 2. 验证 issuer
     * 3. 获取登录事务
     * 4. 兑换 Token
     * 5. 验证 ID Token
     * 6. 创建会话
     * 7. 重定向到原始 URL
     */
    public void handleCallback(Request request, Response response) {
        // 1. 获取回调参数
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String iss = request.getParameter("iss");
        String error = request.getParameter("error");
        
        // 2. 检查错误
        if (error != null) {
            String errorDescription = request.getParameter("error_description");
            throw new AuthenticationException("OAuth error: " + error 
                + " - " + errorDescription);
        }
        
        // 3. 验证必要参数
        if (code == null || state == null) {
            throw new AuthenticationException("Missing required parameters");
        }
        
        // 4. 获取并验证登录事务
        LoginTransaction tx = transactionStore.consume(state);
        if (tx == null) {
            throw new AuthenticationException("Invalid or expired state");
        }
        
        // 5. 验证 issuer
        if (iss != null && !iss.equals(tx.getIssuer())) {
            throw new AuthenticationException("Issuer mismatch");
        }
        
        // 6. 兑换 Token
        TokenResponse tokenResponse = tokenExchangeService.exchangeCode(
            code, 
            tx.getCodeVerifier(),
            tx.getRedirectUri()
        );
        
        // 7. 验证 ID Token
        TokenClaims claims = tokenValidationService.validateIDToken(
            tokenResponse.getIdToken(),
            tx.getNonce(),
            tokenResponse.getJwksUri()
        );
        
        // 8. 创建会话
        SessionInfo session = sessionService.createSession(
            claims.getSubject(),
            claims.getIssuer(),
            claims.getSubject(),
            claims
        );
        
        // 9. 设置 Session Cookie
        response.setCookie(createSessionCookie(session.getId()));
        
        // 10. 重定向到原始 URL
        response.redirect(tx.getOriginalUrl());
    }
    
    private Cookie createSessionCookie(String sessionId) {
        Cookie cookie = new Cookie("SESSION_ID", sessionId);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setSameSite("Lax");
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        return cookie;
    }
}
```

### 6.3.3 登出 Handler

```java
package top.ilovemyhome.zorasso.muserver.handler;

import top.ilovemyhome.zorasso.core.service.SessionService;

/**
 * 登出 Handler
 * 
 * 处理本地登出和 OIDC 登出
 */
public class LogoutHandler {
    
    private final SessionService sessionService;
    private final String oidcEndSessionEndpoint;
    private final String postLogoutRedirectUri;
    
    /**
     * 处理登出请求
     * 
     * 1. 获取 Session Cookie
     * 2. 使会话无效
     * 3. 清除 Session Cookie
     * 4. 重定向到 OIDC 登出端点（可选）
     */
    public void handleLogout(Request request, Response response) {
        // 1. 获取 Session ID
        String sessionId = request.getCookie("SESSION_ID");
        
        // 2. 使会话无效
        if (sessionId != null) {
            sessionService.invalidateSession(sessionId);
        }
        
        // 3. 清除 Session Cookie
        response.setCookie(createClearCookie());
        
        // 4. 重定向到 OIDC 登出端点（如果配置）
        if (oidcEndSessionEndpoint != null && postLogoutRedirectUri != null) {
            String logoutUrl = buildEndSessionUrl();
            response.redirect(logoutUrl);
        } else {
            response.redirect("/");
        }
    }
    
    private Cookie createClearCookie() {
        Cookie cookie = new Cookie("SESSION_ID", "");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setSameSite("Lax");
        cookie.setPath("/");
        cookie.setMaxAge(0); // 立即过期
        return cookie;
    }
    
    private String buildEndSessionUrl() {
        StringBuilder url = new StringBuilder();
        url.append(oidcEndSessionEndpoint);
        url.append("?post_logout_redirect_uri=");
        url.append(urlEncode(postLogoutRedirectUri));
        
        // 如果有 ID Token_hint，添加
        // url.append("&id_token_hint=").append(urlEncode(idToken));
        
        return url.toString();
    }
    
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

## 6.4 客户端接入示例

### 6.4.1 引入依赖

```xml
<!-- pom.xml -->
<dependency>
    <groupId>top.ilovemyhome.zorasso</groupId>
    <artifactId>zora-sso-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 6.4.2 配置

```yaml
# application.yaml
zora:
  sso:
    issuer: https://keycloak.example.com/realms/myrealm
    client-id: my-web-app
    client-secret: ${ZORA_SSO_CLIENT_SECRET}
    redirect-uri: https://myapp.com/oidc/callback
    post-logout-redirect-uri: https://myapp.com
    expected-audience: my-web-app
    required-scopes:
      - openid
      - profile
      - email
    session:
      idle-timeout: 1800
      absolute-timeout: 36000
```

### 6.4.3 使用 SecurityContext

```java
import top.ilovemyhome.zorasso.si.security.SecurityContext;
import top.ilovemyhome.zorasso.si.security.AuthenticatedPrincipal;

/**
 * 示例控制器
 */
@RestController
public class UserController {
    
    @GetMapping("/api/me")
    public ResponseEntity<UserInfo> getCurrentUser() {
        // 获取当前认证用户
        AuthenticatedPrincipal principal = SecurityContext.getCurrentPrincipal();
        
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(principal.getUserId());
        userInfo.setUsername(principal.getUsername());
        userInfo.setEmail(principal.getEmail());
        userInfo.setName(principal.getName());
        
        return ResponseEntity.ok(userInfo);
    }
    
    @GetMapping("/api/admin/users")
    public ResponseEntity<List<UserInfo>> listUsers() {
        // 检查管理员权限
        AuthenticatedPrincipal principal = SecurityContext.getCurrentPrincipal();
        
        if (principal == null || !principal.hasRole("admin")) {
            return ResponseEntity.status(403).build();
        }
        
        // 列出用户
        return ResponseEntity.ok(List.of());
    }
}
```

## 6.5 本章小结

### 核心要点

```
┌─────────────────────────────────────────────────────────────────┐
│                      本章核心要点                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 项目架构                                                    │
│     • zora-sso-si: 领域模型和服务接口                           │
│     • zora-sso-core: 核心业务逻辑实现                          │
│     • zora-sso-muserver: HTTP 接入层                           │
│                                                                 │
│  2. 核心功能实现                                                │
│     • OIDC Discovery: 自动获取 Provider 配置                   │
│     • JWKS: 获取和缓存验签公钥                                  │
│     • 登录服务: 生成 state、nonce、PKCE 参数                   │
│     • Token 验证: 验证签名、iss、aud、exp、nonce              │
│     • 会话管理: 创建、查询、销毁会话                            │
│                                                                 │
│  3. HTTP Handler                                               │
│     • 登录 Handler: 创建事务并重定向到授权服务器               │
│     • 回调 Handler: 验证回调、兑换 Token、建立会话            │
│     • 登出 Handler: 清除会话并重定向到 OIDC 登出               │
│                                                                 │
│  4. 客户端接入                                                 │
│     • 引入依赖和配置                                            │
│     • 使用 SecurityContext 获取当前用户                       │
│     • 权限检查: hasRole() 方法                                 │
│                                                                 │
│  5. 安全要点                                                    │
│     • 所有 Token 和 Code 不记录到日志                          │
│     • state、nonce、PKCE 一次性使用                            │
│     • Session Cookie 安全配置                                  │
│     • 防止开放重定向                                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 课后思考

1. 为什么 OIDC Discovery 和 JWKS 需要缓存？缓存时间设置为多少合适？
2. 登录事务为什么需要设置过期时间？如果不设置会有什么安全风险？
3. Session 为什么要区分"绝对过期"和"空闲过期"？
4. 为什么回调处理时要先验证 state 再兑换 Token？

## 6.6 实践任务

### 任务 1：实现 OIDC Discovery 客户端

```java
// 练习：实现完整的 OIDC Discovery 客户端
// 1. 获取 .well-known/openid-configuration
// 2. 验证 issuer
// 3. 解析所有端点
// 4. 实现缓存机制
```

### 任务 2：实现 Token 验证

```java
// 练习：实现完整的 Token 验证
// 1. 验证签名算法（白名单）
// 2. 验证签名
// 3. 验证 iss、aud、exp、iat
// 4. 验证 nonce
// 5. 验证 auth_time（如果存在）
```

### 任务 3：实现完整的登录流程

```java
// 练习：实现完整的登录流程
// 1. 创建登录事务
// 2. 存储事务（内存或数据库）
// 3. 构建授权 URL
// 4. 处理回调
// 5. 验证参数
// 6. 兑换 Token
// 7. 验证 ID Token
// 8. 创建会话
// 9. 设置 Cookie
```

## 下章预告

下一章我们将学习 **高级主题与生产优化**，深入探讨生产环境所需的高级特性和优化措施，包括：
- 密钥轮换机制
- 分布式会话存储
- 单点登出（SSO Logout）
- 性能优化
- 监控与审计
- 故障处理与灾难恢复