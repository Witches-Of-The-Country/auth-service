package com.dungeoneer.auth.application.port.in;

import com.dungeoneer.auth.application.dto.AuthResponseDto;

import java.util.UUID;

public interface SelfPromoteUseCase {
    AuthResponseDto promoteToDungeonMaster(UUID userId);
}
