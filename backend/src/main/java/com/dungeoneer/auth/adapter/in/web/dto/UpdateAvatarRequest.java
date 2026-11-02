package com.dungeoneer.auth.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
        @NotNull(message = "avatarUrl cannot be null")
        @Size(max = 700000, message = "avatarUrl cannot exceed 700,000 characters")
        String avatarUrl
) {}
