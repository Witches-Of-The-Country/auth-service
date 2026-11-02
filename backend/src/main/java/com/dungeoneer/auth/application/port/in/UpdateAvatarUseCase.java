package com.dungeoneer.auth.application.port.in;

import com.dungeoneer.auth.application.dto.UserProfileDto;

import java.util.UUID;

public interface UpdateAvatarUseCase {
    UserProfileDto updateAvatar(UUID userId, String avatarUrl);
}
