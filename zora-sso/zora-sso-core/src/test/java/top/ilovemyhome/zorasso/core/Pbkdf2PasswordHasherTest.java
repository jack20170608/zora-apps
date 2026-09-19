package top.ilovemyhome.zorasso.core;

import org.junit.jupiter.api.Test;

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
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

        assertThat(hasher.verify("secret".toCharArray(), "plaintext")).isFalse();
        assertThat(hasher.verify(
            "secret".toCharArray(),
            "pbkdf2-sha256$1$AAAAAAAAAAAAAAAAAAAAAA$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        )).isFalse();
    }
}
