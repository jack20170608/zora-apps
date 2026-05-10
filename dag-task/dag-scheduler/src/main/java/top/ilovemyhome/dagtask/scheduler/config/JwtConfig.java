package top.ilovemyhome.dagtask.scheduler.config;

import java.security.PrivateKey;
import java.security.PublicKey;

public record JwtConfig(
    String issuer,
    PublicKey publicKey,
    PrivateKey privateKey,
    long ttl) {
}
