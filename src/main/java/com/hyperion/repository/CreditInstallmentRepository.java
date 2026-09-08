package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.CreditInstallment;
import com.hyperion.model.CreditPayment;
import com.hyperion.exception.DataCorruptionException;
import com.hyperion.exception.PersistenceException;
import com.hyperion.util.Money;

import java.math.BigDecimal;
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

public class CreditInstallmentRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            throw new PersistenceException("Não foi possível listar os alertas do crediário.", exception);
        }
    }

    public List<CreditInstallment> findOpenInstallments() {
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
                ORDER BY due_date ASC, id ASC;
                """;

        return findInstallments(sql);
    }

    public List<CreditInstallment> findPaidInstallments() {
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
                WHERE status = 'PAID'
                ORDER BY COALESCE(paid_at, due_date) DESC, id DESC
                LIMIT 100;
                """;

        return findInstallments(sql);
    }

    public BigDecimal getTotalPaidInstallments() {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS total
                FROM credit_installments
                WHERE status = 'PAID';
                """;

        return queryTotal(sql);
    }

    public BigDecimal getCurrentMonthPaidInstallments() {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS total
                FROM credit_installments
                WHERE status = 'PAID'
                  AND paid_at IS NOT NULL
                  AND strftime('%Y-%m', paid_at) = strftime('%Y-%m', 'now', 'localtime');
                """;

        return queryTotal(sql);
    }

    public boolean markAsPaid(Long id, CreditPayment payment) {
        String sql = """
                UPDATE credit_installments
                SET status = 'PAID',
                    paid_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND status = 'OPEN';
                """;

        String paymentSql = """
                INSERT INTO credit_payments (installment_id, amount, received_by, payment_method, notes)
                SELECT id, amount, ?, ?, ?
                FROM credit_installments
                WHERE id = ?
                  AND status = 'PAID';
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 PreparedStatement paymentStatement = connection.prepareStatement(paymentSql)) {
                statement.setLong(1, id);
                if (statement.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
                paymentStatement.setString(1, payment.receivedBy());
                paymentStatement.setString(2, payment.paymentMethod());
                paymentStatement.setString(3, payment.notes());
                paymentStatement.setLong(4, id);
                if (paymentStatement.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível atualizar a parcela do crediário.", exception);
        }
    }

    public CreditPayment findPaymentByInstallment(Long installmentId) {
        String sql = """
                SELECT id, installment_id, amount, received_by, payment_method, notes, received_at
                FROM credit_payments
                WHERE installment_id = ?;
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, installmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapPayment(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível consultar o pagamento da parcela.", exception);
        }
    }

    public List<CreditPayment> findPaymentsByCustomer(Long customerId) {
        String sql = """
                SELECT payment.id, payment.installment_id, payment.amount, payment.received_by,
                       payment.payment_method, payment.notes, payment.received_at
                FROM credit_payments payment
                INNER JOIN credit_installments installment ON installment.id = payment.installment_id
                WHERE installment.customer_id = ?
                ORDER BY payment.received_at DESC, payment.id DESC;
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CreditPayment> payments = new ArrayList<>();
                while (resultSet.next()) {
                    payments.add(mapPayment(resultSet));
                }
                return payments;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível listar o histórico de recebimentos.", exception);
        }
    }

    public BigDecimal getOpenBalanceByCustomer(Long customerId) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS total
                FROM credit_installments
                WHERE customer_id = ?
                  AND status = 'OPEN';
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Money.getCents(resultSet, "total") : BigDecimal.ZERO;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível calcular o saldo em aberto do cliente.", exception);
        }
    }

    private List<CreditInstallment> findInstallments(String sql) {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            List<CreditInstallment> installments = new ArrayList<>();

            while (resultSet.next()) {
                installments.add(mapInstallment(resultSet));
            }

            return installments;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível listar as parcelas do crediário.", exception);
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
            throw new PersistenceException("Não foi possível calcular o total recebido do crediário.", exception);
        }
    }

    private CreditInstallment mapInstallment(ResultSet resultSet) throws SQLException {
        try {
            return new CreditInstallment(
                resultSet.getLong("id"),
                resultSet.getLong("sale_id"),
                resultSet.getLong("customer_id"),
                resultSet.getString("customer_name"),
                resultSet.getInt("installment_number"),
                resultSet.getInt("total_installments"),
                Money.getCents(resultSet, "amount"),
                LocalDate.parse(resultSet.getString("due_date")),
                    resultSet.getString("status")
            );
        } catch (RuntimeException exception) {
            throw new DataCorruptionException("Há uma parcela do crediário com dados inválidos.", exception);
        }
    }

    private CreditPayment mapPayment(ResultSet resultSet) throws SQLException {
        return new CreditPayment(
                resultSet.getLong("id"),
                resultSet.getLong("installment_id"),
                Money.getCents(resultSet, "amount"),
                resultSet.getString("received_by"),
                resultSet.getString("payment_method"),
                resultSet.getString("notes"),
                LocalDateTime.parse(resultSet.getString("received_at"), SQLITE_DATE_TIME)
        );
    }
}
