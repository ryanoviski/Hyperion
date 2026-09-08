package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Expense;
import com.hyperion.exception.EntityNotFoundException;
import com.hyperion.exception.PersistenceException;
import com.hyperion.util.Money;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Long save(Expense expense) {
        String sql = """
                INSERT INTO expenses (description, category, amount)
                VALUES (?, ?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, expense.getDescription());
            statement.setString(2, expense.getCategory());
            Money.setCents(statement, 3, expense.getAmount());
            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                throw new PersistenceException("Não foi possível obter o identificador da despesa criada.");
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível salvar a despesa.", exception);
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
            if (statement.executeUpdate() != 1) {
                throw new EntityNotFoundException("Despesa");
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível remover a despesa.", exception);
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
            throw new PersistenceException("Não foi possível listar as despesas.", exception);
        }
    }

    public List<Expense> findByDateRange(LocalDate startDate, LocalDate endDateExclusive) {
        String sql = """
                SELECT id, description, category, amount, created_at
                FROM expenses
                WHERE DATE(created_at) >= DATE(?)
                  AND DATE(created_at) < DATE(?)
                ORDER BY created_at DESC, id DESC;
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, startDate.toString());
            statement.setString(2, endDateExclusive.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Expense> expenses = new ArrayList<>();
                while (resultSet.next()) {
                    expenses.add(mapExpense(resultSet));
                }
                return expenses;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível listar as despesas do período.", exception);
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

            return Money.getCents(resultSet, "total");
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível carregar o resumo de despesas.", exception);
        }
    }

    private Expense mapExpense(ResultSet resultSet) throws SQLException {
        return new Expense(
                resultSet.getLong("id"),
                resultSet.getString("description"),
                resultSet.getString("category"),
                Money.getCents(resultSet, "amount"),
                LocalDateTime.parse(resultSet.getString("created_at"), SQLITE_DATE_TIME)
        );
    }
}
