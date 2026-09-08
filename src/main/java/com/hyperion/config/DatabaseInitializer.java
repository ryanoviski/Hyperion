package com.hyperion.config;

import com.hyperion.exception.DatabaseInitializationException;

import java.sql.Connection;
import java.sql.SQLException;

/** Entry point for the versioned database migration chain. */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            DatabaseMigrations.migrate(connection);
        } catch (SQLException exception) {
            throw new DatabaseInitializationException("Não foi possível inicializar o banco de dados.", exception);
        }
    }
}
