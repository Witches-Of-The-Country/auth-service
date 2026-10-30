package com.dungeoneer.auth.domain.model;

import com.dungeoneer.auth.domain.exception.UserValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Should successfully create a valid User")
    void shouldCreateValidUser() {
        UUID id = UUID.randomUUID();
        LocalDate birthdate = LocalDate.of(1995, 5, 20);
        User user = new User(id, "Adventurer@Email.com", "Torvald", birthdate, "$2a$10$hashedpw", List.of(Role.PLAYER));

        assertEquals(id, user.getId());
        assertEquals("adventurer@email.com", user.getEmail());
        assertEquals("Torvald", user.getNickname());
        assertEquals(birthdate, user.getBirthdate());
        assertEquals("$2a$10$hashedpw", user.getPasswordHash());
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(Role.PLAYER));
    }

    @Test
    @DisplayName("Should create user using factory method createNew with default Role.PLAYER")
    void shouldCreateNewUserWithDefaults() {
        LocalDate birthdate = LocalDate.of(2000, 1, 1);
        User user = User.createNew("gandalf@middleearth.com", "Mithrandir", birthdate, "hash123");

        assertNotNull(user.getId());
        assertEquals("gandalf@middleearth.com", user.getEmail());
        assertEquals("Mithrandir", user.getNickname());
        assertEquals(List.of(Role.PLAYER), user.getRoles());
    }

    @Test
    @DisplayName("Should allow adding a new role to the user")
    void shouldAddRole() {
        User user = User.createNew("dm@dnd.com", "MasterDungeon", LocalDate.of(1990, 3, 15), "hash");
        user.addRole(Role.DUNGEON_MASTER);

        assertTrue(user.getRoles().contains(Role.DUNGEON_MASTER));
        assertEquals(2, user.getRoles().size());
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmail() {
        assertThrows(UserValidationException.class, () ->
                User.createNew("invalid-email", "Hero", LocalDate.of(1990, 1, 1), "hash"));
    }

    @Test
    @DisplayName("Should reject blank or too short nickname")
    void shouldRejectShortNickname() {
        assertThrows(UserValidationException.class, () ->
                User.createNew("valid@email.com", "ab", LocalDate.of(1990, 1, 1), "hash"));
    }

    @Test
    @DisplayName("Should reject birthdate in the future")
    void shouldRejectFutureBirthdate() {
        assertThrows(UserValidationException.class, () ->
                User.createNew("valid@email.com", "Hero", LocalDate.now().plusDays(1), "hash"));
    }

    @Test
    @DisplayName("Should reject empty password hash")
    void shouldRejectEmptyPasswordHash() {
        assertThrows(UserValidationException.class, () ->
                User.createNew("valid@email.com", "Hero", LocalDate.of(1990, 1, 1), " "));
    }
}
