package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.exception.PersistenceException;
import com.hyperion.model.PinLockState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppSettingsRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean isFirstRunCompleted() {
        ensureSettingsRowExists();

        String sql = "SELECT first_run_completed FROM app_settings ORDER BY id LIMIT 1;";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            return resultSet.next() && resultSet.getInt("first_run_completed") == 1;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível ler as configurações do aplicativo.", exception);
        }
    }

    public boolean isPinEnabled() {
        ensureSettingsRowExists();

        String sql = "SELECT pin_enabled FROM app_settings ORDER BY id LIMIT 1;";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            return resultSet.next() && resultSet.getInt("pin_enabled") == 1;
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível ler a configuração do PIN.", exception);
        }
    }

    public String findPinHash() {
        ensureSettingsRowExists();

        String sql = "SELECT pin_hash FROM app_settings ORDER BY id LIMIT 1;";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return null;
            }

            return resultSet.getString("pin_hash");
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível ler o PIN armazenado.", exception);
        }
    }

    public String findTheme() {
        ensureSettingsRowExists();

        String sql = "SELECT theme FROM app_settings ORDER BY id LIMIT 1;";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return "dark";
            }

            return resultSet.getString("theme");
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível ler o tema do aplicativo.", exception);
        }
    }

    public PinLockState findPinLockState() {
        ensureSettingsRowExists();

        String sql = """
                SELECT failed_pin_attempts, pin_locked_until
                FROM app_settings
                ORDER BY id
                LIMIT 1;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return new PinLockState(0, null);
            }

            String lockedUntil = resultSet.getString("pin_locked_until");
            return new PinLockState(
                    resultSet.getInt("failed_pin_attempts"),
                    lockedUntil == null ? null : LocalDateTime.parse(lockedUntil, SQLITE_DATE_TIME)
            );
        } catch (SQLException | RuntimeException exception) {
            throw new PersistenceException("Não foi possível ler a proteção do PIN.", exception);
        }
    }

    public void recordFailedPinAttempt(int failureCount, LocalDateTime lockedUntil) {
        ensureSettingsRowExists();
        if (failureCount <= 0 || lockedUntil == null) {
            throw new IllegalArgumentException("Dados inválidos para registrar tentativa de PIN.");
        }

        String updateSql = """
                UPDATE app_settings
                SET failed_pin_attempts = ?,
                    pin_locked_until = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;
        String auditSql = """
                INSERT INTO pin_attempts (failure_count, locked_until)
                VALUES (?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement(updateSql);
                 PreparedStatement audit = connection.prepareStatement(auditSql)) {
                String formattedLockedUntil = lockedUntil.format(SQLITE_DATE_TIME);
                update.setInt(1, failureCount);
                update.setString(2, formattedLockedUntil);
                update.executeUpdate();
                audit.setInt(1, failureCount);
                audit.setString(2, formattedLockedUntil);
                audit.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível registrar a tentativa de PIN.", exception);
        }
    }

    public void resetPinProtection() {
        ensureSettingsRowExists();
        String sql = """
                UPDATE app_settings
                SET failed_pin_attempts = 0,
                    pin_locked_until = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;
        executeUpdate(sql);
    }

    public void completeFirstRunWithoutPin() {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET first_run_completed = 1,
                    pin_enabled = 0,
                    pin_hash = NULL,
                    failed_pin_attempts = 0,
                    pin_locked_until = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        executeUpdate(sql);
    }

    public void completeFirstRunWithPin(String pinHash) {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET first_run_completed = 1,
                    pin_enabled = 1,
                    pin_hash = ?,
                    failed_pin_attempts = 0,
                    pin_locked_until = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pinHash);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível atualizar as configurações do aplicativo.", exception);
        }
    }

    public void updatePin(String pinHash) {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET pin_enabled = 1,
                    pin_hash = ?,
                    failed_pin_attempts = 0,
                    pin_locked_until = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pinHash);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível atualizar o PIN.", exception);
        }
    }

    public void removePin() {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET pin_enabled = 0,
                    pin_hash = NULL,
                    failed_pin_attempts = 0,
                    pin_locked_until = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        executeUpdate(sql);
    }

    public void updateTheme(String theme) {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET theme = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, theme);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível atualizar o tema do aplicativo.", exception);
        }
    }

    private void ensureSettingsRowExists() {
        String sql = """
                INSERT INTO app_settings (first_run_completed, pin_enabled, theme)
                SELECT 0, 0, 'dark'
                WHERE NOT EXISTS (SELECT 1 FROM app_settings);
                """;

        executeUpdate(sql);
    }

    private void executeUpdate(String sql) {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível atualizar as configurações do aplicativo.", exception);
        }
    }
}
