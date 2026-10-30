package com.dungeoneer.auth.application.dto;

public record AuthResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserProfileDto user
) {
    public static AuthResponseDto bearer(String accessToken, long expiresIn, UserProfileDto user) {
        return new AuthResponseDto(accessToken, "Bearer", expiresIn, user);
    }
}
