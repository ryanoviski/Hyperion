package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.model.StockMovement;
import com.hyperion.exception.PersistenceException;
import com.hyperion.util.SqliteTimestamp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StockMovementRepository {

    public void registerMovement(StockMovement movement, int stockDelta) {
        String insertMovementSql = """
                INSERT INTO stock_movements (product_id, type, quantity, notes)
                VALUES (?, ?, ?, ?);
                """;

        String updateStockSql = """
                UPDATE products
                SET stock_quantity = stock_quantity + ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND stock_quantity + ? >= 0;
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
                updateStatement.setInt(3, stockDelta);
                if (updateStatement.executeUpdate() != 1) {
                    throw new InsufficientStockException("produto selecionado");
                }

                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível registrar a movimentação de estoque.", exception);
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
            throw new PersistenceException("Não foi possível listar as movimentações de estoque.", exception);
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
                SqliteTimestamp.toLocalDateTime(resultSet.getString("created_at"), "criação da movimentação")
        );
    }
}
