package com.hyperion.service;

import com.hyperion.model.Product;
import com.hyperion.model.StockMovement;
import com.hyperion.repository.ProductRepository;
import com.hyperion.repository.StockMovementRepository;
import com.hyperion.exception.EntityInactiveException;
import com.hyperion.exception.EntityNotFoundException;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.ValidationException;

import java.util.List;

public class StockService {

    public static final String MOVEMENT_TYPE_IN = "IN";
    public static final String MOVEMENT_TYPE_OUT = "OUT";

    private final ProductRepository productRepository = new ProductRepository();
    private final StockMovementRepository stockMovementRepository = new StockMovementRepository();

    public void registerEntry(Long productId, int quantity, String notes) {
        registerMovement(productId, MOVEMENT_TYPE_IN, quantity, notes);
    }

    public void registerExit(Long productId, int quantity, String notes) {
        registerMovement(productId, MOVEMENT_TYPE_OUT, quantity, notes);
    }

    public List<StockMovement> listLatestMovements() {
        return stockMovementRepository.findLatest();
    }

    private void registerMovement(Long productId, String type, int quantity, String notes) {
        if (productId == null) {
            throw new ValidationException("Produto inválido para movimentação.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produto"));
        if (!product.isActive()) {
            throw new EntityInactiveException("Produto");
        }

        if (quantity <= 0) {
            throw new ValidationException("Informe uma quantidade maior que zero.");
        }

        if (MOVEMENT_TYPE_OUT.equals(type) && product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getName());
        }

        int stockDelta = MOVEMENT_TYPE_IN.equals(type) ? quantity : -quantity;
        StockMovement movement = new StockMovement(productId, type, quantity, normalize(notes));
        stockMovementRepository.registerMovement(movement, stockDelta);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
