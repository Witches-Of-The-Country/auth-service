package com.dungeoneer.auth.application.port.in;

import com.dungeoneer.auth.application.dto.UserProfileDto;

import java.util.Optional;

public interface GetProfileUseCase {
    Optional<UserProfileDto> getProfileFromToken(String token);
}
