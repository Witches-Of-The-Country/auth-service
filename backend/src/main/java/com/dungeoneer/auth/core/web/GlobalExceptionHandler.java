package com.dungeoneer.auth.core.web;

import com.dungeoneer.auth.domain.exception.DuplicateUserException;
import com.dungeoneer.auth.domain.exception.InvalidCredentialsException;
import com.dungeoneer.auth.domain.exception.UserValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserException.class)
    public ProblemDetail handleDuplicateUserException(DuplicateUserException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://dungeoneer.app/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentialsException(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://dungeoneer.app/errors/unauthorized"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(UserValidationException.class)
    public ProblemDetail handleUserValidationException(UserValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://dungeoneer.app/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid request payload");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://dungeoneer.app/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://dungeoneer.app/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
