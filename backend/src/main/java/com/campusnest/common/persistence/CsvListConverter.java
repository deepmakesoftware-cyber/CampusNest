package com.campusnest.common.persistence;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores a short ordered list of codes as comma-separated text, e.g. ["DOUBLE","SINGLE"] <-> "DOUBLE,SINGLE".
 * Only for small lists of simple codes (no commas inside values), which is all we use it for.
 */
@Converter
public class CsvListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        return values == null ? "" : String.join(",", values);
    }

    @Override
    public List<String> convertToEntityAttribute(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
