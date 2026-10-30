package com.dungeoneer.auth.application.port.in;

import java.time.LocalDate;

public record RegisterUserCommand(
        String email,
        String nickname,
        LocalDate birthdate,
        String rawPassword
) {
}
