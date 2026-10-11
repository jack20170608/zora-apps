package top.ilovemyhome.zorasso.core;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class Pbkdf2PasswordHasherTest {

    @Test
    void hashesAndVerifiesWithoutEmbeddingPlaintext() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

        String encoded = hasher.hash("correct horse battery staple".toCharArray());

        assertThat(encoded).startsWith("pbkdf2-sha256$");
        assertThat(encoded).doesNotContain("correct horse battery staple");
        assertThat(hasher.verify("correct horse battery staple".toCharArray(), encoded)).isTrue();
        assertThat(hasher.verify("wrong".toCharArray(), encoded)).isFalse();
    }

    @Test
    void rejectsMalformedOrWeakHashes() {
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(101, new SecureRandom());

        assertThat(hasher.verify("secret".toCharArray(), "plaintext")).isFalse();
        assertThat(hasher.verify(
            "secret".toCharArray(),
            "pbkdf2-sha256$101$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        )).isFalse();
    }

    @Test
    @Disabled
    void testHashGeneration(){
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        String password = "1";
        String encoded = hasher.hash(password.toCharArray());
        System.out.println(encoded);
    }
}
