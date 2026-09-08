package com.hyperion.exception;

public class InsufficientStockException extends BusinessRuleException {
    public InsufficientStockException(String productName) { super("Estoque insuficiente para: " + productName + "."); }
}
