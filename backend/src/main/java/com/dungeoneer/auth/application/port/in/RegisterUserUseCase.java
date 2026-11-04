package com.dungeoneer.auth.application.port.in;

import com.dungeoneer.auth.application.dto.AuthResponseDto;

public interface RegisterUserUseCase {
    AuthResponseDto register(RegisterUserCommand command);
}
