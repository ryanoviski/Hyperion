package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.CreditInstallment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreditInstallmentRepository {

    public List<CreditInstallment> findPendingAlerts() {
        String sql = """
                SELECT id,
                       sale_id,
                       customer_id,
                       customer_name,
                       installment_number,
                       total_installments,
                       amount,
                       due_date,
                       status
                FROM credit_installments
                WHERE status = 'OPEN'
                  AND DATE(due_date) <= DATE('now', 'localtime', '+3 days')
                ORDER BY due_date ASC, id ASC
                LIMIT 5;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<CreditInstallment> installments = new ArrayList<>();

            while (resultSet.next()) {
                installments.add(mapInstallment(resultSet));
            }

            return installments;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list credit installment alerts.", exception);
        }
    }

    private CreditInstallment mapInstallment(ResultSet resultSet) throws SQLException {
        return new CreditInstallment(
                resultSet.getLong("id"),
                resultSet.getLong("sale_id"),
                resultSet.getLong("customer_id"),
                resultSet.getString("customer_name"),
                resultSet.getInt("installment_number"),
                resultSet.getInt("total_installments"),
                resultSet.getBigDecimal("amount"),
                LocalDate.parse(resultSet.getString("due_date")),
                resultSet.getString("status")
        );
    }
}
