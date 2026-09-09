package com.hyperion.model;

import java.math.BigDecimal;

public class SaleItem {

    private Long id;
    private Long saleId;
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private BigDecimal subtotal;
    private BigDecimal netSubtotal;
    private String productCategory;
    private String productSupplier;

    public SaleItem(Long productId, String productName, int quantity, BigDecimal unitPrice) {
        this(productId, productName, quantity, unitPrice, BigDecimal.ZERO);
    }

    public SaleItem(Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal unitCost) {
        this(productId, productName, quantity, unitPrice, unitCost, unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public SaleItem(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            String productCategory,
            String productSupplier
    ) {
        this(productId, productName, quantity, unitPrice, unitCost,
                unitPrice.multiply(BigDecimal.valueOf(quantity)), productCategory, productSupplier);
    }

    public SaleItem(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            BigDecimal netSubtotal
    ) {
        this(productId, productName, quantity, unitPrice, unitCost, netSubtotal, null, null);
    }

    public SaleItem(
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            BigDecimal netSubtotal,
            String productCategory,
            String productSupplier
    ) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitCost = unitCost;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.netSubtotal = netSubtotal;
        this.productCategory = productCategory;
        this.productSupplier = productSupplier;
    }

    public SaleItem(
            Long id,
            Long saleId,
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
        this(id, saleId, productId, productName, quantity, unitPrice, BigDecimal.ZERO, subtotal, subtotal);
    }

    public SaleItem(
            Long id,
            Long saleId,
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            BigDecimal subtotal
    ) {
        this(id, saleId, productId, productName, quantity, unitPrice, unitCost, subtotal, subtotal);
    }

    public SaleItem(
            Long id,
            Long saleId,
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            BigDecimal subtotal,
            BigDecimal netSubtotal
    ) {
        this(id, saleId, productId, productName, quantity, unitPrice, unitCost, subtotal, netSubtotal, null, null);
    }

    public SaleItem(
            Long id,
            Long saleId,
            Long productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal unitCost,
            BigDecimal subtotal,
            BigDecimal netSubtotal,
            String productCategory,
            String productSupplier
    ) {
        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitCost = unitCost;
        this.subtotal = subtotal;
        this.netSubtotal = netSubtotal;
        this.productCategory = productCategory;
        this.productSupplier = productSupplier;
    }

    public Long getId() {
        return id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getNetSubtotal() {
        return netSubtotal;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public String getProductSupplier() {
        return productSupplier;
    }
}
