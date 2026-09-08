package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.util.SqliteTimestamp;
import com.hyperion.model.Customer;
import com.hyperion.exception.EntityNotFoundException;
import com.hyperion.exception.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerRepository {

    public void save(Customer customer) {
        String sql = """
                INSERT INTO customers (name, document, phone, email, address, notes)
                VALUES (?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            fillCustomerStatement(statement, customer);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível salvar o cliente.", exception);
        }
    }

    public void update(Customer customer) {
        String sql = """
                UPDATE customers
                SET name = ?,
                    document = ?,
                    phone = ?,
                    email = ?,
                    address = ?,
                    notes = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            fillCustomerStatement(statement, customer);
            statement.setLong(7, customer.getId());
            if (statement.executeUpdate() != 1) {
                throw new EntityNotFoundException("Cliente");
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível atualizar o cliente.", exception);
        }
    }

    public void deactivate(Long id) {
        String sql = """
                UPDATE customers
                SET active = 0,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) {
                throw new EntityNotFoundException("Cliente");
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível desativar o cliente.", exception);
        }
    }

    public void reactivate(Long id) {
        String sql = """
                UPDATE customers
                SET active = 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) {
                throw new EntityNotFoundException("Cliente");
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível reativar o cliente.", exception);
        }
    }

    public Optional<Customer> findById(Long id) {
        String sql = """
                SELECT id, name, document, phone, email, address, notes, active, created_at, updated_at
                FROM customers
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapCustomer(resultSet));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível localizar o cliente.", exception);
        }
    }

    public List<Customer> findAllActive() {
        return findByActiveStatus(true);
    }

    public List<Customer> findAllInactive() {
        return findByActiveStatus(false);
    }

    public List<Customer> searchActive(String term) {
        return searchByActiveStatus(term, true);
    }

    public List<Customer> searchInactive(String term) {
        return searchByActiveStatus(term, false);
    }

    private List<Customer> findByActiveStatus(boolean active) {
        String sql = """
                SELECT id, name, document, phone, email, address, notes, active, created_at, updated_at
                FROM customers
                WHERE active = ?
                ORDER BY name;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, active ? 1 : 0);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapCustomers(resultSet);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível listar os clientes.", exception);
        }
    }

    private List<Customer> searchByActiveStatus(String term, boolean active) {
        String sql = """
                SELECT id, name, document, phone, email, address, notes, active, created_at, updated_at
                FROM customers
                WHERE active = ?
                  AND (
                      LOWER(name) LIKE LOWER(?)
                      OR LOWER(COALESCE(document, '')) LIKE LOWER(?)
                      OR LOWER(COALESCE(phone, '')) LIKE LOWER(?)
                      OR LOWER(COALESCE(email, '')) LIKE LOWER(?)
                  )
                ORDER BY name;
                """;

        String searchTerm = "%" + term + "%";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, active ? 1 : 0);
            statement.setString(2, searchTerm);
            statement.setString(3, searchTerm);
            statement.setString(4, searchTerm);
            statement.setString(5, searchTerm);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapCustomers(resultSet);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Não foi possível pesquisar os clientes.", exception);
        }
    }

    private void fillCustomerStatement(PreparedStatement statement, Customer customer) throws SQLException {
        statement.setString(1, customer.getName());
        statement.setString(2, customer.getDocument());
        statement.setString(3, customer.getPhone());
        statement.setString(4, customer.getEmail());
        statement.setString(5, customer.getAddress());
        statement.setString(6, customer.getNotes());
    }

    private List<Customer> mapCustomers(ResultSet resultSet) throws SQLException {
        List<Customer> customers = new ArrayList<>();

        while (resultSet.next()) {
            customers.add(mapCustomer(resultSet));
        }

        return customers;
    }

    private Customer mapCustomer(ResultSet resultSet) throws SQLException {
        return new Customer(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("document"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("address"),
                resultSet.getString("notes"),
                resultSet.getInt("active") == 1,
                SqliteTimestamp.toLocalDateTime(resultSet.getString("created_at"), "criação do cliente"),
                SqliteTimestamp.toLocalDateTime(resultSet.getString("updated_at"), "atualização do cliente")
        );
    }
}
