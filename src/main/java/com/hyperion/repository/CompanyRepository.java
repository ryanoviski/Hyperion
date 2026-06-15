package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Company;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CompanyRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void save(Company company) {
        String sql = """
                INSERT INTO company (name, owner_name)
                VALUES (?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, company.getName());
            statement.setString(2, company.getOwnerName());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save company.", exception);
        }
    }

    public boolean exists() {
        String sql = "SELECT EXISTS(SELECT 1 FROM company LIMIT 1);";

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            return resultSet.next() && resultSet.getInt(1) == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not verify company existence.", exception);
        }
    }

    public Optional<Company> findFirst() {
        String sql = """
                SELECT id, name, owner_name, created_at
                FROM company
                ORDER BY id
                LIMIT 1;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (!resultSet.next()) {
                return Optional.empty();
            }

            Company company = new Company(
                    resultSet.getLong("id"),
                    resultSet.getString("name"),
                    resultSet.getString("owner_name"),
                    LocalDateTime.parse(resultSet.getString("created_at"), SQLITE_DATE_TIME)
            );

            return Optional.of(company);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find company.", exception);
        }
    }
}
