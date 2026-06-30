package com.hyperion.controller;

import com.hyperion.model.Customer;
import com.hyperion.model.CreditSalePlan;
import com.hyperion.model.Product;
import com.hyperion.model.SaleItem;
import com.hyperion.service.CustomerService;
import com.hyperion.service.ProductService;
import com.hyperion.service.SaleService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SaleController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final CustomerService customerService = new CustomerService();
    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();
    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();

    @FXML
    private ChoiceBox<Customer> customerChoiceBox;

    @FXML
    private ChoiceBox<Product> productChoiceBox;

    @FXML
    private ChoiceBox<String> paymentMethodChoiceBox;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField discountField;

    @FXML
    private TextField installmentsField;

    @FXML
    private DatePicker firstDueDatePicker;

    @FXML
    private TableView<SaleItem> cartTable;

    @FXML
    private TableColumn<SaleItem, String> productColumn;

    @FXML
    private TableColumn<SaleItem, String> quantityColumn;

    @FXML
    private TableColumn<SaleItem, String> unitPriceColumn;

    @FXML
    private TableColumn<SaleItem, String> subtotalColumn;

    @FXML
    private Label subtotalValueLabel;

    @FXML
    private Label totalValueLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureChoiceBoxes();
        configureTableColumns();
        loadCustomers();
        loadProducts();
        cartTable.setItems(cartItems);
        discountField.textProperty().addListener((observable, oldValue, newValue) -> updateTotals());
        updateTotals();
    }

    @FXML
    private void handleAddItem() {
        Product product = productChoiceBox.getValue();

        if (product == null) {
            showMessage("Selecione um produto.");
            return;
        }

        try {
            int quantity = parseQuantity(quantityField.getText());
            SaleItem item = new SaleItem(product.getId(), product.getName(), quantity, product.getPrice());
            cartItems.add(item);
            quantityField.clear();
            updateTotals();
            showMessage("Produto adicionado ao carrinho.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleRemoveItem() {
        SaleItem selectedItem = cartTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showMessage("Selecione um item para remover.");
            return;
        }

        cartItems.remove(selectedItem);
        updateTotals();
        showMessage("Item removido do carrinho.");
    }

    @FXML
    private void handleFinishSale() {
        try {
            saleService.finishSale(
                    customerChoiceBox.getValue(),
                    List.copyOf(cartItems),
                    parseMoney(discountField.getText()),
                    paymentMethodChoiceBox.getValue(),
                    buildCreditSalePlan()
            );

            clearSale();
            loadProducts();
            showMessage("Venda finalizada com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureChoiceBoxes() {
        customerChoiceBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName();
            }

            @Override
            public Customer fromString(String value) {
                return null;
            }
        });

        productChoiceBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Product product) {
                if (product == null) {
                    return "";
                }

                return product.getName() + " - estoque: " + product.getStockQuantity();
            }

            @Override
            public Product fromString(String value) {
                return null;
            }
        });

        paymentMethodChoiceBox.setItems(FXCollections.observableArrayList(
                "Dinheiro",
                "PIX",
                "Cartão crédito",
                "Cartão débito",
                "Crediário"
        ));
        paymentMethodChoiceBox.setValue("Dinheiro");
        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, paymentMethod) -> {
            boolean isCreditSale = "Crediário".equals(paymentMethod);
            installmentsField.setVisible(isCreditSale);
            installmentsField.setManaged(isCreditSale);
            firstDueDatePicker.setVisible(isCreditSale);
            firstDueDatePicker.setManaged(isCreditSale);
        });
    }

    private void configureTableColumns() {
        productColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getProductName()));
        quantityColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getQuantity())));
        unitPriceColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getUnitPrice())));
        subtotalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getSubtotal())));
    }

    private void loadCustomers() {
        customerChoiceBox.setItems(FXCollections.observableArrayList(customerService.listActiveCustomers()));
    }

    private void loadProducts() {
        productChoiceBox.setItems(FXCollections.observableArrayList(productService.listActiveProducts()));
    }

    private void updateTotals() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal discount = parseMoneyOrZero(discountField.getText());
        BigDecimal total = subtotal.subtract(discount);

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        subtotalValueLabel.setText(formatMoney(subtotal));
        totalValueLabel.setText(formatMoney(total));
    }

    private BigDecimal calculateSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (SaleItem item : cartItems) {
            subtotal = subtotal.add(item.getSubtotal());
        }

        return subtotal;
    }

    private int parseQuantity(String value) {
        String normalizedValue = value == null ? "" : value.trim();

        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException("Informe a quantidade.");
        }

        try {
            int quantity = Integer.parseInt(normalizedValue);

            if (quantity <= 0) {
                throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
            }

            return quantity;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe uma quantidade válida.");
        }
    }

    private BigDecimal parseMoney(String value) {
        String normalizedValue = value == null ? "" : value.replace(",", ".").trim();

        if (normalizedValue.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe um desconto válido.");
        }
    }

    private CreditSalePlan buildCreditSalePlan() {
        if (!"Crediário".equals(paymentMethodChoiceBox.getValue())) {
            return null;
        }

        return new CreditSalePlan(
                parseInstallments(installmentsField.getText()),
                firstDueDatePicker.getValue()
        );
    }

    private int parseInstallments(String value) {
        String normalizedValue = value == null ? "" : value.trim();

        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException("Informe a quantidade de parcelas.");
        }

        try {
            int installments = Integer.parseInt(normalizedValue);

            if (installments <= 0) {
                throw new IllegalArgumentException("A quantidade de parcelas deve ser maior que zero.");
            }

            return installments;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe uma quantidade válida de parcelas.");
        }
    }

    private BigDecimal parseMoneyOrZero(String value) {
        try {
            return parseMoney(value);
        } catch (IllegalArgumentException exception) {
            return BigDecimal.ZERO;
        }
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private void clearSale() {
        cartItems.clear();
        quantityField.clear();
        discountField.clear();
        installmentsField.clear();
        firstDueDatePicker.setValue(null);
        updateTotals();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
