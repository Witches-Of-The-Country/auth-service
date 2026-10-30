package com.dungeoneer.auth.application.port.in;

import com.dungeoneer.auth.application.dto.AuthResponseDto;

public interface LoginUserUseCase {
    AuthResponseDto login(LoginUserCommand command);
}
