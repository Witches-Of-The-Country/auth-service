package com.dungeoneer.auth.application.dto;

import com.dungeoneer.auth.domain.model.Role;
import com.dungeoneer.auth.domain.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String email,
        String nickname,
        LocalDate birthdate,
        List<Role> roles
) {
    public static UserProfileDto fromDomain(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getBirthdate(),
                user.getRoles()
        );
    }
}
