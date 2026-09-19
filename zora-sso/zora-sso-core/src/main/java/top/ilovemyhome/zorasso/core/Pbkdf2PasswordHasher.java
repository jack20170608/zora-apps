package top.ilovemyhome.zorasso.core;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class Pbkdf2PasswordHasher implements PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 210_000;

    public Pbkdf2PasswordHasher() {
        this(DEFAULT_ITERATIONS, new SecureRandom());
    }

    Pbkdf2PasswordHasher(int iterations, SecureRandom secureRandom) {
        if (iterations < 100_000) {
            throw new IllegalArgumentException("iterations must be at least 100000");
        }
        this.iterations = iterations;
        this.secureRandom = secureRandom;
    }

    @Override
    public String hash(char[] secret) {
        validateSecret(secret);
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        byte[] derived = derive(secret, salt, iterations);
        return "pbkdf2-sha256$" + iterations + "$"
            + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$"
            + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
    }

    @Override
    public boolean verify(char[] secret, String encodedHash) {
        validateSecret(secret);
        if (encodedHash == null) {
            return false;
        }
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !"pbkdf2-sha256".equals(parts[0])) {
            return false;
        }
        try {
            int configuredIterations = Integer.parseInt(parts[1]);
            if (configuredIterations < 100_000) {
                return false;
            }
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
            if (salt.length < 16 || expected.length != 32) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(secret, salt, configuredIterations));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] derive(char[] secret, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(secret, salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is not available", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static void validateSecret(char[] secret) {
        if (secret == null || secret.length == 0) {
            throw new IllegalArgumentException("secret must not be empty");
        }
    }

    private final int iterations;
    private final SecureRandom secureRandom;
}
