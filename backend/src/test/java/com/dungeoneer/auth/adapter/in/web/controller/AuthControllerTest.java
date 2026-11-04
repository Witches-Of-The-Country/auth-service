package com.dungeoneer.auth.adapter.in.web.controller;

import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.application.dto.UserProfileDto;
import com.dungeoneer.auth.application.port.in.GetProfileUseCase;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private LoginUserUseCase loginUserUseCase;

    @Mock
    private GetProfileUseCase getProfileUseCase;

    @Mock
    private com.dungeoneer.auth.application.port.in.UpdateUserRolesUseCase updateUserRolesUseCase;

    @Mock
    private com.dungeoneer.auth.application.port.in.SelfPromoteUseCase selfPromoteUseCase;

    @Mock
    private com.dungeoneer.auth.application.port.out.TokenProviderPort tokenProviderPort;

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

    @Test
    @DisplayName("GET /api/auth/me with valid Bearer token returns 200 and UserProfileDto")
    void shouldReturnProfileWithValidToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = new UserProfileDto(userId, "aragorn@gondor.com", "Strider", LocalDate.of(1990, 1, 1), List.of(Role.PLAYER));

        when(getProfileUseCase.getProfileFromToken("valid.jwt.token")).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("aragorn@gondor.com"))
                .andExpect(jsonPath("$.nickname").value("Strider"));
    }

    @Test
    @DisplayName("GET /api/auth/me without token returns 401 Unauthorized")
    void shouldReturn401WithoutAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/me with invalid or wiped token returns 401 Unauthorized")
    void shouldReturn401WhenUserNotFound() throws Exception {
        when(getProfileUseCase.getProfileFromToken("wiped.jwt.token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer wiped.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/users/{userId}/roles with valid role returns 200 OK")
    void shouldAddRoleSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = new UserProfileDto(userId, "aragorn@gondor.com", "Strider", LocalDate.of(1990, 1, 1), List.of(Role.PLAYER, Role.DUNGEON_MASTER));
        AuthResponseDto authResponse = AuthResponseDto.bearer("new.jwt.token", 86400L, profile);

        when(updateUserRolesUseCase.addRole(eq(userId), eq(Role.DUNGEON_MASTER))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/users/" + userId + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"DUNGEON_MASTER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.jwt.token"))
                .andExpect(jsonPath("$.user.roles[1]").value("DUNGEON_MASTER"));
    }

    @Test
    @DisplayName("POST /api/auth/users/{userId}/roles with missing role returns 422 Unprocessable Entity")
    void shouldFailValidationOnMissingRole() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/auth/users/" + userId + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/auth/me/roles/promote-dm with valid token returns 200 and updated JWT")
    void shouldSelfPromoteToDmSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileDto profile = new UserProfileDto(userId, "aragorn@gondor.com", "Strider", LocalDate.of(1990, 1, 1), List.of(Role.PLAYER, Role.DUNGEON_MASTER));
        AuthResponseDto authResponse = AuthResponseDto.bearer("promoted.jwt.token", 86400L, profile);

        when(tokenProviderPort.validateTokenAndGetUserId("valid.bearer.token")).thenReturn(Optional.of(userId));
        when(selfPromoteUseCase.promoteToDungeonMaster(userId)).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/me/roles/promote-dm")
                        .header("Authorization", "Bearer valid.bearer.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("promoted.jwt.token"))
                .andExpect(jsonPath("$.user.roles[1]").value("DUNGEON_MASTER"));
    }

    @Test
    @DisplayName("POST /api/auth/me/roles/promote-dm without token returns 401 Unauthorized")
    void shouldReturn401WhenPromotingWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/me/roles/promote-dm"))
                .andExpect(status().isUnauthorized());
    }
}
