package com.hyperion.config;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionTest {

    public static void main(String[] args) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            System.out.println("SQLite connection successful.");
            System.out.println("Database file: " + DatabaseConfig.getDatabaseFile().toAbsolutePath());
            System.out.println("Connection valid: " + connection.isValid(2));
        } catch (SQLException exception) {
            throw new IllegalStateException("SQLite connection failed.", exception);
        }
    }
}
