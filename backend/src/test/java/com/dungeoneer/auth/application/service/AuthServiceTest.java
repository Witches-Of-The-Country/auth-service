package com.dungeoneer.auth.application.service;

import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.application.port.in.LoginUserCommand;
import com.dungeoneer.auth.application.port.in.RegisterUserCommand;
import com.dungeoneer.auth.application.port.out.PasswordEncoderPort;
import com.dungeoneer.auth.application.port.out.TokenProviderPort;
import com.dungeoneer.auth.application.port.out.UserRepositoryPort;
import com.dungeoneer.auth.domain.exception.DuplicateUserException;
import com.dungeoneer.auth.domain.exception.InvalidCredentialsException;
import com.dungeoneer.auth.domain.model.Role;
import com.dungeoneer.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                UUID.randomUUID(),
                "thorin@erebor.com",
                "Oakenshield",
                LocalDate.of(1985, 4, 10),
                "$2a$10$hashedpassword",
                List.of(Role.PLAYER)
        );
    }

    @Test
    @DisplayName("Should successfully register a new user and return auth response")
    void shouldRegisterNewUser() {
        RegisterUserCommand command = new RegisterUserCommand(
                "Thorin@Erebor.com",
                "Oakenshield",
                LocalDate.of(1985, 4, 10),
                "mypassword123"
        );

        when(userRepositoryPort.existsByEmail("thorin@erebor.com")).thenReturn(false);
        when(userRepositoryPort.existsByNickname("Oakenshield")).thenReturn(false);
        when(passwordEncoderPort.encode("mypassword123")).thenReturn("$2a$10$hashedpassword");
        when(userRepositoryPort.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProviderPort.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(tokenProviderPort.getExpirationSeconds()).thenReturn(86400L);

        AuthResponseDto response = authService.register(command);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86400L, response.expiresIn());
        assertEquals("thorin@erebor.com", response.user().email());
        assertEquals("Oakenshield", response.user().nickname());
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when email already exists")
    void shouldThrowWhenEmailExists() {
        RegisterUserCommand command = new RegisterUserCommand(
                "thorin@erebor.com",
                "Oakenshield",
                LocalDate.of(1985, 4, 10),
                "pw"
        );

        when(userRepositoryPort.existsByEmail("thorin@erebor.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.register(command));
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when nickname already exists")
    void shouldThrowWhenNicknameExists() {
        RegisterUserCommand command = new RegisterUserCommand(
                "unique@erebor.com",
                "Oakenshield",
                LocalDate.of(1985, 4, 10),
                "pw"
        );

        when(userRepositoryPort.existsByEmail("unique@erebor.com")).thenReturn(false);
        when(userRepositoryPort.existsByNickname("Oakenshield")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.register(command));
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully login user with correct credentials")
    void shouldLoginSuccessfully() {
        LoginUserCommand command = new LoginUserCommand("thorin@erebor.com", "mypassword123");

        when(userRepositoryPort.findByEmail("thorin@erebor.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoderPort.matches("mypassword123", sampleUser.getPasswordHash())).thenReturn(true);
        when(tokenProviderPort.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(tokenProviderPort.getExpirationSeconds()).thenReturn(86400L);

        AuthResponseDto response = authService.login(command);

        assertNotNull(response);
        assertEquals("sample.jwt.token", response.accessToken());
        assertEquals(sampleUser.getId(), response.user().id());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when user not found")
    void shouldThrowWhenUserNotFound() {
        LoginUserCommand command = new LoginUserCommand("unknown@dnd.com", "pw");
        when(userRepositoryPort.findByEmail("unknown@dnd.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(command));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void shouldThrowWhenPasswordMismatch() {
        LoginUserCommand command = new LoginUserCommand("thorin@erebor.com", "wrongpassword");

        when(userRepositoryPort.findByEmail("thorin@erebor.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoderPort.matches("wrongpassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(command));
    }

    @Test
    @DisplayName("Should successfully add role to user and return new auth response")
    void shouldAddRoleToUser() {
        when(userRepositoryPort.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenProviderPort.generateToken(any(User.class))).thenReturn("new.dm.jwt.token");
        when(tokenProviderPort.getExpirationSeconds()).thenReturn(86400L);

        AuthResponseDto response = authService.addRole(sampleUser.getId(), Role.DUNGEON_MASTER);

        assertNotNull(response);
        assertEquals("new.dm.jwt.token", response.accessToken());
        assertTrue(response.user().roles().contains(Role.DUNGEON_MASTER));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when adding role to non-existent user")
    void shouldThrowWhenAddingRoleToNonExistentUser() {
        UUID randomId = UUID.randomUUID();
        when(userRepositoryPort.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(com.dungeoneer.auth.domain.exception.UserNotFoundException.class, () ->
                authService.addRole(randomId, Role.DUNGEON_MASTER));
    }
}
