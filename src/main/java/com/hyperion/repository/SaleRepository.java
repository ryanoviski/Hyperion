package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.DailySalesSummary;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class SaleRepository {

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
                WHERE id = ?;
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
                saleStatement.setBigDecimal(3, sale.getSubtotal());
                saleStatement.setBigDecimal(4, sale.getDiscount());
                saleStatement.setBigDecimal(5, sale.getTotal());
                saleStatement.setString(6, sale.getPaymentMethod());
                saleStatement.executeUpdate();

                Long saleId = readGeneratedId(saleStatement);

                for (SaleItem item : sale.getItems()) {
                    itemStatement.setLong(1, saleId);
                    itemStatement.setLong(2, item.getProductId());
                    itemStatement.setString(3, item.getProductName());
                    itemStatement.setInt(4, item.getQuantity());
                    itemStatement.setBigDecimal(5, item.getUnitPrice());
                    itemStatement.setBigDecimal(6, item.getSubtotal());
                    itemStatement.addBatch();

                    stockStatement.setInt(1, item.getQuantity());
                    stockStatement.setLong(2, item.getProductId());
                    stockStatement.addBatch();

                    movementStatement.setLong(1, item.getProductId());
                    movementStatement.setInt(2, item.getQuantity());
                    movementStatement.setString(3, "Venda #" + saleId);
                    movementStatement.addBatch();
                }

                itemStatement.executeBatch();
                stockStatement.executeBatch();
                movementStatement.executeBatch();

                if (creditSalePlan != null) {
                    addInstallmentBatch(installmentStatement, sale, saleId, creditSalePlan);
                    installmentStatement.executeBatch();
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save sale.", exception);
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
            statement.setBigDecimal(6, amount);
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
                WHERE DATE(created_at) = DATE('now', 'localtime');
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return new DailySalesSummary(BigDecimal.ZERO, 0);
            }

            return new DailySalesSummary(
                    resultSet.getBigDecimal("total"),
                    resultSet.getInt("sales_count")
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load daily sales summary.", exception);
        }
    }

    private Long readGeneratedId(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.getGeneratedKeys()) {
            if (!resultSet.next()) {
                throw new SQLException("Sale id was not generated.");
            }

            return resultSet.getLong(1);
        }
    }
}
