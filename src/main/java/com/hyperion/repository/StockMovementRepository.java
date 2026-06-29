package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockMovementRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void registerMovement(StockMovement movement, int stockDelta) {
        String insertMovementSql = """
                INSERT INTO stock_movements (product_id, type, quantity, notes)
                VALUES (?, ?, ?, ?);
                """;

        String updateStockSql = """
                UPDATE products
                SET stock_quantity = stock_quantity + ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement insertStatement = connection.prepareStatement(insertMovementSql);
                 PreparedStatement updateStatement = connection.prepareStatement(updateStockSql)) {

                insertStatement.setLong(1, movement.getProductId());
                insertStatement.setString(2, movement.getType());
                insertStatement.setInt(3, movement.getQuantity());
                insertStatement.setString(4, movement.getNotes());
                insertStatement.executeUpdate();

                updateStatement.setInt(1, stockDelta);
                updateStatement.setLong(2, movement.getProductId());
                updateStatement.executeUpdate();

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not register stock movement.", exception);
        }
    }

    public List<StockMovement> findLatest() {
        String sql = """
                SELECT sm.id,
                       sm.product_id,
                       p.name AS product_name,
                       sm.type,
                       sm.quantity,
                       sm.notes,
                       sm.created_at
                FROM stock_movements sm
                INNER JOIN products p ON p.id = sm.product_id
                ORDER BY sm.created_at DESC, sm.id DESC
                LIMIT 100;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            List<StockMovement> movements = new ArrayList<>();

            while (resultSet.next()) {
                movements.add(mapStockMovement(resultSet));
            }

            return movements;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list stock movements.", exception);
        }
    }

    private StockMovement mapStockMovement(ResultSet resultSet) throws SQLException {
        return new StockMovement(
                resultSet.getLong("id"),
                resultSet.getLong("product_id"),
                resultSet.getString("product_name"),
                resultSet.getString("type"),
                resultSet.getInt("quantity"),
                resultSet.getString("notes"),
                LocalDateTime.parse(resultSet.getString("created_at"), SQLITE_DATE_TIME)
        );
    }
}
