package com.dungeoneer.auth.adapter.in.web.controller;

import com.dungeoneer.auth.adapter.in.web.dto.AddRoleRequest;
import com.dungeoneer.auth.adapter.in.web.dto.LoginRequest;
import com.dungeoneer.auth.adapter.in.web.dto.PublicKeyResponse;
import com.dungeoneer.auth.adapter.in.web.dto.RegisterRequest;
import com.dungeoneer.auth.adapter.in.web.dto.UpdateAvatarRequest;
import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.application.dto.UserProfileDto;
import com.dungeoneer.auth.application.port.in.GetProfileUseCase;
import com.dungeoneer.auth.application.port.in.LoginUserCommand;
import com.dungeoneer.auth.application.port.in.LoginUserUseCase;
import com.dungeoneer.auth.application.port.in.RegisterUserCommand;
import com.dungeoneer.auth.application.port.in.RegisterUserUseCase;
import com.dungeoneer.auth.application.port.in.SelfPromoteUseCase;
import com.dungeoneer.auth.application.port.in.UpdateAvatarUseCase;
import com.dungeoneer.auth.application.port.in.UpdateUserRolesUseCase;
import com.dungeoneer.auth.application.port.out.PasswordCipherPort;
import com.dungeoneer.auth.application.port.out.TokenProviderPort;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String AUTH_COOKIE_NAME = "dungeoneer_auth_token";

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final UpdateUserRolesUseCase updateUserRolesUseCase;
    private final SelfPromoteUseCase selfPromoteUseCase;
    private final UpdateAvatarUseCase updateAvatarUseCase;
    private final TokenProviderPort tokenProviderPort;
    private final PasswordCipherPort passwordCipherPort;
    private final boolean secureCookie;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            GetProfileUseCase getProfileUseCase,
            UpdateUserRolesUseCase updateUserRolesUseCase,
            SelfPromoteUseCase selfPromoteUseCase,
            UpdateAvatarUseCase updateAvatarUseCase,
            TokenProviderPort tokenProviderPort,
            PasswordCipherPort passwordCipherPort
    ) {
        this(registerUserUseCase, loginUserUseCase, getProfileUseCase, updateUserRolesUseCase, selfPromoteUseCase, updateAvatarUseCase, tokenProviderPort, passwordCipherPort, false);
    }

    @Autowired
    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            GetProfileUseCase getProfileUseCase,
            UpdateUserRolesUseCase updateUserRolesUseCase,
            SelfPromoteUseCase selfPromoteUseCase,
            UpdateAvatarUseCase updateAvatarUseCase,
            TokenProviderPort tokenProviderPort,
            PasswordCipherPort passwordCipherPort,
            @Value("${auth.cookie.secure:false}") boolean secureCookie
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.updateUserRolesUseCase = updateUserRolesUseCase;
        this.selfPromoteUseCase = selfPromoteUseCase;
        this.updateAvatarUseCase = updateAvatarUseCase;
        this.tokenProviderPort = tokenProviderPort;
        this.passwordCipherPort = passwordCipherPort;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey() {
        String keyBase64 = passwordCipherPort != null ? passwordCipherPort.getPublicKeyBase64() : "";
        return ResponseEntity.ok(PublicKeyResponse.rsaOaep(keyBase64));
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
        ResponseCookie cookie = createAuthCookie(response.accessToken(), response.expiresIn());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequest request) {
        LoginUserCommand command = new LoginUserCommand(
                request.email(),
                request.password()
        );
        AuthResponseDto response = loginUserUseCase.login(command);
        ResponseCookie cookie = createAuthCookie(response.accessToken(), response.expiresIn());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = createAuthCookie("", 0L);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return getProfileUseCase.getProfileFromToken(token)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<AuthResponseDto> addRole(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody AddRoleRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader
    ) {
        UUID callerId = resolveCallerId(authHeader, xUserIdHeader);
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!callerId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        AuthResponseDto response = updateUserRolesUseCase.addRole(userId, request.role());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/roles/promote-dm")
    public ResponseEntity<AuthResponseDto> selfPromoteToDungeonMaster(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader
    ) {
        UUID callerId = resolveCallerId(authHeader, xUserIdHeader);
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponseDto response = selfPromoteUseCase.promoteToDungeonMaster(callerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/roles/toggle-dm")
    public ResponseEntity<AuthResponseDto> toggleDungeonMasterRole(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader
    ) {
        UUID callerId = resolveCallerId(authHeader, xUserIdHeader);
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponseDto response = selfPromoteUseCase.toggleDungeonMasterRole(callerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserProfileDto> updateAvatar(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader,
            @Valid @RequestBody UpdateAvatarRequest request
    ) {
        UUID callerId = resolveCallerId(authHeader, xUserIdHeader);
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserProfileDto updated = updateAvatarUseCase.updateAvatar(callerId, request.avatarUrl());
        return ResponseEntity.ok(updated);
    }

    private ResponseCookie createAuthCookie(String token, long maxAge) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token != null ? token : "")
                .httpOnly(true)
                .secure(this.secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private UUID resolveCallerId(String authHeader, String xUserIdHeader) {
        if (xUserIdHeader != null && !xUserIdHeader.isBlank()) {
            try {
                return UUID.fromString(xUserIdHeader.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        String token = extractToken(authHeader);
        if (token != null) {
            return tokenProviderPort.validateTokenAndGetUserId(token).orElse(null);
        }
        return null;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        while (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return token.isBlank() ? null : token;
    }
}
