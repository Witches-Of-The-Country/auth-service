package com.dungeoneer.auth.adapter.in.web.dto;

import com.dungeoneer.auth.domain.model.Role;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Nickname is required")
        @Size(min = 3, max = 50, message = "Nickname must be between 3 and 50 characters")
        String nickname,

        @NotNull(message = "Birthdate is required")
        @Past(message = "Birthdate must be a date in the past")
        @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
        LocalDate birthdate,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
        String password,

        List<Role> roles
) {
}
