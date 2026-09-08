package com.hyperion.service;

import com.hyperion.model.Customer;
import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.DailySalesSummary;
import com.hyperion.model.Product;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;
import com.hyperion.repository.ProductRepository;
import com.hyperion.repository.SaleRepository;
import com.hyperion.exception.EntityInactiveException;
import com.hyperion.exception.EntityNotFoundException;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.InvalidCreditPlanException;
import com.hyperion.exception.InvalidPaymentMethodException;
import com.hyperion.exception.ValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SaleService {

    private static final Set<String> PAYMENT_METHODS = Set.of("Dinheiro", "PIX", "Cartão crédito", "Cartão débito", "Crediário");

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
            throw new ValidationException("Selecione um cliente.");
        }

        if (items == null || items.isEmpty()) {
            throw new ValidationException("Adicione pelo menos um produto.");
        }

        String normalizedPaymentMethod = normalize(paymentMethod);

        if (normalizedPaymentMethod.isBlank()) {
            throw new InvalidPaymentMethodException();
        }

        if (!PAYMENT_METHODS.contains(normalizedPaymentMethod)) {
            throw new InvalidPaymentMethodException();
        }

        BigDecimal normalizedDiscount = discount == null ? BigDecimal.ZERO : discount;

        if (normalizedDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("O desconto não pode ser negativo.");
        }

        List<SaleItem> validatedItems = validateItems(items);
        BigDecimal subtotal = calculateSubtotal(validatedItems);

        if (normalizedDiscount.compareTo(subtotal) > 0) {
            throw new ValidationException("O desconto não pode ser maior que o subtotal.");
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

    public List<Sale> listLatestSales(int limit) {
        return saleRepository.findLatest(limit);
    }

    public List<Sale> listCustomerPurchases(Long customerId) {
        if (customerId == null) {
            return List.of();
        }

        return saleRepository.findByCustomerId(customerId);
    }

    private List<SaleItem> validateItems(List<SaleItem> items) {
        List<SaleItem> validatedItems = new ArrayList<>();
        Map<Long, Integer> requestedQuantitiesByProduct = new HashMap<>();

        for (SaleItem item : items) {
            if (item == null || item.getProductId() == null) {
                throw new ValidationException("Produto inválido na venda.");
            }
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto"));
            if (!product.isActive()) {
                throw new EntityInactiveException("Produto");
            }

            if (item.getQuantity() <= 0) {
                throw new ValidationException("A quantidade deve ser maior que zero.");
            }

            int requestedQuantity = requestedQuantitiesByProduct.getOrDefault(product.getId(), 0) + item.getQuantity();
            requestedQuantitiesByProduct.put(product.getId(), requestedQuantity);

            if (product.getStockQuantity() < requestedQuantity) {
                throw new InsufficientStockException(product.getName());
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
        if (!"Crediário".equals(paymentMethod)) {
            return null;
        }

        if (creditSalePlan == null) {
            throw new InvalidCreditPlanException("Informe os dados do crediário.");
        }

        if (creditSalePlan.getInstallments() <= 0) {
            throw new InvalidCreditPlanException("Informe uma quantidade válida de parcelas.");
        }

        if (creditSalePlan.getFirstDueDate() == null) {
            throw new InvalidCreditPlanException("Informe a data de vencimento da primeira parcela.");
        }

        return creditSalePlan;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
