package com.hyperion.support;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.config.DatabaseInitializer;
import com.hyperion.model.Customer;
import com.hyperion.model.Product;
import com.hyperion.service.CustomerService;
import com.hyperion.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class DatabaseIntegrationTest {

    @TempDir
    protected Path testDirectory;

    @BeforeEach
    void initializeDatabase() {
        System.setProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY, testDirectory.resolve("data").toString());
        DatabaseInitializer.initialize();
    }

    @AfterEach
    void clearDataDirectoryOverride() {
        System.clearProperty(DatabaseConfig.DATA_DIRECTORY_PROPERTY);
    }

    protected Customer createCustomer(String name) {
        CustomerService customerService = new CustomerService();
        customerService.createCustomer(name, "", "", "", "", "");

        return customerService.searchActiveCustomers(name).stream()
                .filter(customer -> name.equals(customer.getName()))
                .findFirst()
                .orElseThrow();
    }

    protected Product createProduct(String name, BigDecimal price) {
        ProductService productService = new ProductService();
        productService.createProduct(name, "", price, BigDecimal.ZERO, "", "", "");

        Product product = productService.searchActiveProducts(name).stream()
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(price, product.getPrice());
        return product;
    }
}
