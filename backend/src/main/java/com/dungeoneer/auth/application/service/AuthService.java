package com.dungeoneer.auth.application.service;

import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.application.dto.UserProfileDto;
import com.dungeoneer.auth.application.port.in.GetProfileUseCase;
import com.dungeoneer.auth.application.port.in.LoginUserCommand;
import com.dungeoneer.auth.application.port.in.LoginUserUseCase;
import com.dungeoneer.auth.application.port.in.RegisterUserCommand;
import com.dungeoneer.auth.application.port.in.RegisterUserUseCase;
import com.dungeoneer.auth.application.port.in.SelfPromoteUseCase;
import com.dungeoneer.auth.application.port.in.UpdateUserRolesUseCase;
import com.dungeoneer.auth.application.port.out.PasswordEncoderPort;
import com.dungeoneer.auth.application.port.out.TokenProviderPort;
import com.dungeoneer.auth.application.port.out.UserRepositoryPort;
import com.dungeoneer.auth.domain.exception.DuplicateUserException;
import com.dungeoneer.auth.domain.exception.InvalidCredentialsException;
import com.dungeoneer.auth.domain.exception.UserNotFoundException;
import com.dungeoneer.auth.domain.model.Role;
import com.dungeoneer.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements RegisterUserUseCase, LoginUserUseCase, GetProfileUseCase, UpdateUserRolesUseCase, SelfPromoteUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;

    public AuthService(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            TokenProviderPort tokenProviderPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterUserCommand command) {
        String normalizedEmail = command.email().trim().toLowerCase();
        String normalizedNickname = command.nickname().trim();

        if (userRepositoryPort.existsByEmail(normalizedEmail)) {
            throw new DuplicateUserException("Email is already registered: " + normalizedEmail);
        }
        if (userRepositoryPort.existsByNickname(normalizedNickname)) {
            throw new DuplicateUserException("Nickname is already taken: " + normalizedNickname);
        }

        String hashedPassword = passwordEncoderPort.encode(command.rawPassword());
        User newUser = User.createNew(normalizedEmail, normalizedNickname, command.birthdate(), hashedPassword, command.roles());
        User savedUser = userRepositoryPort.save(newUser);

        String token = tokenProviderPort.generateToken(savedUser);
        return AuthResponseDto.bearer(token, tokenProviderPort.getExpirationSeconds(), UserProfileDto.fromDomain(savedUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginUserCommand command) {
        String normalizedEmail = command.email().trim().toLowerCase();

        User user = userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoderPort.matches(command.rawPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = tokenProviderPort.generateToken(user);
        return AuthResponseDto.bearer(token, tokenProviderPort.getExpirationSeconds(), UserProfileDto.fromDomain(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDto> getProfileFromToken(String token) {
        return tokenProviderPort.validateTokenAndGetUserId(token)
                .flatMap(userRepositoryPort::findById)
                .map(UserProfileDto::fromDomain);
    }

    @Override
    @Transactional
    public AuthResponseDto addRole(UUID userId, Role role) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.addRole(role);
        User updatedUser = userRepositoryPort.save(user);

        String token = tokenProviderPort.generateToken(updatedUser);
        return AuthResponseDto.bearer(token, tokenProviderPort.getExpirationSeconds(), UserProfileDto.fromDomain(updatedUser));
    }

    @Override
    @Transactional
    public AuthResponseDto promoteToDungeonMaster(UUID userId) {
        return addRole(userId, Role.DUNGEON_MASTER);
    }
}
