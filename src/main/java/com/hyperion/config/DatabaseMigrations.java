package com.hyperion.config;

import com.hyperion.exception.DatabaseInitializationException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Versioned, transactional SQLite migrations. */
final class DatabaseMigrations {

    private static final int MAXIMUM_PRODUCT_QUANTITY = Integer.MAX_VALUE;

    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, "baseline-schema", false, DatabaseMigrations::createBaselineSchema),
            new Migration(2, "strict-integrity-and-money-in-cents", true, DatabaseMigrations::rebuildWithStrictSchema),
            new Migration(3, "query-indexes", false, DatabaseMigrations::createQueryIndexes),
            new Migration(4, "optional-unique-identifiers", false, DatabaseMigrations::createUniqueIdentifierIndexes),
            new Migration(5, "operational-security-and-credit-history", false, DatabaseMigrations::createOperationalSecurityAndCreditHistory),
            new Migration(6, "sale-item-cost-snapshots", false, DatabaseMigrations::addSaleItemCostSnapshots),
            new Migration(7, "sale-item-net-subtotal-snapshots", false, DatabaseMigrations::addSaleItemNetSubtotalSnapshots),
            new Migration(8, "sale-item-classification-snapshots", false, DatabaseMigrations::addSaleItemClassificationSnapshots),
            new Migration(9, "product-quantity-range", false, DatabaseMigrations::enforceProductQuantityRange)
    );

    private DatabaseMigrations() {
    }

    static void migrate(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) STRICT;
                """);

        for (Migration migration : MIGRATIONS) {
            if (!isApplied(connection, migration.version())) {
                apply(connection, migration);
            }
        }

        verifyForeignKeys(connection);
    }

    private static boolean isApplied(Connection connection, int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM schema_migrations WHERE version = ?")) {
            statement.setInt(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void apply(Connection connection, Migration migration) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        if (migration.disableForeignKeys()) {
            setForeignKeys(connection, false);
        }

        try {
            connection.setAutoCommit(false);
            migration.operation().apply(connection);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO schema_migrations (version, description)
                    VALUES (?, ?);
                    """)) {
                statement.setInt(1, migration.version());
                statement.setString(2, migration.description());
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
            if (migration.disableForeignKeys()) {
                setForeignKeys(connection, true);
            }
        }
    }

    /** Historical schema retained as the first migration for old installations. */
    private static void createBaselineSchema(Connection connection) throws SQLException {
        executeAll(connection,
                """
                        CREATE TABLE IF NOT EXISTS app_settings (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            first_run_completed INTEGER NOT NULL DEFAULT 0,
                            pin_enabled INTEGER NOT NULL DEFAULT 0,
                            pin_hash TEXT,
                            theme TEXT NOT NULL DEFAULT 'dark',
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        );
                        """,
                """
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
                        """,
                """
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
                        """,
                """
                        CREATE TABLE IF NOT EXISTS stock_movements (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            product_id INTEGER NOT NULL,
                            type TEXT NOT NULL,
                            quantity INTEGER NOT NULL,
                            notes TEXT,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (product_id) REFERENCES products(id)
                        );
                        """,
                """
                        CREATE TABLE IF NOT EXISTS sales (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            customer_id INTEGER NOT NULL,
                            customer_name TEXT NOT NULL,
                            subtotal NUMERIC NOT NULL DEFAULT 0,
                            discount NUMERIC NOT NULL DEFAULT 0,
                            total NUMERIC NOT NULL DEFAULT 0,
                            payment_method TEXT NOT NULL,
                            status TEXT NOT NULL DEFAULT 'COMPLETED',
                            cancelled_at TEXT,
                            cancellation_reason TEXT,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (customer_id) REFERENCES customers(id)
                        );
                        """,
                """
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
                        """,
                """
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
                            cancelled_at TEXT,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (sale_id) REFERENCES sales(id),
                            FOREIGN KEY (customer_id) REFERENCES customers(id)
                        );
                        """,
                """
                        CREATE TABLE IF NOT EXISTS expenses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            description TEXT NOT NULL,
                            category TEXT,
                            amount NUMERIC NOT NULL DEFAULT 0,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        );
                        """,
                """
                        CREATE TABLE IF NOT EXISTS attachments (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            module TEXT NOT NULL,
                            entity_id INTEGER NOT NULL,
                            original_name TEXT NOT NULL,
                            stored_name TEXT NOT NULL,
                            file_path TEXT NOT NULL,
                            content_type TEXT,
                            file_size INTEGER NOT NULL DEFAULT 0,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                        );
                        """);

        addColumnIfMissing(connection, "credit_installments", "paid_at", "ALTER TABLE credit_installments ADD COLUMN paid_at TEXT;");
        addColumnIfMissing(connection, "app_settings", "theme", "ALTER TABLE app_settings ADD COLUMN theme TEXT NOT NULL DEFAULT 'dark';");
        addColumnIfMissing(connection, "sales", "status", "ALTER TABLE sales ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED';");
        addColumnIfMissing(connection, "sales", "cancelled_at", "ALTER TABLE sales ADD COLUMN cancelled_at TEXT;");
        addColumnIfMissing(connection, "sales", "cancellation_reason", "ALTER TABLE sales ADD COLUMN cancellation_reason TEXT;");
        addColumnIfMissing(connection, "credit_installments", "cancelled_at", "ALTER TABLE credit_installments ADD COLUMN cancelled_at TEXT;");
    }

    /**
     * SQLite does not support adding CHECK constraints or changing types in
     * place. This rebuild keeps ids and converts legacy decimal amounts to
     * integer cents in one atomic migration.
     */
    private static void rebuildWithStrictSchema(Connection connection) throws SQLException {
        executeAll(connection,
                createAppSettingsTable(),
                createCustomersTable(),
                createProductsTable(),
                createSalesTable(),
                createStockMovementsTable(),
                createSaleItemsTable(),
                createCreditInstallmentsTable(),
                createExpensesTable(),
                createAttachmentsTable(),
                """
                        INSERT INTO app_settings_new (id, first_run_completed, pin_enabled, pin_hash, theme, created_at, updated_at)
                        SELECT id,
                               CASE WHEN first_run_completed = 1 THEN 1 ELSE 0 END,
                               CASE WHEN pin_enabled = 1 THEN 1 ELSE 0 END,
                               pin_hash,
                               CASE WHEN theme IN ('dark', 'light') THEN theme ELSE 'dark' END,
                               created_at,
                               updated_at
                        FROM app_settings;
                        """,
                """
                        INSERT INTO customers_new (id, name, document, phone, email, address, notes, active, created_at, updated_at)
                        SELECT id,
                               COALESCE(NULLIF(trim(name), ''), 'Cliente sem nome #' || id),
                               NULLIF(trim(document), ''), NULLIF(trim(phone), ''), NULLIF(trim(email), ''),
                               NULLIF(trim(address), ''), NULLIF(trim(notes), ''),
                               CASE WHEN active = 0 THEN 0 ELSE 1 END,
                               created_at, updated_at
                        FROM customers;
                        """,
                """
                        INSERT INTO products_new (
                            id, name, description, price, cost, stock_quantity, category, barcode, supplier, active, created_at, updated_at
                        )
                        SELECT id,
                               COALESCE(NULLIF(trim(name), ''), 'Produto sem nome #' || id),
                               NULLIF(trim(description), ''),
                               CAST(ROUND(MAX(COALESCE(price, 0), 0) * 100.0, 0) AS INTEGER),
                               CAST(ROUND(MAX(COALESCE(cost, 0), 0) * 100.0, 0) AS INTEGER),
                               MAX(COALESCE(stock_quantity, 0), 0),
                               NULLIF(trim(category), ''), NULLIF(trim(barcode), ''), NULLIF(trim(supplier), ''),
                               CASE WHEN active = 0 THEN 0 ELSE 1 END,
                               created_at, updated_at
                        FROM products;
                        """,
                """
                        INSERT INTO sales_new (
                            id, customer_id, customer_name, subtotal, discount, total, payment_method, status,
                            cancelled_at, cancellation_reason, created_at
                        )
                        SELECT id, customer_id,
                               COALESCE(NULLIF(trim(customer_name), ''), 'Cliente #' || customer_id),
                               CAST(ROUND(MAX(COALESCE(subtotal, 0), 0) * 100.0, 0) AS INTEGER),
                               CAST(ROUND(MIN(MAX(COALESCE(discount, 0), 0), MAX(COALESCE(subtotal, 0), 0)) * 100.0, 0) AS INTEGER),
                               CAST(ROUND(MAX(COALESCE(subtotal, 0), 0) * 100.0, 0) AS INTEGER)
                                   - CAST(ROUND(MIN(MAX(COALESCE(discount, 0), 0), MAX(COALESCE(subtotal, 0), 0)) * 100.0, 0) AS INTEGER),
                               CASE WHEN payment_method IN ('Dinheiro', 'PIX', 'Cartão crédito', 'Cartão débito', 'Crediário')
                                    THEN payment_method ELSE 'Dinheiro' END,
                               CASE WHEN status = 'CANCELLED' THEN 'CANCELLED' ELSE 'COMPLETED' END,
                               CASE WHEN status = 'CANCELLED' THEN COALESCE(cancelled_at, created_at) ELSE NULL END,
                               CASE WHEN status = 'CANCELLED' THEN cancellation_reason ELSE NULL END,
                               created_at
                        FROM sales;
                        """,
                """
                        INSERT INTO stock_movements_new (id, product_id, type, quantity, notes, created_at)
                        SELECT id, product_id, CASE WHEN type = 'OUT' THEN 'OUT' ELSE 'IN' END,
                               MAX(COALESCE(quantity, 0), 1), NULLIF(trim(notes), ''), created_at
                        FROM stock_movements;
                        """,
                """
                        INSERT INTO sale_items_new (id, sale_id, product_id, product_name, quantity, unit_price, subtotal)
                        SELECT id, sale_id, product_id,
                               COALESCE(NULLIF(trim(product_name), ''), 'Produto #' || product_id),
                               MAX(COALESCE(quantity, 0), 1),
                               CAST(ROUND(MAX(COALESCE(unit_price, 0), 0) * 100.0, 0) AS INTEGER),
                               MAX(COALESCE(quantity, 0), 1)
                                   * CAST(ROUND(MAX(COALESCE(unit_price, 0), 0) * 100.0, 0) AS INTEGER)
                        FROM sale_items;
                        """,
                """
                        INSERT INTO credit_installments_new (
                            id, sale_id, customer_id, customer_name, installment_number, total_installments,
                            amount, due_date, status, paid_at, cancelled_at, created_at
                        )
                        SELECT id, sale_id, customer_id,
                               COALESCE(NULLIF(trim(customer_name), ''), 'Cliente #' || customer_id),
                               MAX(COALESCE(installment_number, 0), 1),
                               MAX(MAX(COALESCE(total_installments, 0), 1), MAX(COALESCE(installment_number, 0), 1)),
                               MAX(CAST(ROUND(MAX(COALESCE(amount, 0), 0) * 100.0, 0) AS INTEGER), 1),
                               due_date,
                               CASE WHEN status IN ('PAID', 'CANCELLED') THEN status ELSE 'OPEN' END,
                               paid_at, cancelled_at, created_at
                        FROM credit_installments;
                        """,
                """
                        INSERT INTO expenses_new (id, description, category, amount, created_at)
                        SELECT id, COALESCE(NULLIF(trim(description), ''), 'Despesa sem descrição #' || id),
                               NULLIF(trim(category), ''),
                               MAX(CAST(ROUND(MAX(COALESCE(amount, 0), 0) * 100.0, 0) AS INTEGER), 1),
                               created_at
                        FROM expenses;
                        """,
                """
                        INSERT INTO attachments_new (
                            id, module, entity_id, original_name, stored_name, file_path, content_type, file_size, created_at
                        )
                        SELECT id, 'FINANCE', MAX(COALESCE(entity_id, 0), 1),
                               COALESCE(NULLIF(trim(original_name), ''), 'arquivo-' || id),
                               COALESCE(NULLIF(trim(stored_name), ''), 'arquivo-' || id),
                               COALESCE(NULLIF(trim(file_path), ''), 'anexo-indisponivel-' || id),
                               NULLIF(trim(content_type), ''), MAX(COALESCE(file_size, 0), 0), created_at
                        FROM attachments;
                        """);

        executeAll(connection,
                "DROP TABLE credit_installments;", "DROP TABLE sale_items;", "DROP TABLE stock_movements;",
                "DROP TABLE sales;", "DROP TABLE attachments;", "DROP TABLE expenses;", "DROP TABLE products;",
                "DROP TABLE customers;", "DROP TABLE app_settings;",
                "ALTER TABLE app_settings_new RENAME TO app_settings;",
                "ALTER TABLE customers_new RENAME TO customers;",
                "ALTER TABLE products_new RENAME TO products;",
                "ALTER TABLE sales_new RENAME TO sales;",
                "ALTER TABLE stock_movements_new RENAME TO stock_movements;",
                "ALTER TABLE sale_items_new RENAME TO sale_items;",
                "ALTER TABLE credit_installments_new RENAME TO credit_installments;",
                "ALTER TABLE expenses_new RENAME TO expenses;",
                "ALTER TABLE attachments_new RENAME TO attachments;");

        verifyForeignKeys(connection);
    }

    private static String createAppSettingsTable() {
        return """
                CREATE TABLE app_settings_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_run_completed INTEGER NOT NULL DEFAULT 0 CHECK (first_run_completed IN (0, 1)),
                    pin_enabled INTEGER NOT NULL DEFAULT 0 CHECK (pin_enabled IN (0, 1)),
                    pin_hash TEXT,
                    theme TEXT NOT NULL DEFAULT 'dark' CHECK (theme IN ('dark', 'light')),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) STRICT;
                """;
    }

    private static String createCustomersTable() {
        return """
                CREATE TABLE customers_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
                    document TEXT, phone TEXT, email TEXT, address TEXT, notes TEXT,
                    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) STRICT;
                """;
    }

    private static String createProductsTable() {
        return """
                CREATE TABLE products_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
                    description TEXT,
                    price INTEGER NOT NULL DEFAULT 0 CHECK (price >= 0),
                    cost INTEGER NOT NULL DEFAULT 0 CHECK (cost >= 0),
                    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
                    category TEXT, barcode TEXT, supplier TEXT,
                    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) STRICT;
                """;
    }

    private static String createSalesTable() {
        return """
                CREATE TABLE sales_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    customer_name TEXT NOT NULL CHECK (length(trim(customer_name)) > 0),
                    subtotal INTEGER NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
                    discount INTEGER NOT NULL DEFAULT 0 CHECK (discount >= 0 AND discount <= subtotal),
                    total INTEGER NOT NULL DEFAULT 0 CHECK (total >= 0 AND total = subtotal - discount),
                    payment_method TEXT NOT NULL CHECK (payment_method IN ('Dinheiro', 'PIX', 'Cartão crédito', 'Cartão débito', 'Crediário')),
                    status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'CANCELLED')),
                    cancelled_at TEXT, cancellation_reason TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES customers_new(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                ) STRICT;
                """;
    }

    private static String createStockMovementsTable() {
        return """
                CREATE TABLE stock_movements_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    type TEXT NOT NULL CHECK (type IN ('IN', 'OUT')),
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    notes TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (product_id) REFERENCES products_new(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                ) STRICT;
                """;
    }

    private static String createSaleItemsTable() {
        return """
                CREATE TABLE sale_items_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sale_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL CHECK (length(trim(product_name)) > 0),
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    unit_price INTEGER NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
                    subtotal INTEGER NOT NULL DEFAULT 0 CHECK (subtotal >= 0 AND subtotal = quantity * unit_price),
                    FOREIGN KEY (sale_id) REFERENCES sales_new(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                    FOREIGN KEY (product_id) REFERENCES products_new(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                ) STRICT;
                """;
    }

    private static String createCreditInstallmentsTable() {
        return """
                CREATE TABLE credit_installments_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sale_id INTEGER NOT NULL,
                    customer_id INTEGER NOT NULL,
                    customer_name TEXT NOT NULL CHECK (length(trim(customer_name)) > 0),
                    installment_number INTEGER NOT NULL CHECK (installment_number > 0),
                    total_installments INTEGER NOT NULL CHECK (total_installments > 0 AND installment_number <= total_installments),
                    amount INTEGER NOT NULL DEFAULT 0 CHECK (amount > 0),
                    due_date TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'PAID', 'CANCELLED')),
                    paid_at TEXT, cancelled_at TEXT,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (sale_id) REFERENCES sales_new(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                    FOREIGN KEY (customer_id) REFERENCES customers_new(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
                    UNIQUE (sale_id, installment_number)
                ) STRICT;
                """;
    }

    private static String createExpensesTable() {
        return """
                CREATE TABLE expenses_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    description TEXT NOT NULL CHECK (length(trim(description)) > 0),
                    category TEXT,
                    amount INTEGER NOT NULL DEFAULT 0 CHECK (amount > 0),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) STRICT;
                """;
    }

    private static String createAttachmentsTable() {
        return """
                CREATE TABLE attachments_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    module TEXT NOT NULL CHECK (module IN ('FINANCE')),
                    entity_id INTEGER NOT NULL CHECK (entity_id > 0),
                    original_name TEXT NOT NULL CHECK (length(trim(original_name)) > 0),
                    stored_name TEXT NOT NULL CHECK (length(trim(stored_name)) > 0),
                    file_path TEXT NOT NULL CHECK (length(trim(file_path)) > 0),
                    content_type TEXT,
                    file_size INTEGER NOT NULL DEFAULT 0 CHECK (file_size >= 0),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) STRICT;
                """;
    }

    private static void createQueryIndexes(Connection connection) throws SQLException {
        executeAll(connection,
                "CREATE INDEX IF NOT EXISTS idx_customers_active_name ON customers(active, name);",
                "CREATE INDEX IF NOT EXISTS idx_products_active_name ON products(active, name);",
                "CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);",
                "CREATE INDEX IF NOT EXISTS idx_stock_movements_product_created_at ON stock_movements(product_id, created_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_sales_customer_created_at ON sales(customer_id, created_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_sales_status_created_at ON sales(status, created_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_sales_payment_status_created_at ON sales(payment_method, status, created_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items(sale_id);",
                "CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items(product_id);",
                "CREATE INDEX IF NOT EXISTS idx_credit_installments_status_due_date ON credit_installments(status, due_date, id);",
                "CREATE INDEX IF NOT EXISTS idx_credit_installments_customer_status_due_date ON credit_installments(customer_id, status, due_date);",
                "CREATE INDEX IF NOT EXISTS idx_credit_installments_sale_id ON credit_installments(sale_id);",
                "CREATE INDEX IF NOT EXISTS idx_expenses_created_at ON expenses(created_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_expenses_category_created_at ON expenses(category, created_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_attachments_module_entity_created_at ON attachments(module, entity_id, created_at DESC);");
    }

    private static void createUniqueIdentifierIndexes(Connection connection) throws SQLException {
        failWhenDuplicateIdentifier(connection, "customers", "document", "documento de cliente");
        failWhenDuplicateIdentifier(connection, "products", "barcode", "código de barras");
        executeAll(connection,
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_customers_document ON customers(document COLLATE NOCASE) WHERE document IS NOT NULL AND trim(document) <> '';",
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_products_barcode ON products(barcode COLLATE NOCASE) WHERE barcode IS NOT NULL AND trim(barcode) <> ''; ");
    }

    private static void createOperationalSecurityAndCreditHistory(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "app_settings", "failed_pin_attempts",
                "ALTER TABLE app_settings ADD COLUMN failed_pin_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_pin_attempts >= 0);");
        addColumnIfMissing(connection, "app_settings", "pin_locked_until",
                "ALTER TABLE app_settings ADD COLUMN pin_locked_until TEXT;");
        addColumnIfMissing(connection, "products", "minimum_stock",
                "ALTER TABLE products ADD COLUMN minimum_stock INTEGER NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0);");

        executeAll(connection,
                """
                        CREATE TABLE IF NOT EXISTS pin_attempts (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            attempted_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            failure_count INTEGER NOT NULL CHECK (failure_count > 0),
                            locked_until TEXT NOT NULL
                        ) STRICT;
                        """,
                """
                        CREATE TABLE IF NOT EXISTS credit_payments (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            installment_id INTEGER NOT NULL UNIQUE,
                            amount INTEGER NOT NULL CHECK (amount > 0),
                            received_by TEXT NOT NULL CHECK (length(trim(received_by)) > 0),
                            payment_method TEXT NOT NULL CHECK (payment_method IN ('Dinheiro', 'PIX', 'Cartão crédito', 'Cartão débito')),
                            notes TEXT,
                            received_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (installment_id) REFERENCES credit_installments(id) ON UPDATE RESTRICT ON DELETE RESTRICT
                        ) STRICT;
                        """,
                "CREATE INDEX IF NOT EXISTS idx_pin_attempts_attempted_at ON pin_attempts(attempted_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_credit_payments_received_at ON credit_payments(received_at DESC);",
                "CREATE INDEX IF NOT EXISTS idx_products_stock_minimum ON products(active, stock_quantity, minimum_stock);");
    }

    private static void addSaleItemCostSnapshots(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "sale_items", "unit_cost",
                "ALTER TABLE sale_items ADD COLUMN unit_cost INTEGER NOT NULL DEFAULT 0 CHECK (unit_cost >= 0);");
        execute(connection, """
                UPDATE sale_items
                SET unit_cost = COALESCE((
                    SELECT cost
                    FROM products
                    WHERE products.id = sale_items.product_id
                ), 0)
                WHERE unit_cost = 0;
                """);
    }

    /**
     * Stores each item's exact share of a sale-level discount. Reports can
     * then reconcile their product totals with the sale total without relying
     * on the product's price at query time.
     */
    private static void addSaleItemNetSubtotalSnapshots(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "sale_items", "net_subtotal",
                "ALTER TABLE sale_items ADD COLUMN net_subtotal INTEGER NOT NULL DEFAULT 0 CHECK (net_subtotal >= 0 AND net_subtotal <= subtotal);");

        String salesSql = "SELECT id, total FROM sales ORDER BY id;";
        String itemsSql = "SELECT id, subtotal FROM sale_items WHERE sale_id = ? ORDER BY id;";
        String updateSql = "UPDATE sale_items SET net_subtotal = ? WHERE id = ?;";

        try (PreparedStatement salesStatement = connection.prepareStatement(salesSql);
             PreparedStatement itemsStatement = connection.prepareStatement(itemsSql);
             PreparedStatement updateStatement = connection.prepareStatement(updateSql);
             ResultSet sales = salesStatement.executeQuery()) {
            while (sales.next()) {
                long saleId = sales.getLong("id");
                long saleTotal = sales.getLong("total");
                List<SaleItemAmount> items = new ArrayList<>();
                itemsStatement.setLong(1, saleId);
                try (ResultSet resultSet = itemsStatement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(new SaleItemAmount(resultSet.getLong("id"), resultSet.getLong("subtotal")));
                    }
                }

                List<Long> netSubtotals = allocateNetSubtotals(saleId, saleTotal, items);
                for (int index = 0; index < items.size(); index++) {
                    updateStatement.setLong(1, netSubtotals.get(index));
                    updateStatement.setLong(2, items.get(index).id());
                    updateStatement.addBatch();
                }
            }
            updateStatement.executeBatch();
        }
    }

    private static void addSaleItemClassificationSnapshots(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "sale_items", "product_category",
                "ALTER TABLE sale_items ADD COLUMN product_category TEXT;");
        addColumnIfMissing(connection, "sale_items", "product_supplier",
                "ALTER TABLE sale_items ADD COLUMN product_supplier TEXT;");
        execute(connection, """
                UPDATE sale_items
                SET product_category = (
                    SELECT category FROM products WHERE products.id = sale_items.product_id
                ),
                    product_supplier = (
                    SELECT supplier FROM products WHERE products.id = sale_items.product_id
                )
                WHERE product_category IS NULL
                   OR product_supplier IS NULL;
                """);
        executeAll(connection,
                "CREATE INDEX IF NOT EXISTS idx_sale_items_category ON sale_items(product_category);",
                "CREATE INDEX IF NOT EXISTS idx_sale_items_supplier ON sale_items(product_supplier);");
    }

    private static void enforceProductQuantityRange(Connection connection) throws SQLException {
        String invalidQuantitySql = """
                SELECT id
                FROM products
                WHERE stock_quantity > ?
                   OR minimum_stock > ?
                LIMIT 1;
                """;
        try (PreparedStatement statement = connection.prepareStatement(invalidQuantitySql)) {
            statement.setInt(1, MAXIMUM_PRODUCT_QUANTITY);
            statement.setInt(2, MAXIMUM_PRODUCT_QUANTITY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new DatabaseInitializationException(
                            "O produto " + resultSet.getLong("id")
                                    + " possui uma quantidade acima do limite suportado.",
                            null
                    );
                }
            }
        }

        String limit = Integer.toString(MAXIMUM_PRODUCT_QUANTITY);
        executeAll(connection,
                """
                        CREATE TRIGGER IF NOT EXISTS trg_products_quantity_range_insert
                        BEFORE INSERT ON products
                        WHEN NEW.stock_quantity > %s OR NEW.minimum_stock > %s
                        BEGIN
                            SELECT RAISE(ABORT, 'A quantidade de estoque excede o limite suportado.');
                        END;
                        """.formatted(limit, limit),
                """
                        CREATE TRIGGER IF NOT EXISTS trg_products_quantity_range_update
                        BEFORE UPDATE OF stock_quantity, minimum_stock ON products
                        WHEN NEW.stock_quantity > %s OR NEW.minimum_stock > %s
                        BEGIN
                            SELECT RAISE(ABORT, 'A quantidade de estoque excede o limite suportado.');
                        END;
                        """.formatted(limit, limit));
    }

    private static List<Long> allocateNetSubtotals(long saleId, long saleTotal, List<SaleItemAmount> items) {
        if (saleTotal < 0) {
            throw invalidSaleForNetSubtotalMigration(saleId);
        }

        long grossTotal = 0;
        for (SaleItemAmount item : items) {
            if (item.subtotal() < 0) {
                throw invalidSaleForNetSubtotalMigration(saleId);
            }
            try {
                grossTotal = Math.addExact(grossTotal, item.subtotal());
            } catch (ArithmeticException exception) {
                throw invalidSaleForNetSubtotalMigration(saleId);
            }
        }
        if ((items.isEmpty() && saleTotal != 0) || saleTotal > grossTotal) {
            throw invalidSaleForNetSubtotalMigration(saleId);
        }
        if (items.isEmpty()) {
            return List.of();
        }
        if (grossTotal == 0) {
            return java.util.Collections.nCopies(items.size(), 0L);
        }

        BigInteger divisor = BigInteger.valueOf(grossTotal);
        BigInteger total = BigInteger.valueOf(saleTotal);
        List<NetSubtotalAllocation> allocations = new ArrayList<>(items.size());
        long allocatedCents = 0;
        for (int index = 0; index < items.size(); index++) {
            BigInteger dividend = BigInteger.valueOf(items.get(index).subtotal()).multiply(total);
            BigInteger[] division = dividend.divideAndRemainder(divisor);
            long allocated = division[0].longValueExact();
            allocations.add(new NetSubtotalAllocation(index, allocated, division[1]));
            allocatedCents = Math.addExact(allocatedCents, allocated);
        }

        long remainingCents = saleTotal - allocatedCents;
        allocations.sort(Comparator.comparing(NetSubtotalAllocation::remainder).reversed()
                .thenComparing(NetSubtotalAllocation::index));
        for (int index = 0; index < remainingCents; index++) {
            NetSubtotalAllocation allocation = allocations.get(index);
            allocations.set(index, allocation.withAmount(Math.addExact(allocation.amount(), 1)));
        }

        Long[] values = new Long[items.size()];
        for (NetSubtotalAllocation allocation : allocations) {
            values[allocation.index()] = allocation.amount();
        }
        return List.of(values);
    }

    private static DatabaseInitializationException invalidSaleForNetSubtotalMigration(long saleId) {
        return new DatabaseInitializationException(
                "A venda " + saleId + " possui itens incompatíveis com o total e não pode receber o rateio de desconto.",
                null
        );
    }

    private static void failWhenDuplicateIdentifier(Connection connection, String table, String column, String label) throws SQLException {
        String sql = """
                SELECT %s FROM %s
                WHERE %s IS NOT NULL AND trim(%s) <> ''
                GROUP BY %s COLLATE NOCASE
                HAVING COUNT(*) > 1
                LIMIT 1;
                """.formatted(column, table, column, column, column);
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                throw new DatabaseInitializationException(
                        "A migração não pôde criar a unicidade de " + label
                                + ": existe valor duplicado '" + resultSet.getString(1) + "'.",
                        null
                );
            }
        }
    }

    private static void verifyForeignKeys(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_check;")) {
            if (resultSet.next()) {
                throw new DatabaseInitializationException(
                        "O banco de dados contém uma referência inválida na tabela " + resultSet.getString("table") + ".",
                        null
                );
            }
        }
    }

    private static void setForeignKeys(Connection connection, boolean enabled) throws SQLException {
        execute(connection, "PRAGMA foreign_keys = " + (enabled ? "ON" : "OFF") + ";");
    }

    private static void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String alterTableSql
    ) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ");")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        execute(connection, alterTableSql);
    }

    private static void executeAll(Connection connection, String... sqlStatements) throws SQLException {
        for (String sql : sqlStatements) {
            execute(connection, sql);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record Migration(int version, String description, boolean disableForeignKeys, SqlOperation operation) {
    }

    private record SaleItemAmount(long id, long subtotal) {
    }

    private record NetSubtotalAllocation(int index, long amount, BigInteger remainder) {
        private NetSubtotalAllocation withAmount(long value) {
            return new NetSubtotalAllocation(index, value, remainder);
        }
    }

    @FunctionalInterface
    private interface SqlOperation {
        void apply(Connection connection) throws SQLException;
    }
}
