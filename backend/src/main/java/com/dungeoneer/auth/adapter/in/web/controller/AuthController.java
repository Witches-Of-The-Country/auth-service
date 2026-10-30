package com.dungeoneer.auth.adapter.in.web.controller;

import com.dungeoneer.auth.adapter.in.web.dto.LoginRequest;
import com.dungeoneer.auth.adapter.in.web.dto.RegisterRequest;
import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.application.port.in.LoginUserCommand;
import com.dungeoneer.auth.application.port.in.LoginUserUseCase;
import com.dungeoneer.auth.application.port.in.RegisterUserCommand;
import com.dungeoneer.auth.application.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUserUseCase loginUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
                request.email(),
                request.nickname(),
                request.birthdate(),
                request.password()
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
}
