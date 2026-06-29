package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SaleRepository {

    public void save(Sale sale) {
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

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement saleStatement = connection.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement itemStatement = connection.prepareStatement(insertSaleItemSql);
                 PreparedStatement stockStatement = connection.prepareStatement(updateStockSql);
                 PreparedStatement movementStatement = connection.prepareStatement(insertStockMovementSql)) {

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

    private Long readGeneratedId(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.getGeneratedKeys()) {
            if (!resultSet.next()) {
                throw new SQLException("Sale id was not generated.");
            }

            return resultSet.getLong(1);
        }
    }
}
