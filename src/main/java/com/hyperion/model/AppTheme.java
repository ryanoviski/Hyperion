package com.hyperion.model;

import java.util.Locale;

public enum AppTheme {
    DARK("dark", "Escuro"),
    LIGHT("light", "Claro");

    private final String storageValue;
    private final String displayName;

    AppTheme(String storageValue, String displayName) {
        this.storageValue = storageValue;
        this.displayName = displayName;
    }

    public String getStorageValue() {
        return storageValue;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static AppTheme fromStorageValue(String value) {
        if (value == null || value.isBlank()) {
            return DARK;
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        for (AppTheme theme : values()) {
            if (theme.storageValue.equals(normalizedValue)) {
                return theme;
            }
        }

        return DARK;
    }
}
