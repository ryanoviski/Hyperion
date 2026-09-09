package com.hyperion.config;

import com.hyperion.exception.DatabaseInitializationException;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    public static final String DATA_DIRECTORY_PROPERTY = "hyperion.data.dir";
    private static final int SQLITE_BUSY_TIMEOUT_MILLISECONDS = 5_000;
    private static final Path LEGACY_DATABASE_FILE = Path.of("data", "hyperion.db").toAbsolutePath().normalize();

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        Path databaseFile = getDatabaseFile();
        createDatabaseDirectory(getDataDirectory());
        migrateLegacyDatabaseIfNecessary(databaseFile);
        loadDriver();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());

        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute("PRAGMA busy_timeout = " + SQLITE_BUSY_TIMEOUT_MILLISECONDS + ";");
            statement.execute("PRAGMA journal_mode = WAL;");
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }

        return connection;
    }

    public static Path getDatabaseFile() {
        return getDataDirectory().resolve("hyperion.db");
    }

    public static Path getDataDirectory() {
        return resolveDataDirectory();
    }

    private static void createDatabaseDirectory(Path databaseDirectory) {
        try {
            Files.createDirectories(databaseDirectory);
        } catch (IOException exception) {
            throw new DatabaseInitializationException("Não foi possível criar o diretório do banco de dados.", exception);
        }
    }

    private static void migrateLegacyDatabaseIfNecessary(Path databaseFile) {
        if (hasDataDirectoryOverride()
                || databaseFile.equals(LEGACY_DATABASE_FILE)
                || Files.exists(databaseFile)
                || !Files.isRegularFile(LEGACY_DATABASE_FILE)) {
            return;
        }

        try {
            Files.copy(LEGACY_DATABASE_FILE, databaseFile);
            copyLegacySidecarIfPresent(databaseFile, "-wal");
            copyLegacySidecarIfPresent(databaseFile, "-shm");
        } catch (IOException exception) {
            throw new DatabaseInitializationException("Não foi possível migrar os dados locais existentes.", exception);
        }
    }

    private static void copyLegacySidecarIfPresent(Path databaseFile, String suffix) throws IOException {
        Path legacySidecar = Path.of(LEGACY_DATABASE_FILE + suffix);
        if (Files.isRegularFile(legacySidecar)) {
            Files.copy(legacySidecar, Path.of(databaseFile + suffix));
        }
    }

    private static Path resolveDataDirectory() {
        String configuredDirectory = System.getProperty(DATA_DIRECTORY_PROPERTY);
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory).toAbsolutePath().normalize();
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Path.of(appData, "Hyperion");
        }

        return Path.of(System.getProperty("user.home"), ".hyperion");
    }

    private static boolean hasDataDirectoryOverride() {
        String configuredDirectory = System.getProperty(DATA_DIRECTORY_PROPERTY);
        return configuredDirectory != null && !configuredDirectory.isBlank();
    }

    private static void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new DatabaseInitializationException("O driver SQLite JDBC não foi encontrado.", exception);
        }
    }
}
