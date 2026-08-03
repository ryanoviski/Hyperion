package com.hyperion.service;

import com.hyperion.model.Product;
import com.hyperion.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProductService {

    private final ProductRepository productRepository = new ProductRepository();

    public void createProduct(
            String name,
            String description,
            BigDecimal price,
            BigDecimal cost,
            int stockQuantity,
            String category,
            String barcode,
            String supplier
    ) {
        String normalizedName = normalize(name);

        validateProduct(normalizedName, price, cost, stockQuantity);

        productRepository.save(new Product(
                normalizedName,
                normalize(description),
                price,
                cost,
                stockQuantity,
                normalize(category),
                normalize(barcode),
                normalize(supplier)
        ));
    }

    public void updateProduct(Product product) {
        if (product.getId() == null) {
            throw new IllegalArgumentException("Produto inválido para atualização.");
        }

        validateProduct(product.getName(), product.getPrice(), product.getCost(), product.getStockQuantity());
        productRepository.update(product);
    }

    public void deactivateProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Produto inválido para desativação.");
        }

        productRepository.deactivate(id);
    }

    public void reactivateProduct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Produto inválido para reativação.");
        }

        productRepository.reactivate(id);
    }

    public Optional<Product> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        return productRepository.findById(id);
    }

    public List<Product> listActiveProducts() {
        return productRepository.findAllActive();
    }

    public List<Product> listInactiveProducts() {
        return productRepository.findAllInactive();
    }

    public List<Product> searchActiveProducts(String term) {
        String normalizedTerm = normalize(term);

        if (normalizedTerm.isBlank()) {
            return listActiveProducts();
        }

        return productRepository.searchActive(normalizedTerm);
    }

    public List<Product> searchInactiveProducts(String term) {
        String normalizedTerm = normalize(term);

        if (normalizedTerm.isBlank()) {
            return listInactiveProducts();
        }

        return productRepository.searchInactive(normalizedTerm);
    }

    private void validateProduct(String name, BigDecimal price, BigDecimal cost, int stockQuantity) {
        if (normalize(name).isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto.");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Informe um preço válido.");
        }

        if (cost == null || cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Informe um custo válido.");
        }

        if (stockQuantity < 0) {
            throw new IllegalArgumentException("O estoque não pode ser negativo.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
