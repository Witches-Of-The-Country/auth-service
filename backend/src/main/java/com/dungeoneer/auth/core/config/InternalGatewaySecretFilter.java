package com.dungeoneer.auth.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalGatewaySecretFilter extends OncePerRequestFilter {

    public static final String HEADER_INTERNAL_SECRET = "X-Internal-Secret";

    private final String expectedSecret;

    public InternalGatewaySecretFilter(@Value("${internal.gateway.secret:}") String expectedSecret) {
        this.expectedSecret = expectedSecret != null ? expectedSecret.trim() : "";
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (expectedSecret.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isExemptPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedSecret = request.getHeader(HEADER_INTERNAL_SECRET);
        if (providedSecret == null || !expectedSecret.equals(providedSecret.trim())) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/problem+json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String json = String.format(
                    "{\"type\":\"https://dungeoneer.app/errors/forbidden\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"Access denied: missing or invalid perimeter secret\",\"instance\":\"%s\"}",
                    path != null ? path : "/"
            );
            response.getWriter().write(json);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        if (path == null) return false;
        return path.startsWith("/actuator") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/error");
    }
}
