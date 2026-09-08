package com.hyperion.util;

import com.hyperion.exception.DataCorruptionException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Converts SQLite UTC timestamps to the local time shown to the operator. */
public final class SqliteTimestamp {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SqliteTimestamp() {
    }

    public static LocalDateTime toLocalDateTime(String value, String fieldName) {
        try {
            LocalDateTime utcValue = LocalDateTime.parse(value, FORMATTER);
            return utcValue.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new DataCorruptionException("Há uma data inválida no campo " + fieldName + ".", exception);
        }
    }
}
