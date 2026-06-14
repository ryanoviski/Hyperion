package com.hyperion.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final Path DATABASE_DIRECTORY = Path.of("data");
    private static final Path DATABASE_FILE = DATABASE_DIRECTORY.resolve("hyperion.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        createDatabaseDirectory();
        loadDriver();
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static Path getDatabaseFile() {
        return DATABASE_FILE;
    }

    private static void createDatabaseDirectory() {
        try {
            Files.createDirectories(DATABASE_DIRECTORY);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create database directory.", exception);
        }
    }

    private static void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("SQLite JDBC driver was not found.", exception);
        }
    }
}
