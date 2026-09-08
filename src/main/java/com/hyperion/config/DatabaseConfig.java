package com.hyperion.config;

import com.hyperion.exception.DatabaseInitializationException;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final Path LEGACY_DATABASE_FILE = Path.of("data", "hyperion.db").toAbsolutePath().normalize();
    private static final Path DATABASE_DIRECTORY = resolveDataDirectory();
    private static final Path DATABASE_FILE = DATABASE_DIRECTORY.resolve("hyperion.db");
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FILE.toAbsolutePath();

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        createDatabaseDirectory();
        migrateLegacyDatabaseIfNecessary();
        loadDriver();
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static Path getDatabaseFile() {
        return DATABASE_FILE;
    }

    public static Path getDataDirectory() {
        return DATABASE_DIRECTORY;
    }

    private static void createDatabaseDirectory() {
        try {
            Files.createDirectories(DATABASE_DIRECTORY);
        } catch (IOException exception) {
            throw new DatabaseInitializationException("Não foi possível criar o diretório do banco de dados.", exception);
        }
    }

    private static void migrateLegacyDatabaseIfNecessary() {
        if (DATABASE_FILE.equals(LEGACY_DATABASE_FILE) || Files.exists(DATABASE_FILE) || !Files.isRegularFile(LEGACY_DATABASE_FILE)) {
            return;
        }

        try {
            Files.copy(LEGACY_DATABASE_FILE, DATABASE_FILE);
            copyLegacySidecarIfPresent("-wal");
            copyLegacySidecarIfPresent("-shm");
        } catch (IOException exception) {
            throw new DatabaseInitializationException("Não foi possível migrar os dados locais existentes.", exception);
        }
    }

    private static void copyLegacySidecarIfPresent(String suffix) throws IOException {
        Path legacySidecar = Path.of(LEGACY_DATABASE_FILE + suffix);
        if (Files.isRegularFile(legacySidecar)) {
            Files.copy(legacySidecar, Path.of(DATABASE_FILE + suffix));
        }
    }

    private static Path resolveDataDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "Hyperion");
        }

        return Path.of(System.getProperty("user.home"), ".hyperion");
    }

    private static void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new DatabaseInitializationException("O driver SQLite JDBC não foi encontrado.", exception);
        }
    }
}
