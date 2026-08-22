package com.dungeoneer.auth.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class User {
    private final UUID id;
    private final String email;
    private final String nickname;
    private final LocalDate birthdate;
    private final String passwordHash;
    private final List<Role> roles;

    public User(UUID id, String email, String nickname, LocalDate birthdate, String passwordHash, List<Role> roles) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.birthdate = birthdate;
        this.passwordHash = passwordHash;
        this.roles = new ArrayList<>(roles != null ? roles : new ArrayList<>());
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
