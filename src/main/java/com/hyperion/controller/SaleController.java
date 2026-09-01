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
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SaleController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final ObservableList<SaleItem> CART_ITEMS = FXCollections.observableArrayList();
    private static Customer draftCustomer;
    private static Product draftProduct;
    private static String draftDiscount = "";
    private static String draftPaymentMethod = "Dinheiro";
    private static Integer draftInstallments = 1;
    private static LocalDate draftFirstDueDate;

    private final CustomerService customerService = new CustomerService();
    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();

    private Customer selectedCustomer;
    private Product selectedProduct;

    @FXML
    private TextField selectedCustomerField;

    @FXML
    private TextField selectedProductField;

    @FXML
    private ChoiceBox<String> paymentMethodChoiceBox;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField discountField;

    @FXML
    private ChoiceBox<Integer> installmentsChoiceBox;

    @FXML
    private DatePicker firstDueDatePicker;

    @FXML
    private VBox installmentsGroup;

    @FXML
    private VBox firstDueDateGroup;

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
        configureSelectedFields();
        cartTable.setItems(CART_ITEMS);
        restoreDraft();
        configureDraftPersistence();
        updateTotals();
    }

    @FXML
    private void handleSearchCustomer() {
        Optional<Customer> customer = showCustomerSearchDialog();

        customer.ifPresent(selected -> {
            selectedCustomer = selected;
            draftCustomer = selected;
            selectedCustomerField.setText(formatCustomer(selected));
            showMessage("Cliente selecionado: " + selected.getName() + ".");
        });
    }

    @FXML
    private void handleSearchProduct() {
        Optional<Product> product = showProductSearchDialog();

        product.ifPresent(selected -> {
            selectedProduct = selected;
            draftProduct = selected;
            selectedProductField.setText(formatProduct(selected));
            showMessage("Produto selecionado: " + selected.getName() + ".");
        });
    }

    @FXML
    private void handleAddItem() {
        if (selectedProduct == null) {
            showMessage("Selecione um produto.");
            return;
        }

        try {
            int quantity = parseQuantity(quantityField.getText());
            SaleItem item = new SaleItem(
                    selectedProduct.getId(),
                    selectedProduct.getName(),
                    quantity,
                    selectedProduct.getPrice()
            );

            CART_ITEMS.add(item);
            selectedProduct = null;
            draftProduct = null;
            selectedProductField.clear();
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

        CART_ITEMS.remove(selectedItem);
        updateTotals();
        showMessage("Item removido do carrinho.");
    }

    @FXML
    private void handleFinishSale() {
        try {
            saleService.finishSale(
                    selectedCustomer,
                    List.copyOf(CART_ITEMS),
                    parseMoney(discountField.getText()),
                    paymentMethodChoiceBox.getValue(),
                    buildCreditSalePlan()
            );

            clearSale();
            showMessage("Venda finalizada com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureChoiceBoxes() {
        paymentMethodChoiceBox.setItems(FXCollections.observableArrayList(
                "Dinheiro",
                "PIX",
                "Cartão crédito",
                "Cartão débito",
                "Crediário"
        ));

        installmentsChoiceBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        installmentsChoiceBox.setValue(1);

        paymentMethodChoiceBox.setValue("Dinheiro");
        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, paymentMethod) ->
                updateCreditSaleFieldsVisibility(paymentMethod)
        );
        updateCreditSaleFieldsVisibility(paymentMethodChoiceBox.getValue());
    }

    private void updateCreditSaleFieldsVisibility(String paymentMethod) {
        boolean isCreditSale = "Crediário".equals(paymentMethod);
        installmentsGroup.setVisible(isCreditSale);
        installmentsGroup.setManaged(isCreditSale);
        installmentsChoiceBox.setVisible(isCreditSale);
        installmentsChoiceBox.setManaged(isCreditSale);
        firstDueDateGroup.setVisible(isCreditSale);
        firstDueDateGroup.setManaged(isCreditSale);
        firstDueDatePicker.setVisible(isCreditSale);
        firstDueDatePicker.setManaged(isCreditSale);
    }

    private void configureTableColumns() {
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        productColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getProductName()));
        quantityColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getQuantity())));
        unitPriceColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getUnitPrice())));
        subtotalColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getSubtotal())));
    }

    private void configureSelectedFields() {
        selectedCustomerField.setEditable(false);
        selectedCustomerField.setFocusTraversable(false);
        selectedProductField.setEditable(false);
        selectedProductField.setFocusTraversable(false);
    }

    private void restoreDraft() {
        selectedCustomer = draftCustomer;
        selectedProduct = draftProduct;

        if (selectedCustomer != null) {
            selectedCustomerField.setText(formatCustomer(selectedCustomer));
        }

        if (selectedProduct != null) {
            selectedProductField.setText(formatProduct(selectedProduct));
        }

        discountField.setText(draftDiscount);
        paymentMethodChoiceBox.setValue(draftPaymentMethod);
        installmentsChoiceBox.setValue(draftInstallments);
        firstDueDatePicker.setValue(draftFirstDueDate);
    }

    private void configureDraftPersistence() {
        discountField.textProperty().addListener((observable, oldValue, newValue) -> {
            draftDiscount = textValue(newValue);
            updateTotals();
        });

        paymentMethodChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, paymentMethod) ->
                draftPaymentMethod = paymentMethod
        );

        installmentsChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, installments) ->
                draftInstallments = installments
        );

        firstDueDatePicker.valueProperty().addListener((observable, oldValue, dueDate) ->
                draftFirstDueDate = dueDate
        );
    }

    private Optional<Customer> showCustomerSearchDialog() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Buscar cliente");
        dialog.setHeaderText(null);
        dialog.initOwner(cartTable.getScene().getWindow());
        addDialogStyles(dialog);

        ButtonType selectButtonType = new ButtonType("Selecionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar por nome, CPF/CNPJ, telefone ou email");
        searchField.setMaxWidth(Double.MAX_VALUE);

        TableView<Customer> table = createCustomerSearchTable();
        table.setItems(FXCollections.observableArrayList(customerService.listActiveCustomers()));
        searchField.textProperty().addListener((observable, oldValue, term) ->
                table.setItems(FXCollections.observableArrayList(customerService.searchActiveCustomers(term)))
        );

        Node selectButton = dialog.getDialogPane().lookupButton(selectButtonType);
        selectButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        VBox content = new VBox(12, searchField, table);
        content.getStyleClass().add("dialog-content");
        content.setPrefWidth(720);
        content.setPrefHeight(440);
        content.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != selectButtonType) {
                return null;
            }

            return table.getSelectionModel().getSelectedItem();
        });

        return dialog.showAndWait();
    }

    private Optional<Product> showProductSearchDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Buscar produto");
        dialog.setHeaderText(null);
        dialog.initOwner(cartTable.getScene().getWindow());
        addDialogStyles(dialog);

        ButtonType selectButtonType = new ButtonType("Selecionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar por nome, categoria, código ou fornecedor");
        searchField.setMaxWidth(Double.MAX_VALUE);

        TableView<Product> table = createProductSearchTable();
        table.setItems(FXCollections.observableArrayList(productService.listActiveProducts()));
        searchField.textProperty().addListener((observable, oldValue, term) ->
                table.setItems(FXCollections.observableArrayList(productService.searchActiveProducts(term)))
        );

        Node selectButton = dialog.getDialogPane().lookupButton(selectButtonType);
        selectButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        VBox content = new VBox(12, searchField, table);
        content.getStyleClass().add("dialog-content");
        content.setPrefWidth(780);
        content.setPrefHeight(460);
        content.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != selectButtonType) {
                return null;
            }

            return table.getSelectionModel().getSelectedItem();
        });

        return dialog.showAndWait();
    }

    private TableView<Customer> createCustomerSearchTable() {
        TableView<Customer> table = new TableView<>();
        table.setPrefHeight(360);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Customer, String> nameColumn = new TableColumn<>("Cliente");
        nameColumn.setPrefWidth(240);
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));

        TableColumn<Customer, String> documentColumn = new TableColumn<>("Documento");
        documentColumn.setPrefWidth(160);
        documentColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getDocument())));

        TableColumn<Customer, String> phoneColumn = new TableColumn<>("Telefone");
        phoneColumn.setPrefWidth(140);
        phoneColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getPhone())));

        TableColumn<Customer, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setPrefWidth(180);
        emailColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getEmail())));

        table.getColumns().addAll(nameColumn, documentColumn, phoneColumn, emailColumn);
        return table;
    }

    private TableView<Product> createProductSearchTable() {
        TableView<Product> table = new TableView<>();
        table.setPrefHeight(380);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Product, String> nameColumn = new TableColumn<>("Produto");
        nameColumn.setPrefWidth(240);
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));

        TableColumn<Product, String> barcodeColumn = new TableColumn<>("Código");
        barcodeColumn.setPrefWidth(140);
        barcodeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getBarcode())));

        TableColumn<Product, String> stockColumn = new TableColumn<>("Estoque");
        stockColumn.setPrefWidth(100);
        stockColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getStockQuantity())));

        TableColumn<Product, String> priceColumn = new TableColumn<>("Preço");
        priceColumn.setPrefWidth(120);
        priceColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getPrice())));

        TableColumn<Product, String> supplierColumn = new TableColumn<>("Fornecedor");
        supplierColumn.setPrefWidth(180);
        supplierColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getSupplier())));

        table.getColumns().addAll(nameColumn, barcodeColumn, stockColumn, priceColumn, supplierColumn);
        return table;
    }

    private void addDialogStyles(Dialog<?> dialog) {
        String stylesheet = SaleController.class.getResource("/css/app.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
    }

    private String formatCustomer(Customer customer) {
        String document = textValue(customer.getDocument());

        if (document.isBlank()) {
            return customer.getName();
        }

        return customer.getName() + " - " + document;
    }

    private String formatProduct(Product product) {
        return product.getName()
                + " - estoque: "
                + product.getStockQuantity()
                + " - "
                + formatMoney(product.getPrice());
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

        for (SaleItem item : CART_ITEMS) {
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
                parseInstallments(installmentsChoiceBox.getValue()),
                firstDueDatePicker.getValue()
        );
    }

    private int parseInstallments(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Informe a quantidade de parcelas.");
        }

        if (value < 1 || value > 10) {
            throw new IllegalArgumentException("Selecione de 1 a 10 parcelas.");
        }

        return value;
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
        CART_ITEMS.clear();
        selectedCustomer = null;
        selectedProduct = null;
        draftCustomer = null;
        draftProduct = null;
        draftDiscount = "";
        draftPaymentMethod = "Dinheiro";
        draftInstallments = 1;
        draftFirstDueDate = null;
        selectedCustomerField.clear();
        selectedProductField.clear();
        quantityField.clear();
        discountField.clear();
        paymentMethodChoiceBox.setValue("Dinheiro");
        installmentsChoiceBox.setValue(1);
        firstDueDatePicker.setValue(null);
        updateTotals();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    private String textValue(String value) {
        return value == null ? "" : value;
    }
}
