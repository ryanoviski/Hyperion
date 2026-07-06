package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AppSettingsRepository {

    public boolean isFirstRunCompleted() {
        ensureSettingsRowExists();

        String sql = "SELECT first_run_completed FROM app_settings ORDER BY id LIMIT 1;";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            return resultSet.next() && resultSet.getInt("first_run_completed") == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read app settings.", exception);
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
            throw new IllegalStateException("Could not read PIN setting.", exception);
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
            throw new IllegalStateException("Could not read PIN hash.", exception);
        }
    }

    public void completeFirstRunWithoutPin() {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET first_run_completed = 1,
                    pin_enabled = 0,
                    pin_hash = NULL,
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
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pinHash);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update app settings.", exception);
        }
    }

    public void updatePin(String pinHash) {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET pin_enabled = 1,
                    pin_hash = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, pinHash);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update PIN.", exception);
        }
    }

    public void removePin() {
        ensureSettingsRowExists();

        String sql = """
                UPDATE app_settings
                SET pin_enabled = 0,
                    pin_hash = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = (SELECT id FROM app_settings ORDER BY id LIMIT 1);
                """;

        executeUpdate(sql);
    }

    private void ensureSettingsRowExists() {
        String sql = """
                INSERT INTO app_settings (first_run_completed, pin_enabled)
                SELECT 0, 0
                WHERE NOT EXISTS (SELECT 1 FROM app_settings);
                """;

        executeUpdate(sql);
    }

    private void executeUpdate(String sql) {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update app settings.", exception);
        }
    }
}
