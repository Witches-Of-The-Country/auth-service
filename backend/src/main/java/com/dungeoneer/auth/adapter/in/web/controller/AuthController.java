package com.dungeoneer.auth.adapter.in.web.controller;

import com.dungeoneer.auth.adapter.in.web.dto.AddRoleRequest;
import com.dungeoneer.auth.adapter.in.web.dto.LoginRequest;
import com.dungeoneer.auth.adapter.in.web.dto.RegisterRequest;
import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.application.dto.UserProfileDto;
import com.dungeoneer.auth.application.port.in.GetProfileUseCase;
import com.dungeoneer.auth.application.port.in.LoginUserCommand;
import com.dungeoneer.auth.application.port.in.LoginUserUseCase;
import com.dungeoneer.auth.application.port.in.RegisterUserCommand;
import com.dungeoneer.auth.application.port.in.RegisterUserUseCase;
import com.dungeoneer.auth.application.port.in.SelfPromoteUseCase;
import com.dungeoneer.auth.application.port.in.UpdateUserRolesUseCase;
import com.dungeoneer.auth.application.port.out.TokenProviderPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final UpdateUserRolesUseCase updateUserRolesUseCase;
    private final SelfPromoteUseCase selfPromoteUseCase;
    private final TokenProviderPort tokenProviderPort;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            GetProfileUseCase getProfileUseCase,
            UpdateUserRolesUseCase updateUserRolesUseCase,
            SelfPromoteUseCase selfPromoteUseCase,
            TokenProviderPort tokenProviderPort
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.updateUserRolesUseCase = updateUserRolesUseCase;
        this.selfPromoteUseCase = selfPromoteUseCase;
        this.tokenProviderPort = tokenProviderPort;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.email(),
                request.nickname(),
                request.birthdate(),
                request.password(),
                request.roles()
        );
        AuthResponseDto response = registerUserUseCase.register(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequest request) {
        LoginUserCommand command = new LoginUserCommand(
                request.email(),
                request.password()
        );
        AuthResponseDto response = loginUserUseCase.login(command);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7).trim();
        while (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return getProfileUseCase.getProfileFromToken(token)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<AuthResponseDto> addRole(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody AddRoleRequest request
    ) {
        AuthResponseDto response = updateUserRolesUseCase.addRole(userId, request.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/roles/promote-dm")
    public ResponseEntity<AuthResponseDto> selfPromoteToDungeonMaster(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7).trim();
        while (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        return tokenProviderPort.validateTokenAndGetUserId(token)
                .map(selfPromoteUseCase::promoteToDungeonMaster)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
