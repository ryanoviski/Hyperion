package com.hyperion.config;

import com.hyperion.model.Customer;
import com.hyperion.model.Product;
import com.hyperion.model.SaleItem;
import com.hyperion.service.CustomerService;
import com.hyperion.service.ProductService;
import com.hyperion.service.SaleService;
import com.hyperion.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.List;

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
            assertEquals("9", scalar(statement, "SELECT MAX(version) FROM schema_migrations;"));
            assertEquals("0", scalar(statement, "SELECT minimum_stock FROM products WHERE name = 'Produto legado';"));
            assertEquals("INTEGER", scalar(statement, "SELECT type FROM pragma_table_info('sale_items') WHERE name = 'unit_cost';"));
            assertEquals("INTEGER", scalar(statement, "SELECT type FROM pragma_table_info('sale_items') WHERE name = 'net_subtotal';"));
            assertEquals("TEXT", scalar(statement, "SELECT type FROM pragma_table_info('sale_items') WHERE name = 'product_category';"));
            assertEquals("TEXT", scalar(statement, "SELECT type FROM pragma_table_info('sale_items') WHERE name = 'product_supplier';"));
            assertEquals("1", scalar(statement, "PRAGMA foreign_keys;"));
            assertEquals("5000", scalar(statement, "PRAGMA busy_timeout;"));
            assertEquals("wal", scalar(statement, "PRAGMA journal_mode;"));
            assertThrows(Exception.class, () -> statement.executeUpdate("""
                    INSERT INTO products (name, price, cost, stock_quantity)
                    VALUES ('Preço inválido', -1, 0, 0);
                    """));
            assertThrows(Exception.class, () -> statement.executeUpdate("""
                    UPDATE products
                    SET stock_quantity = 2147483648
                    WHERE name = 'Produto legado';
                    """));
        }
    }

    @Test
    void backfillsNetTotalsAndItemClassificationSnapshotsForExistingSales() throws Exception {
        Path dataDirectory = testDirectory.resolve("data");
        System.setProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY, dataDirectory.toString());
        DatabaseInitializer.initialize();

        CustomerService customerService = new CustomerService();
        customerService.createCustomer("Cliente", "", "", "", "", "");
        Customer customer = customerService.searchActiveCustomers("Cliente").getFirst();
        ProductService productService = new ProductService();
        productService.createProduct("Produto", "", new BigDecimal("20.00"), new BigDecimal("5.00"), "Higiene", "", "Fornecedor A");
        Product product = productService.searchActiveProducts("Produto").getFirst();
        new StockService().registerEntry(product.getId(), 1, "Estoque inicial");
        new SaleService().finishSale(
                customer,
                List.of(new SaleItem(product.getId(), product.getName(), 1, product.getPrice())),
                new BigDecimal("1.00"),
                "PIX"
        );

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE sale_items SET net_subtotal = 0, product_category = NULL, product_supplier = NULL;");
            statement.executeUpdate("DELETE FROM schema_migrations WHERE version IN (7, 8);");
        }

        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            assertEquals("1900", scalar(statement, "SELECT net_subtotal FROM sale_items;"));
            assertEquals("Higiene", scalar(statement, "SELECT product_category FROM sale_items;"));
            assertEquals("Fornecedor A", scalar(statement, "SELECT product_supplier FROM sale_items;"));
            assertEquals("9", scalar(statement, "SELECT MAX(version) FROM schema_migrations;"));
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
