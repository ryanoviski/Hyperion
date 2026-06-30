package com.hyperion.service;

import com.hyperion.model.Customer;
import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.DailySalesSummary;
import com.hyperion.model.Product;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;
import com.hyperion.repository.ProductRepository;
import com.hyperion.repository.SaleRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaleService {

    private final ProductRepository productRepository = new ProductRepository();
    private final SaleRepository saleRepository = new SaleRepository();

    public void finishSale(
            Customer customer,
            List<SaleItem> items,
            BigDecimal discount,
            String paymentMethod,
            CreditSalePlan creditSalePlan
    ) {
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException("Selecione um cliente.");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um produto.");
        }

        String normalizedPaymentMethod = normalize(paymentMethod);

        if (normalizedPaymentMethod.isBlank()) {
            throw new IllegalArgumentException("Selecione a forma de pagamento.");
        }

        BigDecimal normalizedDiscount = discount == null ? BigDecimal.ZERO : discount;

        if (normalizedDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O desconto nao pode ser negativo.");
        }

        List<SaleItem> validatedItems = validateItems(items);
        BigDecimal subtotal = calculateSubtotal(validatedItems);

        if (normalizedDiscount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("O desconto nao pode ser maior que o subtotal.");
        }

        BigDecimal total = subtotal.subtract(normalizedDiscount);
        CreditSalePlan validatedCreditSalePlan = validateCreditSalePlan(normalizedPaymentMethod, creditSalePlan);

        Sale sale = new Sale(
                customer.getId(),
                customer.getName(),
                subtotal,
                normalizedDiscount,
                total,
                normalizedPaymentMethod,
                validatedItems
        );

        saleRepository.save(sale, validatedCreditSalePlan);
    }

    public void finishSale(Customer customer, List<SaleItem> items, BigDecimal discount, String paymentMethod) {
        finishSale(customer, items, discount, paymentMethod, null);
    }

    public DailySalesSummary getTodaySummary() {
        return saleRepository.findTodaySummary();
    }

    private List<SaleItem> validateItems(List<SaleItem> items) {
        List<SaleItem> validatedItems = new ArrayList<>();
        Map<Long, Integer> requestedQuantitiesByProduct = new HashMap<>();

        for (SaleItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .filter(Product::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Produto invalido na venda."));

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
            }

            int requestedQuantity = requestedQuantitiesByProduct.getOrDefault(product.getId(), 0) + item.getQuantity();
            requestedQuantitiesByProduct.put(product.getId(), requestedQuantity);

            if (product.getStockQuantity() < requestedQuantity) {
                throw new IllegalArgumentException("Estoque insuficiente para: " + product.getName() + ".");
            }

            validatedItems.add(new SaleItem(
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getPrice()
            ));
        }

        return validatedItems;
    }

    private BigDecimal calculateSubtotal(List<SaleItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (SaleItem item : items) {
            subtotal = subtotal.add(item.getSubtotal());
        }

        return subtotal;
    }

    private CreditSalePlan validateCreditSalePlan(String paymentMethod, CreditSalePlan creditSalePlan) {
        if (!"Crediario".equals(paymentMethod)) {
            return null;
        }

        if (creditSalePlan == null) {
            throw new IllegalArgumentException("Informe os dados do crediario.");
        }

        if (creditSalePlan.getInstallments() <= 0) {
            throw new IllegalArgumentException("Informe uma quantidade valida de parcelas.");
        }

        if (creditSalePlan.getFirstDueDate() == null) {
            throw new IllegalArgumentException("Informe a data de vencimento da primeira parcela.");
        }

        return creditSalePlan;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
