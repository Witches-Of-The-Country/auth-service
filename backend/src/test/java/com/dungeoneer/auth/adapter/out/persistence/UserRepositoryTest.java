package com.dungeoneer.auth.adapter.out.persistence;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save a new user entity and retrieve it by email")
    void shouldSaveAndFindUserByEmail() {
        // Arrange
        UserJpaEntity entity = new UserJpaEntity();
        entity.setEmail("integration@test.com");
        entity.setNickname("IntegrationTest");
        entity.setBirthdate(LocalDate.of(2000, 1, 1));
        entity.setPasswordHash("hashed_password");
        entity.setRoles(List.of("PLAYER", "DUNGEON_MASTER"));

        // Act
        userRepository.save(entity);
        UserJpaEntity retrieved = userRepository.findByEmail("integration@test.com").orElse(null);

        // Assert
        assertNotNull(retrieved);
        assertNotNull(retrieved.getId()); // UUID should be generated
        assertEquals("integration@test.com", retrieved.getEmail());
        assertEquals("IntegrationTest", retrieved.getNickname());
        assertEquals("hashed_password", retrieved.getPasswordHash());
        assertEquals(2, retrieved.getRoles().size());
        assertTrue(retrieved.getRoles().contains("PLAYER"));
    }
}
