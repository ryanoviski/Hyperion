package com.hyperion.config;

import java.sql.Connection;
import java.sql.ResultSet;
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

    private static final String CREATE_STOCK_MOVEMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS stock_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                notes TEXT,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES products(id)
            );
            """;

    private static final String CREATE_SALES_TABLE = """
            CREATE TABLE IF NOT EXISTS sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                customer_name TEXT NOT NULL,
                subtotal NUMERIC NOT NULL DEFAULT 0,
                discount NUMERIC NOT NULL DEFAULT 0,
                total NUMERIC NOT NULL DEFAULT 0,
                payment_method TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (customer_id) REFERENCES customers(id)
            );
            """;

    private static final String CREATE_SALE_ITEMS_TABLE = """
            CREATE TABLE IF NOT EXISTS sale_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                product_id INTEGER NOT NULL,
                product_name TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                unit_price NUMERIC NOT NULL DEFAULT 0,
                subtotal NUMERIC NOT NULL DEFAULT 0,
                FOREIGN KEY (sale_id) REFERENCES sales(id),
                FOREIGN KEY (product_id) REFERENCES products(id)
            );
            """;

    private static final String CREATE_CREDIT_INSTALLMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS credit_installments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id INTEGER NOT NULL,
                customer_id INTEGER NOT NULL,
                customer_name TEXT NOT NULL,
                installment_number INTEGER NOT NULL,
                total_installments INTEGER NOT NULL,
                amount NUMERIC NOT NULL DEFAULT 0,
                due_date TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'OPEN',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (sale_id) REFERENCES sales(id),
                FOREIGN KEY (customer_id) REFERENCES customers(id)
            );
            """;

    private static final String CREATE_EXPENSES_TABLE = """
            CREATE TABLE IF NOT EXISTS expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                category TEXT,
                amount NUMERIC NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;

    private static final String ADD_CREDIT_INSTALLMENTS_PAID_AT_COLUMN = """
            ALTER TABLE credit_installments
            ADD COLUMN paid_at TEXT;
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
            statement.execute(CREATE_STOCK_MOVEMENTS_TABLE);
            statement.execute(CREATE_SALES_TABLE);
            statement.execute(CREATE_SALE_ITEMS_TABLE);
            statement.execute(CREATE_CREDIT_INSTALLMENTS_TABLE);
            statement.execute(CREATE_EXPENSES_TABLE);

            addColumnIfMissing(
                    connection,
                    "credit_installments",
                    "paid_at",
                    ADD_CREDIT_INSTALLMENTS_PAID_AT_COLUMN
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize database.", exception);
        }
    }

    private static void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String alterTableSql
    ) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ");";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(alterTableSql);
        }
    }
}
