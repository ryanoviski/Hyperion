package com.hyperion.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializerTest {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT name
                     FROM sqlite_master
                     WHERE type = 'table'
                       AND name IN ('company', 'app_settings')
                     ORDER BY name;
                     """)) {

            while (resultSet.next()) {
                System.out.println("Table found: " + resultSet.getString("name"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not verify database tables.", exception);
        }
    }
}
