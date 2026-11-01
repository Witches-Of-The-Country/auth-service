package com.dungeoneer.auth.application.port.in;

import com.dungeoneer.auth.domain.model.Role;

import java.time.LocalDate;
import java.util.List;

public record RegisterUserCommand(
        String email,
        String nickname,
        LocalDate birthdate,
        String rawPassword,
        List<Role> roles
) {
    public RegisterUserCommand(String email, String nickname, LocalDate birthdate, String rawPassword) {
        this(email, nickname, birthdate, rawPassword, null);
    }
}
