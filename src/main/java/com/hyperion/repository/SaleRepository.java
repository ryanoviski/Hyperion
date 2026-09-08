package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.DailySalesSummary;
import com.hyperion.model.PaymentMethodReport;
import com.hyperion.model.ProductSalesReport;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;
import com.hyperion.model.SalesReportSummary;
import com.hyperion.model.SalesReportFilter;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.InvalidDateRangeException;
import com.hyperion.exception.InvalidLimitException;
import com.hyperion.exception.EntityNotFoundException;
import com.hyperion.exception.SaleAlreadyCancelledException;
import com.hyperion.exception.SaleCancellationNotAllowedException;
import com.hyperion.exception.PersistenceException;
import com.hyperion.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaleRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void save(Sale sale, CreditSalePlan creditSalePlan) {
        String insertSaleSql = """
                INSERT INTO sales (customer_id, customer_name, subtotal, discount, total, payment_method)
                VALUES (?, ?, ?, ?, ?, ?);
                """;

        String insertSaleItemSql = """
                INSERT INTO sale_items (sale_id, product_id, product_name, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?);
                """;

        String updateStockSql = """
                UPDATE products
                SET stock_quantity = stock_quantity - ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND stock_quantity >= ?;
                """;

        String insertStockMovementSql = """
                INSERT INTO stock_movements (product_id, type, quantity, notes)
                VALUES (?, 'OUT', ?, ?);
                """;

        String insertInstallmentSql = """
                INSERT INTO credit_installments (
                    sale_id,
                    customer_id,
                    customer_name,
                    installment_number,
                    total_installments,
                    amount,
                    due_date
                )
                VALUES (?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement saleStatement = connection.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement itemStatement = connection.prepareStatement(insertSaleItemSql);
                 PreparedStatement stockStatement = connection.prepareStatement(updateStockSql);
                 PreparedStatement movementStatement = connection.prepareStatement(insertStockMovementSql);
                 PreparedStatement installmentStatement = connection.prepareStatement(insertInstallmentSql)) {

                saleStatement.setLong(1, sale.getCustomerId());
                saleStatement.setString(2, sale.getCustomerName());
                Money.setCents(saleStatement, 3, sale.getSubtotal());
                Money.setCents(saleStatement, 4, sale.getDiscount());
                Money.setCents(saleStatement, 5, sale.getTotal());
                saleStatement.setString(6, sale.getPaymentMethod());
                saleStatement.executeUpdate();

                Long saleId = readGeneratedId(saleStatement);

                for (SaleItem item : sale.getItems()) {
                    itemStatement.setLong(1, saleId);
                    itemStatement.setLong(2, item.getProductId());
                    itemStatement.setString(3, item.getProductName());
                    itemStatement.setInt(4, item.getQuantity());
                    Money.setCents(itemStatement, 5, item.getUnitPrice());
                    Money.setCents(itemStatement, 6, item.getSubtotal());
                    itemStatement.addBatch();

                    stockStatement.setInt(1, item.getQuantity());
                    stockStatement.setLong(2, item.getProductId());
                    stockStatement.setInt(3, item.getQuantity());
                    stockStatement.addBatch();

                    movementStatement.setLong(1, item.getProductId());
                    movementStatement.setInt(2, item.getQuantity());
                    movementStatement.setString(3, "Venda #" + saleId);
                    movementStatement.addBatch();
                }

                itemStatement.executeBatch();
                int[] stockResults = stockStatement.executeBatch();
                for (int index = 0; index < stockResults.length; index++) {
                    if (stockResults[index] == 0 || stockResults[index] == Statement.EXECUTE_FAILED) {
                        throw new InsufficientStockException(sale.getItems().get(index).getProductName());
                    }
                }
                movementStatement.executeBatch();

                if (creditSalePlan != null) {
                    addInstallmentBatch(installmentStatement, sale, saleId, creditSalePlan);
                    installmentStatement.executeBatch();
                }

                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível salvar a venda.", exception);
        }
    }

    public void cancel(Long saleId, String reason) {
        String findSaleSql = "SELECT status FROM sales WHERE id = ?;";
        String paidInstallmentsSql = "SELECT COUNT(*) FROM credit_installments WHERE sale_id = ? AND status = 'PAID';";
        String saleItemsSql = "SELECT id, sale_id, product_id, product_name, quantity, unit_price, subtotal FROM sale_items WHERE sale_id = ?;";
        String cancelSaleSql = """
                UPDATE sales
                SET status = 'CANCELLED',
                    cancelled_at = CURRENT_TIMESTAMP,
                    cancellation_reason = ?
                WHERE id = ?
                  AND status = 'COMPLETED';
                """;
        String cancelInstallmentsSql = """
                UPDATE credit_installments
                SET status = 'CANCELLED',
                    cancelled_at = CURRENT_TIMESTAMP
                WHERE sale_id = ?
                  AND status = 'OPEN';
                """;
        String restoreStockSql = """
                UPDATE products
                SET stock_quantity = stock_quantity + ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;
        String movementSql = """
                INSERT INTO stock_movements (product_id, type, quantity, notes)
                VALUES (?, 'IN', ?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String status = findSaleStatus(connection, findSaleSql, saleId);
                if ("CANCELLED".equals(status)) {
                    throw new SaleAlreadyCancelledException();
                }
                if (hasPaidInstallments(connection, paidInstallmentsSql, saleId)) {
                    throw new SaleCancellationNotAllowedException();
                }

                List<SaleItem> items = findSaleItems(connection, saleItemsSql, saleId);
                if (items.isEmpty()) {
                    throw new PersistenceException("A venda não possui itens para estornar.");
                }

                try (PreparedStatement cancelSaleStatement = connection.prepareStatement(cancelSaleSql);
                     PreparedStatement cancelInstallmentsStatement = connection.prepareStatement(cancelInstallmentsSql);
                     PreparedStatement restoreStockStatement = connection.prepareStatement(restoreStockSql);
                     PreparedStatement movementStatement = connection.prepareStatement(movementSql)) {

                    cancelSaleStatement.setString(1, reason);
                    cancelSaleStatement.setLong(2, saleId);
                    if (cancelSaleStatement.executeUpdate() != 1) {
                        throw new SaleAlreadyCancelledException();
                    }

                    cancelInstallmentsStatement.setLong(1, saleId);
                    cancelInstallmentsStatement.executeUpdate();

                    for (SaleItem item : items) {
                        restoreStockStatement.setInt(1, item.getQuantity());
                        restoreStockStatement.setLong(2, item.getProductId());
                        if (restoreStockStatement.executeUpdate() != 1) {
                            throw new EntityNotFoundException("Produto da venda");
                        }

                        movementStatement.setLong(1, item.getProductId());
                        movementStatement.setInt(2, item.getQuantity());
                        movementStatement.setString(3, "Cancelamento da venda #" + saleId + ": " + reason);
                        movementStatement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível cancelar a venda.", exception);
        }
    }

    private void addInstallmentBatch(
            PreparedStatement statement,
            Sale sale,
            Long saleId,
            CreditSalePlan creditSalePlan
    ) throws SQLException {
        int installments = creditSalePlan.getInstallments();
        BigDecimal installmentAmount = sale.getTotal().divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
        BigDecimal allocatedAmount = BigDecimal.ZERO;

        for (int installmentNumber = 1; installmentNumber <= installments; installmentNumber++) {
            BigDecimal amount = installmentAmount;

            if (installmentNumber == installments) {
                amount = sale.getTotal().subtract(allocatedAmount);
            }

            LocalDate dueDate = creditSalePlan.getFirstDueDate().plusMonths(installmentNumber - 1L);

            statement.setLong(1, saleId);
            statement.setLong(2, sale.getCustomerId());
            statement.setString(3, sale.getCustomerName());
            statement.setInt(4, installmentNumber);
            statement.setInt(5, installments);
            Money.setCents(statement, 6, amount);
            statement.setString(7, dueDate.toString());
            statement.addBatch();

            allocatedAmount = allocatedAmount.add(amount);
        }
    }

    public DailySalesSummary findTodaySummary() {
        String sql = """
                SELECT COUNT(*) AS sales_count,
                       COALESCE(SUM(total), 0) AS total
                FROM sales
                WHERE status = 'COMPLETED'
                  AND DATE(created_at) = DATE('now', 'localtime');
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return new DailySalesSummary(BigDecimal.ZERO, 0);
            }

            return new DailySalesSummary(
                    Money.getCents(resultSet, "total"),
                    resultSet.getInt("sales_count")
            );
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o resumo de vendas do dia.", exception);
        }
    }

    public BigDecimal getTotalSales() {
        String sql = """
                SELECT COALESCE(SUM(total), 0) AS total
                FROM sales
                WHERE status = 'COMPLETED';
                """;

        return queryTotal(sql);
    }

    public BigDecimal getCurrentMonthSales() {
        String sql = """
                SELECT COALESCE(SUM(total), 0) AS total
                FROM sales
                WHERE status = 'COMPLETED'
                  AND strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now', 'localtime');
                """;

        return queryTotal(sql);
    }

    public BigDecimal getTotalImmediateSales() {
        String sql = """
                SELECT COALESCE(SUM(total), 0) AS total
                FROM sales
                WHERE status = 'COMPLETED'
                  AND payment_method NOT IN ('Crediário', 'Crediario');
                """;

        return queryTotal(sql);
    }

    public BigDecimal getCurrentMonthImmediateSales() {
        String sql = """
                SELECT COALESCE(SUM(total), 0) AS total
                FROM sales
                WHERE status = 'COMPLETED'
                  AND payment_method NOT IN ('Crediário', 'Crediario')
                  AND strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now', 'localtime');
                """;

        return queryTotal(sql);
    }

    public List<Sale> findByCustomerId(Long customerId) {
        String sql = """
                SELECT id,
                       customer_id,
                       customer_name,
                       subtotal,
                       discount,
                       total,
                       payment_method,
                       status,
                       cancelled_at,
                       cancellation_reason,
                       created_at
                FROM sales
                WHERE customer_id = ?
                ORDER BY created_at DESC, id DESC;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Sale> sales = new ArrayList<>();

                while (resultSet.next()) {
                    sales.add(mapSale(resultSet));
                }

                return sales;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível listar as compras do cliente.", exception);
        }
    }

    public List<Sale> findLatest(int limit) {
        if (limit <= 0) {
            throw new InvalidLimitException();
        }
        String sql = """
                SELECT id,
                       customer_id,
                       customer_name,
                       subtotal,
                       discount,
                       total,
                       payment_method,
                       status,
                       cancelled_at,
                       cancellation_reason,
                       created_at
                FROM sales
                WHERE status = 'COMPLETED'
                ORDER BY created_at DESC, id DESC
                LIMIT ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Sale> sales = new ArrayList<>();

                while (resultSet.next()) {
                    sales.add(mapSale(resultSet));
                }

                return sales;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível listar as vendas recentes.", exception);
        }
    }

    public List<Sale> findLatestIncludingCancelled(int limit) {
        if (limit <= 0) {
            throw new InvalidLimitException();
        }

        String sql = """
                SELECT id, customer_id, customer_name, subtotal, discount, total, payment_method,
                       status, cancelled_at, cancellation_reason, created_at
                FROM sales
                ORDER BY created_at DESC, id DESC
                LIMIT ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Sale> sales = new ArrayList<>();
                while (resultSet.next()) {
                    sales.add(mapSale(resultSet));
                }
                return sales;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível listar as vendas.", exception);
        }
    }

    public SalesReportSummary getSalesReportSummary() {
        return getSalesReportSummary(null, null);
    }

    public SalesReportSummary getSalesReportSummary(LocalDate startDate, LocalDate endDateExclusive) {
        String sql = """
                SELECT COUNT(*) AS sales_count,
                       COALESCE(SUM(total), 0) AS total_sales
                FROM sales
                %s;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = prepareDateFilteredStatement(
                     connection,
                     sql.formatted(buildCompletedSalesDateWhereClause(startDate, endDateExclusive)),
                     startDate,
                     endDateExclusive
             );
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.next()) {
                return new SalesReportSummary(0, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            return new SalesReportSummary(
                    resultSet.getInt("sales_count"),
                    Money.getCents(resultSet, "total_sales"),
                    averageTicket(Money.getCents(resultSet, "total_sales"), resultSet.getInt("sales_count"))
            );
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o resumo do relatório de vendas.", exception);
        }
    }

    public List<PaymentMethodReport> findSalesByPaymentMethod() {
        return findSalesByPaymentMethod(null, null);
    }

    public List<PaymentMethodReport> findSalesByPaymentMethod(LocalDate startDate, LocalDate endDateExclusive) {
        String sql = """
                SELECT payment_method,
                       COUNT(*) AS sales_count,
                       COALESCE(SUM(total), 0) AS total_amount
                FROM sales
                %s
                GROUP BY payment_method
                ORDER BY total_amount DESC;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = prepareDateFilteredStatement(
                     connection,
                     sql.formatted(buildCompletedSalesDateWhereClause(startDate, endDateExclusive)),
                     startDate,
                     endDateExclusive
             );
             ResultSet resultSet = statement.executeQuery()) {

            List<PaymentMethodReport> reports = new ArrayList<>();

            while (resultSet.next()) {
                reports.add(new PaymentMethodReport(
                        resultSet.getString("payment_method"),
                        resultSet.getInt("sales_count"),
                        Money.getCents(resultSet, "total_amount")
                ));
            }

            return reports;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o relatório por forma de pagamento.", exception);
        }
    }

    public List<ProductSalesReport> findTopSellingProducts() {
        return findTopSellingProducts(null, null);
    }

    public List<ProductSalesReport> findTopSellingProducts(LocalDate startDate, LocalDate endDateExclusive) {
        String sql = """
                SELECT si.product_name,
                       COALESCE(SUM(si.quantity), 0) AS quantity_sold,
                       COALESCE(SUM(si.subtotal), 0) AS total_amount
                FROM sale_items si
                JOIN sales s ON s.id = si.sale_id
                %s
                GROUP BY si.product_id, si.product_name
                ORDER BY quantity_sold DESC, total_amount DESC
                LIMIT 10;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = prepareDateFilteredStatement(
                     connection,
                     sql.formatted(buildCompletedSalesDateWhereClause("s.created_at", startDate, endDateExclusive)),
                     startDate,
                     endDateExclusive
             );
             ResultSet resultSet = statement.executeQuery()) {

            List<ProductSalesReport> reports = new ArrayList<>();

            while (resultSet.next()) {
                reports.add(new ProductSalesReport(
                        resultSet.getString("product_name"),
                        averageTicket(Money.getCents(resultSet, "total_amount"), resultSet.getInt("quantity_sold")),
                        resultSet.getInt("quantity_sold"),
                        Money.getCents(resultSet, "total_amount")
                ));
            }

            return reports;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o relatório dos produtos mais vendidos.", exception);
        }
    }

    public SalesReportSummary getSalesReportSummary(SalesReportFilter filter) {
        FilterSql filterSql = buildReportFilter(filter, false);
        String sql = """
                SELECT COUNT(*) AS sales_count,
                       COALESCE(SUM(s.total), 0) AS total_sales
                FROM sales s
                %s;
                """.formatted(filterSql.whereClause());
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = prepareFilteredStatement(connection, sql, filterSql.parameters());
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return new SalesReportSummary(0, BigDecimal.ZERO, BigDecimal.ZERO);
            }
            int salesCount = resultSet.getInt("sales_count");
            BigDecimal total = Money.getCents(resultSet, "total_sales");
            return new SalesReportSummary(salesCount, total, averageTicket(total, salesCount));
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o resumo do relatório de vendas.", exception);
        }
    }

    public List<PaymentMethodReport> findSalesByPaymentMethod(SalesReportFilter filter) {
        FilterSql filterSql = buildReportFilter(filter, false);
        String sql = """
                SELECT s.payment_method,
                       COUNT(*) AS sales_count,
                       COALESCE(SUM(s.total), 0) AS total_amount
                FROM sales s
                %s
                GROUP BY s.payment_method
                ORDER BY total_amount DESC;
                """.formatted(filterSql.whereClause());
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = prepareFilteredStatement(connection, sql, filterSql.parameters());
             ResultSet resultSet = statement.executeQuery()) {
            List<PaymentMethodReport> reports = new ArrayList<>();
            while (resultSet.next()) {
                reports.add(new PaymentMethodReport(
                        resultSet.getString("payment_method"),
                        resultSet.getInt("sales_count"),
                        Money.getCents(resultSet, "total_amount")
                ));
            }
            return reports;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o relatório por forma de pagamento.", exception);
        }
    }

    public List<ProductSalesReport> findTopSellingProducts(SalesReportFilter filter) {
        FilterSql filterSql = buildReportFilter(filter, true);
        String sql = """
                SELECT si.product_name,
                       COALESCE(SUM(si.quantity), 0) AS quantity_sold,
                       COALESCE(SUM(si.subtotal), 0) AS total_amount
                FROM sale_items si
                INNER JOIN sales s ON s.id = si.sale_id
                INNER JOIN products p ON p.id = si.product_id
                %s
                GROUP BY si.product_id, si.product_name
                ORDER BY quantity_sold DESC, total_amount DESC
                LIMIT 10;
                """.formatted(filterSql.whereClause());
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = prepareFilteredStatement(connection, sql, filterSql.parameters());
             ResultSet resultSet = statement.executeQuery()) {
            List<ProductSalesReport> reports = new ArrayList<>();
            while (resultSet.next()) {
                int quantity = resultSet.getInt("quantity_sold");
                BigDecimal total = Money.getCents(resultSet, "total_amount");
                reports.add(new ProductSalesReport(
                        resultSet.getString("product_name"), averageTicket(total, quantity), quantity, total
                ));
            }
            return reports;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o relatório dos produtos mais vendidos.", exception);
        }
    }

    public List<String> findReportCustomers() {
        return findReportFilterValues("s.customer_name", "FROM sales s", "WHERE s.status = 'COMPLETED'");
    }

    public List<String> findReportCategories() {
        return findReportFilterValues("p.category", "FROM sale_items si INNER JOIN sales s ON s.id = si.sale_id INNER JOIN products p ON p.id = si.product_id", "WHERE s.status = 'COMPLETED'");
    }

    public List<String> findReportSuppliers() {
        return findReportFilterValues("p.supplier", "FROM sale_items si INNER JOIN sales s ON s.id = si.sale_id INNER JOIN products p ON p.id = si.product_id", "WHERE s.status = 'COMPLETED'");
    }

    private FilterSql buildReportFilter(SalesReportFilter filter, boolean directProductJoin) {
        if (filter == null) {
            filter = new SalesReportFilter(null, null, "", "", "", "");
        }
        if ((filter.startDate() == null) != (filter.endDateExclusive() == null)
                || (filter.startDate() != null && !filter.startDate().isBefore(filter.endDateExclusive()))) {
            throw new InvalidDateRangeException();
        }

        StringBuilder where = new StringBuilder("WHERE s.status = 'COMPLETED'");
        List<String> parameters = new ArrayList<>();
        if (filter.startDate() != null) {
            where.append(" AND DATE(s.created_at) >= DATE(?) AND DATE(s.created_at) < DATE(?)");
            parameters.add(filter.startDate().toString());
            parameters.add(filter.endDateExclusive().toString());
        }
        appendEqualsFilter(where, parameters, "s.customer_name", filter.customerName());
        appendEqualsFilter(where, parameters, "s.payment_method", filter.paymentMethod());

        if (directProductJoin) {
            appendEqualsFilter(where, parameters, "p.category", filter.category());
            appendEqualsFilter(where, parameters, "p.supplier", filter.supplier());
        } else if (!filter.category().isBlank() || !filter.supplier().isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM sale_items filter_item ")
                    .append("INNER JOIN products filter_product ON filter_product.id = filter_item.product_id ")
                    .append("WHERE filter_item.sale_id = s.id");
            if (!filter.category().isBlank()) {
                where.append(" AND filter_product.category = ?");
                parameters.add(filter.category());
            }
            if (!filter.supplier().isBlank()) {
                where.append(" AND filter_product.supplier = ?");
                parameters.add(filter.supplier());
            }
            where.append(")");
        }
        return new FilterSql(where.toString(), parameters);
    }

    private void appendEqualsFilter(StringBuilder where, List<String> parameters, String column, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        where.append(" AND ").append(column).append(" = ?");
        parameters.add(value);
    }

    private PreparedStatement prepareFilteredStatement(Connection connection, String sql, List<String> parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int index = 0; index < parameters.size(); index++) {
            statement.setString(index + 1, parameters.get(index));
        }
        return statement;
    }

    private List<String> findReportFilterValues(String column, String fromClause, String whereClause) {
        String sql = "SELECT DISTINCT " + column + " AS value " + fromClause + " " + whereClause
                + " AND " + column + " IS NOT NULL AND trim(" + column + ") <> '' ORDER BY value;";
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            List<String> values = new ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getString("value"));
            }
            return values;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar os filtros do relatório.", exception);
        }
    }

    private BigDecimal queryTotal(String sql) {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return BigDecimal.ZERO;
            }

            return Money.getCents(resultSet, "total");
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o total de vendas.", exception);
        }
    }

    private BigDecimal averageTicket(BigDecimal total, int quantity) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }

        return total.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    private String buildSalesDateWhereClause(LocalDate startDate, LocalDate endDateExclusive) {
        return buildSalesDateWhereClause("created_at", startDate, endDateExclusive);
    }

    private String buildSalesDateWhereClause(String columnName, LocalDate startDate, LocalDate endDateExclusive) {
        if ((startDate == null) != (endDateExclusive == null)
                || (startDate != null && !startDate.isBefore(endDateExclusive))) {
            throw new InvalidDateRangeException();
        }
        if (startDate == null || endDateExclusive == null) {
            return "";
        }

        return "WHERE DATE(" + columnName + ") >= DATE(?) AND DATE(" + columnName + ") < DATE(?)";
    }

    private String buildCompletedSalesDateWhereClause(LocalDate startDate, LocalDate endDateExclusive) {
        return buildCompletedSalesDateWhereClause("created_at", startDate, endDateExclusive);
    }

    private String buildCompletedSalesDateWhereClause(String columnName, LocalDate startDate, LocalDate endDateExclusive) {
        if ((startDate == null) != (endDateExclusive == null)
                || (startDate != null && !startDate.isBefore(endDateExclusive))) {
            throw new InvalidDateRangeException();
        }

        String statusColumn = columnName.contains(".")
                ? columnName.substring(0, columnName.indexOf('.')) + ".status"
                : "status";
        if (startDate == null) {
            return "WHERE " + statusColumn + " = 'COMPLETED'";
        }

        return "WHERE " + statusColumn + " = 'COMPLETED'"
                + " AND DATE(" + columnName + ") >= DATE(?) AND DATE(" + columnName + ") < DATE(?)";
    }

    private PreparedStatement prepareDateFilteredStatement(
            Connection connection,
            String sql,
            LocalDate startDate,
            LocalDate endDateExclusive
    ) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);

        if (startDate != null && endDateExclusive != null) {
            statement.setString(1, startDate.toString());
            statement.setString(2, endDateExclusive.toString());
        }

        return statement;
    }

    private Sale mapSale(ResultSet resultSet) throws SQLException {
        return new Sale(
                resultSet.getLong("id"),
                resultSet.getLong("customer_id"),
                resultSet.getString("customer_name"),
                Money.getCents(resultSet, "subtotal"),
                Money.getCents(resultSet, "discount"),
                Money.getCents(resultSet, "total"),
                resultSet.getString("payment_method"),
                LocalDateTime.parse(resultSet.getString("created_at"), SQLITE_DATE_TIME),
                resultSet.getString("status"),
                parseDateTime(resultSet.getString("cancelled_at")),
                resultSet.getString("cancellation_reason"),
                List.of()
        );
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null ? null : LocalDateTime.parse(value, SQLITE_DATE_TIME);
    }

    private String findSaleStatus(Connection connection, String sql, Long saleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new EntityNotFoundException("Venda");
                }
                return resultSet.getString("status");
            }
        }
    }

    private boolean hasPaidInstallments(Connection connection, String sql, Long saleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private List<SaleItem> findSaleItems(Connection connection, String sql, Long saleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SaleItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(new SaleItem(
                            resultSet.getLong("id"),
                            resultSet.getLong("sale_id"),
                            resultSet.getLong("product_id"),
                            resultSet.getString("product_name"),
                            resultSet.getInt("quantity"),
                            Money.getCents(resultSet, "unit_price"),
                            Money.getCents(resultSet, "subtotal")
                    ));
                }
                return items;
            }
        }
    }

    private Long readGeneratedId(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.getGeneratedKeys()) {
            if (!resultSet.next()) {
                throw new SQLException("O identificador da venda não foi gerado.");
            }

            return resultSet.getLong(1);
        }
    }

    private record FilterSql(String whereClause, List<String> parameters) {
    }
}
