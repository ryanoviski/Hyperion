package com.hyperion.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private static final String CREATE_COMPANY_TABLE = """
            CREATE TABLE IF NOT EXISTS company (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                owner_name TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_APP_SETTINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                first_run_completed INTEGER NOT NULL DEFAULT 0,
                pin_enabled INTEGER NOT NULL DEFAULT 0,
                pin_hash TEXT,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_CUSTOMERS_TABLE = """
            CREATE TABLE IF NOT EXISTS customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                document TEXT,
                phone TEXT,
                email TEXT,
                address TEXT,
                notes TEXT,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String CREATE_PRODUCTS_TABLE = """
            CREATE TABLE IF NOT EXISTS products (
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
            """;

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(CREATE_COMPANY_TABLE);
            statement.execute(CREATE_APP_SETTINGS_TABLE);
            statement.execute(CREATE_CUSTOMERS_TABLE);
            statement.execute(CREATE_PRODUCTS_TABLE);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize database.", exception);
        }
    }
}
