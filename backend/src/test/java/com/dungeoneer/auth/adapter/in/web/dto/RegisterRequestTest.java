package com.dungeoneer.auth.adapter.in.web.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should deserialize birthdate in pt-BR dd/MM/yyyy format")
    void shouldDeserializeBrazilianDateFormat() throws Exception {
        String json = """
                {
                    "email": "frodo@shire.com",
                    "nickname": "Ringbearer",
                    "birthdate": "22/09/1990",
                    "password": "mypassword123",
                    "roles": ["PLAYER", "DUNGEON_MASTER"]
                }
                """;

        RegisterRequest request = objectMapper.readValue(json, RegisterRequest.class);

        assertNotNull(request);
        assertEquals(LocalDate.of(1990, 9, 22), request.birthdate());
        assertEquals(2, request.roles().size());
    }

    @Test
    @DisplayName("Should deserialize birthdate in ISO yyyy-MM-dd format")
    void shouldDeserializeIsoDateFormat() throws Exception {
        String json = """
                {
                    "email": "sam@shire.com",
                    "nickname": "BraveSam",
                    "birthdate": "1991-04-10",
                    "password": "mypassword123"
                }
                """;

        RegisterRequest request = objectMapper.readValue(json, RegisterRequest.class);

        assertNotNull(request);
        assertEquals(LocalDate.of(1991, 4, 10), request.birthdate());
        assertNull(request.roles());
    }
}
