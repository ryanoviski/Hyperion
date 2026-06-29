package com.hyperion.service;

import com.hyperion.model.Product;
import com.hyperion.model.StockMovement;
import com.hyperion.repository.ProductRepository;
import com.hyperion.repository.StockMovementRepository;

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
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Produto invalido para movimentacao."));

        if (quantity <= 0) {
            throw new IllegalArgumentException("Informe uma quantidade maior que zero.");
        }

        if (MOVEMENT_TYPE_OUT.equals(type) && product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Estoque insuficiente para saida.");
        }

        int stockDelta = MOVEMENT_TYPE_IN.equals(type) ? quantity : -quantity;
        StockMovement movement = new StockMovement(productId, type, quantity, normalize(notes));
        stockMovementRepository.registerMovement(movement, stockDelta);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
