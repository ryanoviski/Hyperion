package com.hyperion.service;

import com.hyperion.model.Customer;
import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.DailySalesSummary;
import com.hyperion.model.Product;
import com.hyperion.model.Sale;
import com.hyperion.model.SaleItem;
import com.hyperion.repository.CustomerRepository;
import com.hyperion.repository.ProductRepository;
import com.hyperion.repository.SaleRepository;
import com.hyperion.exception.EntityInactiveException;
import com.hyperion.exception.EntityNotFoundException;
import com.hyperion.exception.InsufficientStockException;
import com.hyperion.exception.InvalidCreditPlanException;
import com.hyperion.exception.InvalidPaymentMethodException;
import com.hyperion.exception.ValidationException;
import com.hyperion.util.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SaleService {

    private static final Set<String> PAYMENT_METHODS = Set.of("Dinheiro", "PIX", "Cartão crédito", "Cartão débito", "Crediário");
    private static final int MAX_CREDIT_INSTALLMENTS = 10;

    private final CustomerRepository customerRepository = new CustomerRepository();
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

        Customer persistedCustomer = customerRepository.findById(customer.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente"));
        if (!persistedCustomer.isActive()) {
            throw new EntityInactiveException("Cliente");
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

        if (normalizedDiscount.compareTo(BigDecimal.ZERO) < 0 || !Money.hasAtMostTwoFractionDigits(normalizedDiscount)) {
            throw new ValidationException("O desconto deve ter valor não negativo e no máximo duas casas decimais.");
        }

        List<SaleItem> validatedItems = validateItems(items);
        BigDecimal subtotal = calculateSubtotal(validatedItems);

        if (normalizedDiscount.compareTo(subtotal) > 0) {
            throw new ValidationException("O desconto não pode ser maior que o subtotal.");
        }

        BigDecimal total = subtotal.subtract(normalizedDiscount);
        if (!Money.fitsInCents(subtotal) || !Money.fitsInCents(total)) {
            throw new ValidationException("O valor total da venda excede o limite suportado.");
        }
        CreditSalePlan validatedCreditSalePlan = validateCreditSalePlan(normalizedPaymentMethod, creditSalePlan);
        if ("Crediário".equals(normalizedPaymentMethod) && total.signum() <= 0) {
            throw new InvalidCreditPlanException("A venda no crediário deve ter valor total maior que zero.");
        }

        Sale sale = new Sale(
                persistedCustomer.getId(),
                persistedCustomer.getName(),
                subtotal,
                normalizedDiscount,
                total,
                normalizedPaymentMethod,
                allocateNetSubtotals(validatedItems, total)
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

    public List<Sale> listRecentSalesForManagement(int limit) {
        return saleRepository.findLatestIncludingCancelled(limit);
    }

    public void cancelSale(Sale sale, String reason) {
        if (sale == null || sale.getId() == null) {
            throw new ValidationException("Selecione uma venda para cancelar.");
        }

        String normalizedReason = normalize(reason);
        if (normalizedReason.isBlank()) {
            throw new ValidationException("Informe o motivo do cancelamento.");
        }

        saleRepository.cancel(sale.getId(), normalizedReason);
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
                    product.getPrice(),
                    product.getCost(),
                    product.getCategory(),
                    product.getSupplier()
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

    private List<SaleItem> allocateNetSubtotals(List<SaleItem> items, BigDecimal total) {
        List<BigDecimal> allocations = Money.allocateProportionally(
                total,
                items.stream().map(SaleItem::getSubtotal).toList()
        );
        List<SaleItem> allocatedItems = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            SaleItem item = items.get(index);
            allocatedItems.add(new SaleItem(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getUnitCost(),
                    allocations.get(index),
                    item.getProductCategory(),
                    item.getProductSupplier()
            ));
        }
        return allocatedItems;
    }

    private CreditSalePlan validateCreditSalePlan(String paymentMethod, CreditSalePlan creditSalePlan) {
        if (!"Crediário".equals(paymentMethod)) {
            return null;
        }

        if (creditSalePlan == null) {
            throw new InvalidCreditPlanException("Informe os dados do crediário.");
        }

        if (creditSalePlan.getInstallments() <= 0 || creditSalePlan.getInstallments() > MAX_CREDIT_INSTALLMENTS) {
            throw new InvalidCreditPlanException("Selecione de 1 a " + MAX_CREDIT_INSTALLMENTS + " parcelas.");
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
