package com.dungeoneer.auth.adapter.in.web.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final DateTimeFormatter BR_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        text = text.trim();

        if (text.contains("/")) {
            try {
                return LocalDate.parse(text, BR_FORMATTER);
            } catch (DateTimeParseException ignored) {
                // fall through to ISO attempt
            }
        }

        try {
            return LocalDate.parse(text, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            throw context.instantiationException(
                    LocalDate.class,
                    "Invalid date format: '" + text + "'. Expected dd/MM/yyyy or yyyy-MM-dd."
            );
        }
    }
}
