package com.dungeoneer.auth.adapter.in.web.controller;

import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.application.dto.UserProfileDto;
import com.dungeoneer.auth.application.port.in.LoginUserCommand;
import com.dungeoneer.auth.application.port.in.LoginUserUseCase;
import com.dungeoneer.auth.application.port.in.RegisterUserCommand;
import com.dungeoneer.auth.application.port.in.RegisterUserUseCase;
import com.dungeoneer.auth.core.web.GlobalExceptionHandler;
import com.dungeoneer.auth.domain.exception.DuplicateUserException;
import com.dungeoneer.auth.domain.exception.InvalidCredentialsException;
import com.dungeoneer.auth.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private LoginUserUseCase loginUserUseCase;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register with valid payload returns 201 Created and JWT")
    void shouldRegisterSuccessfully() throws Exception {
        String jsonPayload = """
                {
                    "email": "aragorn@gondor.com",
                    "nickname": "Strider",
                    "birthdate": "1987-03-01",
                    "password": "kingofgondor123"
                }
                """;

        AuthResponseDto responseDto = AuthResponseDto.bearer(
                "fake.jwt.token",
                86400L,
                new UserProfileDto(UUID.randomUUID(), "aragorn@gondor.com", "Strider", LocalDate.of(1987, 3, 1), List.of(Role.PLAYER))
        );

        when(registerUserUseCase.register(any(RegisterUserCommand.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("fake.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("aragorn@gondor.com"))
                .andExpect(jsonPath("$.user.nickname").value("Strider"));
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate email returns 409 Conflict ProblemDetail")
    void shouldReturn409OnDuplicateEmail() throws Exception {
        String jsonPayload = """
                {
                    "email": "aragorn@gondor.com",
                    "nickname": "Strider",
                    "birthdate": "1987-03-01",
                    "password": "kingofgondor123"
                }
                """;

        when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                .thenThrow(new DuplicateUserException("Email is already registered: aragorn@gondor.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid DTO returns 422 Unprocessable Entity")
    void shouldReturn422OnInvalidDto() throws Exception {
        String jsonPayload = """
                {
                    "email": "invalid-email",
                    "nickname": "ab",
                    "birthdate": "2099-01-01",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.nickname").exists())
                .andExpect(jsonPath("$.errors.birthdate").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns 200 OK and JWT")
    void shouldLoginSuccessfully() throws Exception {
        String jsonPayload = """
                {
                    "email": "aragorn@gondor.com",
                    "password": "kingofgondor123"
                }
                """;

        AuthResponseDto responseDto = AuthResponseDto.bearer(
                "valid.jwt.token",
                86400L,
                new UserProfileDto(UUID.randomUUID(), "aragorn@gondor.com", "Strider", LocalDate.of(1987, 3, 1), List.of(Role.PLAYER))
        );

        when(loginUserUseCase.login(any(LoginUserCommand.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("valid.jwt.token"))
                .andExpect(jsonPath("$.user.nickname").value("Strider"));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid credentials returns 401 Unauthorized ProblemDetail")
    void shouldReturn401OnBadCredentials() throws Exception {
        String jsonPayload = """
                {
                    "email": "aragorn@gondor.com",
                    "password": "wrongpassword"
                }
                """;

        when(loginUserUseCase.login(any(LoginUserCommand.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
