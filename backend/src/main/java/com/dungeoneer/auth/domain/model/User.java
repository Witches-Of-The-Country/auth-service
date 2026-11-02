package com.dungeoneer.auth.domain.model;

import com.dungeoneer.auth.domain.exception.UserValidationException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class User {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UUID id;
    private final String email;
    private final String nickname;
    private final LocalDate birthdate;
    private final String passwordHash;
    private final List<Role> roles;
    private String avatarUrl;

    public User(UUID id, String email, String nickname, LocalDate birthdate, String passwordHash, List<Role> roles, String avatarUrl) {
        if (id == null) {
            throw new UserValidationException("User id cannot be null");
        }
        if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new UserValidationException("Invalid email format");
        }
        if (nickname == null || nickname.trim().isEmpty() || nickname.trim().length() < 3 || nickname.trim().length() > 50) {
            throw new UserValidationException("Nickname must be between 3 and 50 characters");
        }
        if (birthdate == null || !birthdate.isBefore(LocalDate.now())) {
            throw new UserValidationException("Birthdate must be a date in the past");
        }
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new UserValidationException("Password hash cannot be empty");
        }

        validateAvatarUrl(avatarUrl);

        this.id = id;
        this.email = email.trim().toLowerCase();
        this.nickname = nickname.trim();
        this.birthdate = birthdate;
        this.passwordHash = passwordHash;
        this.roles = new ArrayList<>(roles != null && !roles.isEmpty() ? roles : List.of(Role.PLAYER));
        this.avatarUrl = avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl.trim() : null;
    }

    public User(UUID id, String email, String nickname, LocalDate birthdate, String passwordHash, List<Role> roles) {
        this(id, email, nickname, birthdate, passwordHash, roles, null);
    }

    private void validateAvatarUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (url.length() > 700000) {
            throw new UserValidationException("Avatar URL exceeds maximum allowed length of 700,000 characters");
        }
        String lower = url.trim().toLowerCase();
        if (lower.contains("<svg") || lower.contains("image/svg+xml") || lower.contains("javascript:") || lower.contains("<script")) {
            throw new UserValidationException("Invalid avatar format. SVG and script injections are strictly forbidden.");
        }
        if (!lower.startsWith("data:image/") && !lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new UserValidationException("Avatar must be a valid image Data URL or HTTP(S) link");
        }
    }

    public static User createNew(String email, String nickname, LocalDate birthdate, String passwordHash) {
        return createNew(email, nickname, birthdate, passwordHash, List.of(Role.PLAYER));
    }

    public static User createNew(String email, String nickname, LocalDate birthdate, String passwordHash, List<Role> roles) {
        List<Role> initialRoles = (roles != null && !roles.isEmpty()) ? roles : List.of(Role.PLAYER);
        return new User(UUID.randomUUID(), email, nickname, birthdate, passwordHash, initialRoles);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void updateAvatarUrl(String avatarUrl) {
        validateAvatarUrl(avatarUrl);
        this.avatarUrl = avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl.trim() : null;
    }

    public void addRole(Role role) {
        if (role != null && !this.roles.contains(role)) {
            this.roles.add(role);
        }
    }

    public void removeRole(Role role) {
        if (role == Role.PLAYER) {
            throw new UserValidationException("Base role PLAYER cannot be removed");
        }
        if (role != null) {
            this.roles.remove(role);
        }
    }
}
