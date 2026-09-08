package com.hyperion.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseMigrationIntegrationTest {

    @TempDir
    Path testDirectory;

    @AfterEach
    void clearDataDirectoryOverride() {
        System.clearProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY);
    }

    @Test
    void migratesLegacyDecimalPricesToStrictIntegerCents() throws Exception {
        Path dataDirectory = testDirectory.resolve("data");
        Files.createDirectories(dataDirectory);
        Path legacyDatabase = dataDirectory.resolve("hyperion.db");

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + legacyDatabase.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        description TEXT,
                        price NUMERIC NOT NULL DEFAULT 0,
                        cost NUMERIC NOT NULL DEFAULT 0,
                        stock_quantity INTEGER NOT NULL DEFAULT 0,
                        category TEXT,
                        barcode TEXT,
                        supplier TEXT,
                        active INTEGER NOT NULL DEFAULT 1,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );
                    """);
            statement.executeUpdate("""
                    INSERT INTO products (name, price, cost, stock_quantity)
                    VALUES ('Produto legado', 12.34, 5.67, 3);
                    """);
        }

        System.setProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY, dataDirectory.toString());
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals("INTEGER", scalar(statement, "SELECT type FROM pragma_table_info('products') WHERE name = 'price';"));
            assertEquals("1234", scalar(statement, "SELECT price FROM products WHERE name = 'Produto legado';"));
            assertEquals("567", scalar(statement, "SELECT cost FROM products WHERE name = 'Produto legado';"));
            assertEquals("5", scalar(statement, "SELECT MAX(version) FROM schema_migrations;"));
            assertEquals("0", scalar(statement, "SELECT minimum_stock FROM products WHERE name = 'Produto legado';"));
            assertEquals("1", scalar(statement, "PRAGMA foreign_keys;"));
            assertThrows(Exception.class, () -> statement.executeUpdate("""
                    INSERT INTO products (name, price, cost, stock_quantity)
                    VALUES ('Preço inválido', -1, 0, 0);
                    """));
        }
    }

    private String scalar(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new AssertionError("Consulta não retornou resultado: " + sql);
            }
            return resultSet.getString(1);
        }
    }
}
