package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Expense;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void save(Expense expense) {
        String sql = """
                INSERT INTO expenses (description, category, amount)
                VALUES (?, ?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, expense.getDescription());
            statement.setString(2, expense.getCategory());
            statement.setBigDecimal(3, expense.getAmount());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save expense.", exception);
        }
    }

    public void delete(Long id) {
        String sql = """
                DELETE FROM expenses
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete expense.", exception);
        }
    }

    public List<Expense> findLatest() {
        String sql = """
                SELECT id,
                       description,
                       category,
                       amount,
                       created_at
                FROM expenses
                ORDER BY created_at DESC, id DESC
                LIMIT 100;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            List<Expense> expenses = new ArrayList<>();

            while (resultSet.next()) {
                expenses.add(mapExpense(resultSet));
            }

            return expenses;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list expenses.", exception);
        }
    }

    public BigDecimal getTotalExpenses() {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS total
                FROM expenses;
                """;

        return queryTotal(sql);
    }

    public BigDecimal getCurrentMonthExpenses() {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS total
                FROM expenses
                WHERE strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now', 'localtime');
                """;

        return queryTotal(sql);
    }

    private BigDecimal queryTotal(String sql) {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return BigDecimal.ZERO;
            }

            return resultSet.getBigDecimal("total");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load expenses summary.", exception);
        }
    }

    private Expense mapExpense(ResultSet resultSet) throws SQLException {
        return new Expense(
                resultSet.getLong("id"),
                resultSet.getString("description"),
                resultSet.getString("category"),
                resultSet.getBigDecimal("amount"),
                LocalDateTime.parse(resultSet.getString("created_at"), SQLITE_DATE_TIME)
        );
    }
}
