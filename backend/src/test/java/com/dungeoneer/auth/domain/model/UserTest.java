package com.dungeoneer.auth.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Should create a user with valid properties")
    void shouldCreateUser() {
        UUID id = UUID.randomUUID();
        LocalDate birthdate = LocalDate.of(1990, 1, 1);
        
        User user = new User(id, "test@email.com", "TestUser", birthdate, "hashedPassword", List.of(Role.PLAYER));
        
        assertEquals(id, user.getId());
        assertEquals("test@email.com", user.getEmail());
        assertEquals("TestUser", user.getNickname());
        assertEquals(birthdate, user.getBirthdate());
        assertEquals("hashedPassword", user.getPasswordHash());
        assertTrue(user.getRoles().contains(Role.PLAYER));
    }

    @Test
    @DisplayName("Should add a new role to an existing user")
    void shouldAddRole() {
        User user = new User(UUID.randomUUID(), "test@email.com", "TestUser", LocalDate.now(), "hashed", List.of(Role.PLAYER));
        
        user.addRole(Role.DUNGEON_MASTER);
        
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains(Role.PLAYER));
        assertTrue(user.getRoles().contains(Role.DUNGEON_MASTER));
    }

    @Test
    @DisplayName("Should not duplicate roles when adding an existing role")
    void shouldNotDuplicateRole() {
        User user = new User(UUID.randomUUID(), "test@email.com", "TestUser", LocalDate.now(), "hashed", List.of(Role.PLAYER));
        
        user.addRole(Role.PLAYER);
        
        assertEquals(1, user.getRoles().size());
    }
}
