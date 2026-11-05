package com.dungeoneer.auth.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("SEC-MED-02: handleGenericException sanitizes 500 internal server error details")
    void shouldSanitize500Errors() {
        Exception leakException = new RuntimeException("SELECT * FROM users WHERE password_hash = 'leak' Connection refused");
        ProblemDetail problem = handler.handleGenericException(leakException);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).doesNotContain("SELECT");
        assertThat(problem.getDetail()).doesNotContain("Connection refused");
    }
}
