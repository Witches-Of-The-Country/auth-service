package com.dungeoneer.auth.application.port.in;

import com.dungeoneer.auth.application.dto.AuthResponseDto;
import com.dungeoneer.auth.domain.model.Role;

import java.util.UUID;

public interface UpdateUserRolesUseCase {
    AuthResponseDto addRole(UUID userId, Role role);
}
