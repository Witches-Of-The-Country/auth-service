package com.dungeoneer.auth.adapter.in.web.dto;

import com.dungeoneer.auth.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record AddRoleRequest(
        @NotNull(message = "Role must not be null")
        Role role
) {}
