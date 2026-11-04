package com.dungeoneer.auth.application.port.in;

public record LoginUserCommand(
        String email,
        String rawPassword
) {
}
