package top.ilovemyhome.dagtask.scheduler.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.ilovemyhome.dagtask.scheduler.config.JwtConfig;
import top.ilovemyhome.dagtask.scheduler.port.out.AgentTokenRepository;

class TokenServiceTest {

    private AgentTokenRepository repository;
    private JwtConfig jwtConfig;
    private TokenService tokenService;

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(AgentTokenRepository.class);

        // Generate test keys
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        jwtConfig = new JwtConfig(
            "test-issuer",
            "test-audience",
            "test-path",
            "test-path",
            publicKey,
            privateKey,
            86400L
        );

        tokenService = new TokenService(repository, jwtConfig);
    }

    @Test
    @DisplayName("generateToken throws when name is null")
    void generateToken_nullName_throws() {
        assertThatThrownBy(() -> tokenService.generateToken(null, "desc", 1, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("name must not be null or blank");

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("generateToken throws when name is blank")
    void generateToken_blankName_throws() {
        assertThatThrownBy(() -> tokenService.generateToken("   ", "desc", 1, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("name must not be null or blank");

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("generateToken throws when expiresInDays is zero")
    void generateToken_zeroExpiresInDays_throws() {
        assertThatThrownBy(() -> tokenService.generateToken("test-token", "desc", 0, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expiresInDays must be greater than 0");

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("generateToken throws when expiresInDays is negative")
    void generateToken_negativeExpiresInDays_throws() {
        assertThatThrownBy(() -> tokenService.generateToken("test-token", "desc", -5, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expiresInDays must be greater than 0");

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("generateToken throws when createdBy is null")
    void generateToken_nullCreatedBy_throws() {
        assertThatThrownBy(() -> tokenService.generateToken("test-token", "desc", 1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("createdBy must not be null or blank");

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("generateToken throws when createdBy is blank")
    void generateToken_blankCreatedBy_throws() {
        assertThatThrownBy(() -> tokenService.generateToken("test-token", "desc", 1, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("createdBy must not be null or blank");

        verify(repository, never()).create(any());
    }

    @Test
    @DisplayName("generateToken succeeds with valid parameters")
    void generateToken_validParameters_succeeds() {
        var result = tokenService.generateToken("test-token", "desc", 1, "admin");

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("test-token");
        assertThat(result.token()).isNotNull();
        verify(repository).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("validateToken returns false for blank JWT")
    void validateToken_blankJwt_returnsFalse(String jwt) {
        boolean result = tokenService.validateToken(jwt);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for null JWT")
    void validateToken_nullJwt_returnsFalse() {
        boolean result = tokenService.validateToken(null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("revokeToken throws when tokenId is null")
    void revokeToken_nullTokenId_throws() {
        assertThatThrownBy(() -> tokenService.revokeToken(null, "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tokenId must not be null or blank");

        verify(repository, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("revokeToken throws when tokenId is blank")
    void revokeToken_blankTokenId_throws() {
        assertThatThrownBy(() -> tokenService.revokeToken("   ", "admin"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tokenId must not be null or blank");

        verify(repository, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("revokeToken throws when revokedBy is null")
    void revokeToken_nullRevokedBy_throws() {
        assertThatThrownBy(() -> tokenService.revokeToken("token-123", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("revokedBy must not be null or blank");

        verify(repository, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("revokeToken throws when revokedBy is blank")
    void revokeToken_blankRevokedBy_throws() {
        assertThatThrownBy(() -> tokenService.revokeToken("token-123", "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("revokedBy must not be null or blank");

        verify(repository, never()).revokeToken(any(), any());
    }

    @Test
    @DisplayName("revokeToken succeeds with valid parameters")
    void revokeToken_validParameters_succeeds() {
        tokenService.revokeToken("token-123", "admin");

        verify(repository).revokeToken("token-123", "admin");
    }
}
