package top.ilovemyhome.dagtask.allinone.muserver.security;

import io.muserver.Cookie;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllInOneSecurityHandlerTest {

    @Mock
    private MuRequest request;

    @Mock
    private MuResponse response;

    @Mock
    private Function<String, String> authenticator;

    private AllInOneSecurityHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AllInOneSecurityHandler("token", authenticator,
            List.of("/login", "/api/agent/health", "/api/agent/ping", "/swagger", "/static"));
    }

    @Test
    void whitelistedPath_shouldPassThrough() {
        // Given
        when(request.uri()).thenReturn(URI.create("/login"));

        // When
        boolean handled = handler.handle(request, response);

        // Then
        assertThat(handled).isFalse(); // false means continue to next handler
        verifyNoInteractions(authenticator);
    }

    @Test
    void validToken_shouldPassThrough() {
        // Given
        when(request.uri()).thenReturn(URI.create("/api/admin/stats"));
        when(request.cookie("token")).thenReturn(java.util.Optional.of("valid-jwt-token"));
        when(authenticator.apply("valid-jwt-token")).thenReturn("admin");

        // When
        boolean handled = handler.handle(request, response);

        // Then
        assertThat(handled).isFalse();
        verify(request).attribute("user", "admin");
    }

    @Test
    void missingToken_shouldReturn401() {
        // Given
        when(request.uri()).thenReturn(URI.create("/api/admin/stats"));
        when(request.cookie("token")).thenReturn(java.util.Optional.empty());

        // When
        boolean handled = handler.handle(request, response);

        // Then
        assertThat(handled).isTrue();
        verify(response).status(401);
    }

    @Test
    void invalidToken_shouldReturn401() {
        // Given
        when(request.uri()).thenReturn(URI.create("/api/admin/stats"));
        when(request.cookie("token")).thenReturn(java.util.Optional.of("invalid-token"));
        when(authenticator.apply("invalid-token")).thenReturn(null);

        // When
        boolean handled = handler.handle(request, response);

        // Then
        assertThat(handled).isTrue();
        verify(response).status(401);
    }
}
