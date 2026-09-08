package com.hyperion.repository;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.model.Product;
import com.hyperion.exception.EntityNotFoundException;
import com.hyperion.util.Money;

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
import java.util.Optional;

public class ProductRepository {

    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void save(Product product) {
        String sql = """
                INSERT INTO products (name, description, price, cost, stock_quantity, minimum_stock, category, barcode, supplier)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            fillProductStatement(statement, product);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível salvar o produto.", exception);
        }
    }

    public void update(Product product) {
        String sql = """
                UPDATE products
                SET name = ?,
                    description = ?,
                    price = ?,
                    cost = ?,
                    minimum_stock = ?,
                    category = ?,
                    barcode = ?,
                    supplier = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            fillProductUpdateStatement(statement, product);
            statement.setLong(9, product.getId());
            if (statement.executeUpdate() != 1) {
                throw new EntityNotFoundException("Produto");
            }
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível atualizar o produto.", exception);
        }
    }

    public void deactivate(Long id) {
        String sql = """
                UPDATE products
                SET active = 0,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) {
                throw new EntityNotFoundException("Produto");
            }
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível desativar o produto.", exception);
        }
    }

    public void reactivate(Long id) {
        String sql = """
                UPDATE products
                SET active = 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) {
                throw new EntityNotFoundException("Produto");
            }
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível reativar o produto.", exception);
        }
    }

    public Optional<Product> findById(Long id) {
        String sql = """
                SELECT id, name, description, price, cost, stock_quantity, minimum_stock, category, barcode, supplier, active, created_at, updated_at
                FROM products
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapProduct(resultSet));
            }
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível localizar o produto.", exception);
        }
    }

    public List<Product> findAllActive() {
        return findByActiveStatus(true);
    }

    public List<Product> findAllInactive() {
        return findByActiveStatus(false);
    }

    public List<Product> searchActive(String term) {
        return searchByActiveStatus(term, true);
    }

    public List<Product> searchInactive(String term) {
        return searchByActiveStatus(term, false);
    }

    public List<Product> findLowStockProducts() {
        String sql = """
                SELECT id, name, description, price, cost, stock_quantity, minimum_stock, category, barcode, supplier, active, created_at, updated_at
                FROM products
                WHERE active = 1
                  AND stock_quantity <= minimum_stock
                ORDER BY stock_quantity ASC, name ASC;
                """;
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return mapProducts(resultSet);
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível listar os produtos com estoque baixo.", exception);
        }
    }

    private List<Product> findByActiveStatus(boolean active) {
        String sql = """
                SELECT id, name, description, price, cost, stock_quantity, minimum_stock, category, barcode, supplier, active, created_at, updated_at
                FROM products
                WHERE active = ?
                ORDER BY name;
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, active ? 1 : 0);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapProducts(resultSet);
            }
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível listar os produtos.", exception);
        }
    }

    private List<Product> searchByActiveStatus(String term, boolean active) {
        String sql = """
                SELECT id, name, description, price, cost, stock_quantity, minimum_stock, category, barcode, supplier, active, created_at, updated_at
                FROM products
                WHERE active = ?
                  AND (
                      LOWER(name) LIKE LOWER(?)
                      OR LOWER(COALESCE(category, '')) LIKE LOWER(?)
                      OR LOWER(COALESCE(barcode, '')) LIKE LOWER(?)
                      OR LOWER(COALESCE(supplier, '')) LIKE LOWER(?)
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
                return mapProducts(resultSet);
            }
        } catch (SQLException exception) {
            throw new com.hyperion.exception.PersistenceException("Não foi possível pesquisar os produtos.", exception);
        }
    }

    private void fillProductStatement(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getName());
        statement.setString(2, product.getDescription());
        Money.setCents(statement, 3, product.getPrice());
        Money.setCents(statement, 4, product.getCost());
        statement.setInt(5, product.getStockQuantity());
        statement.setInt(6, product.getMinimumStock());
        statement.setString(7, product.getCategory());
        statement.setString(8, product.getBarcode());
        statement.setString(9, product.getSupplier());
    }

    private void fillProductUpdateStatement(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getName());
        statement.setString(2, product.getDescription());
        Money.setCents(statement, 3, product.getPrice());
        Money.setCents(statement, 4, product.getCost());
        statement.setInt(5, product.getMinimumStock());
        statement.setString(6, product.getCategory());
        statement.setString(7, product.getBarcode());
        statement.setString(8, product.getSupplier());
    }

    private List<Product> mapProducts(ResultSet resultSet) throws SQLException {
        List<Product> products = new ArrayList<>();

        while (resultSet.next()) {
            products.add(mapProduct(resultSet));
        }

        return products;
    }

    private Product mapProduct(ResultSet resultSet) throws SQLException {
        return new Product(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                Money.getCents(resultSet, "price"),
                Money.getCents(resultSet, "cost"),
                resultSet.getInt("stock_quantity"),
                resultSet.getInt("minimum_stock"),
                resultSet.getString("category"),
                resultSet.getString("barcode"),
                resultSet.getString("supplier"),
                resultSet.getInt("active") == 1,
                LocalDateTime.parse(resultSet.getString("created_at"), SQLITE_DATE_TIME),
                LocalDateTime.parse(resultSet.getString("updated_at"), SQLITE_DATE_TIME)
        );
    }
}
