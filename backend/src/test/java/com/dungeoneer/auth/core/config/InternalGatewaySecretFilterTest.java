package com.dungeoneer.auth.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InternalGatewaySecretFilterTest {

    private static final String SECRET = "auth-internal-secret-123";

    @Test
    @DisplayName("SEC-CRIT-02: allows request when X-Internal-Secret matches expected secret")
    void shouldAllowWhenSecretMatches() throws ServletException, IOException {
        InternalGatewaySecretFilter filter = new InternalGatewaySecretFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("X-Internal-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("SEC-CRIT-02: rejects with 403 Forbidden when X-Internal-Secret is missing")
    void shouldRejectWhenSecretMissing() throws ServletException, IOException {
        InternalGatewaySecretFilter filter = new InternalGatewaySecretFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Access denied: missing or invalid perimeter secret");
        assertThat(response.getContentType()).contains("application/problem+json");
    }

    @Test
    @DisplayName("SEC-CRIT-02: rejects with 403 Forbidden when X-Internal-Secret is invalid")
    void shouldRejectWhenSecretInvalid() throws ServletException, IOException {
        InternalGatewaySecretFilter filter = new InternalGatewaySecretFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("X-Internal-Secret", "wrong-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("SEC-CRIT-02: allows documentation and actuator paths without secret")
    void shouldAllowExemptPaths() throws ServletException, IOException {
        InternalGatewaySecretFilter filter = new InternalGatewaySecretFilter(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("allows all requests when no secret is configured (local dev fallback)")
    void shouldAllowWhenSecretNotConfigured() throws ServletException, IOException {
        InternalGatewaySecretFilter filter = new InternalGatewaySecretFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
