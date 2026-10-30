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

    public User(UUID id, String email, String nickname, LocalDate birthdate, String passwordHash, List<Role> roles) {
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

        this.id = id;
        this.email = email.trim().toLowerCase();
        this.nickname = nickname.trim();
        this.birthdate = birthdate;
        this.passwordHash = passwordHash;
        this.roles = new ArrayList<>(roles != null && !roles.isEmpty() ? roles : List.of(Role.PLAYER));
    }

    public static User createNew(String email, String nickname, LocalDate birthdate, String passwordHash) {
        return new User(UUID.randomUUID(), email, nickname, birthdate, passwordHash, List.of(Role.PLAYER));
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

    public void addRole(Role role) {
        if (role != null && !this.roles.contains(role)) {
            this.roles.add(role);
        }
    }
}
