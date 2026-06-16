package top.ilovemyhome.dagtask.scheduler.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.ilovemyhome.dagtask.si.auth.GenerateTokenRequest;
import top.ilovemyhome.dagtask.si.auth.TokenInfo;

class TokenManagementApiTest {

    private TokenService tokenService;
    private TokenManagementApi api;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        api = new TokenManagementApi(tokenService);
    }

    @Test
    @DisplayName("generateToken returns 400 when request is null")
    void generateToken_nullRequest_returns400() {
        Response response = api.generateToken(null, "admin");

        assertThat(response.getStatus()).isEqualTo(400);
        verify(tokenService, never()).generateToken(anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("generateToken returns 400 when service throws IllegalArgumentException")
    void generateToken_serviceThrows_returns400() {
        when(tokenService.generateToken(anyString(), anyString(), anyInt(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid parameter"));

        GenerateTokenRequest request = new GenerateTokenRequest("test", "desc", 1);
        Response response = api.generateToken(request, "admin");

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("generateToken returns 200 when successful")
    void generateToken_successful_returns200() {
        TokenInfo tokenInfo = new TokenInfo(
            1L, "token-id", "agent-id", "test", "desc",
            "admin", Instant.now(), Instant.now().plusSeconds(86400), "jwt-token"
        );
        when(tokenService.generateToken(anyString(), anyString(), anyInt(), anyString()))
            .thenReturn(tokenInfo);

        GenerateTokenRequest request = new GenerateTokenRequest("test", "desc", 1);
        Response response = api.generateToken(request, "admin");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("revokeToken returns 400 when service throws IllegalArgumentException")
    void revokeToken_serviceThrows_returns400() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Invalid parameter"))
            .when(tokenService).revokeToken(anyString(), anyString());

        Response response = api.revokeToken("token-id", "admin");

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("revokeToken returns 200 when successful")
    void revokeToken_successful_returns200() {
        Response response = api.revokeToken("token-id", "admin");

        assertThat(response.getStatus()).isEqualTo(200);
        verify(tokenService).revokeToken("token-id", "admin");
    }
}
