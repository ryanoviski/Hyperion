package com.hyperion.controller;

import com.hyperion.model.Product;
import com.hyperion.service.ProductService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProductController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final ProductService productService = new ProductService();

    @FXML
    private TextField searchField;

    @FXML
    private Button editProductButton;

    @FXML
    private Button deactivateProductButton;

    @FXML
    private TableView<Product> productsTable;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, String> categoryColumn;

    @FXML
    private TableColumn<Product, String> priceColumn;

    @FXML
    private TableColumn<Product, String> costColumn;

    @FXML
    private TableColumn<Product, String> stockColumn;

    @FXML
    private TableColumn<Product, String> barcodeColumn;

    @FXML
    private TableColumn<Product, String> supplierColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureTableColumns();
        configureSelectionState();
        loadProducts();
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        List<Product> products = productService.searchActiveProducts(searchTerm);
        updateTable(products);
        showMessage(products.size() + " produto(s) encontrado(s).");
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadProducts();
    }

    @FXML
    private void handleNewProduct() {
        try {
            Optional<ProductFormData> result = showProductDialog("Novo produto", null);

            result.ifPresent(formData -> {
                productService.createProduct(
                        formData.name(),
                        formData.description(),
                        formData.price(),
                        formData.cost(),
                        formData.stockQuantity(),
                        formData.category(),
                        formData.barcode(),
                        formData.supplier()
                );

                loadProducts();
                showMessage("Produto cadastrado: " + formData.name() + ".");
            });
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleEditProduct() {
        Product selectedProduct = getSelectedProduct();

        if (selectedProduct == null) {
            showMessage("Selecione um produto para editar.");
            return;
        }

        try {
            Optional<ProductFormData> result = showProductDialog("Editar produto", selectedProduct);

            result.ifPresent(formData -> {
                Product updatedProduct = new Product(
                        selectedProduct.getId(),
                        formData.name(),
                        formData.description(),
                        formData.price(),
                        formData.cost(),
                        formData.stockQuantity(),
                        formData.category(),
                        formData.barcode(),
                        formData.supplier(),
                        selectedProduct.isActive(),
                        selectedProduct.getCreatedAt(),
                        selectedProduct.getUpdatedAt()
                );

                productService.updateProduct(updatedProduct);
                loadProducts();
                showMessage("Produto atualizado: " + formData.name() + ".");
            });
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleDeactivateProduct() {
        Product selectedProduct = getSelectedProduct();

        if (selectedProduct == null) {
            showMessage("Selecione um produto para desativar.");
            return;
        }

        try {
            productService.deactivateProduct(selectedProduct.getId());
            loadProducts();
            showMessage("Produto desativado: " + selectedProduct.getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureTableColumns() {
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        categoryColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCategory()));
        priceColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getPrice())));
        costColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getCost())));
        stockColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getStockQuantity())));
        barcodeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getBarcode()));
        supplierColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSupplier()));
    }

    private void configureSelectionState() {
        editProductButton.setDisable(true);
        deactivateProductButton.setDisable(true);

        productsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedProduct) -> {
            boolean hasSelection = selectedProduct != null;
            editProductButton.setDisable(!hasSelection);
            deactivateProductButton.setDisable(!hasSelection);
        });
    }

    private void loadProducts() {
        List<Product> products = productService.listActiveProducts();
        updateTable(products);
        showMessage(products.size() + " produto(s) ativo(s).");
    }

    private void updateTable(List<Product> products) {
        productsTable.setItems(FXCollections.observableArrayList(products));
    }

    private Product getSelectedProduct() {
        return productsTable.getSelectionModel().getSelectedItem();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    private Optional<ProductFormData> showProductDialog(String title, Product product) {
        Dialog<ProductFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(productsTable.getScene().getWindow());

        ButtonType saveButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(createProductForm(product));

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(productNameField.textProperty().isEmpty());

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            return new ProductFormData(
                    productNameField.getText(),
                    productDescriptionArea.getText(),
                    parseMoney(productPriceField.getText()),
                    parseMoney(productCostField.getText()),
                    parseStock(productStockField.getText()),
                    productCategoryField.getText(),
                    productBarcodeField.getText(),
                    productSupplierField.getText()
            );
        });

        return dialog.showAndWait();
    }

    private GridPane createProductForm(Product product) {
        productNameField = new TextField();
        productPriceField = new TextField();
        productCostField = new TextField();
        productStockField = new TextField();
        productCategoryField = new TextField();
        productBarcodeField = new TextField();
        productSupplierField = new TextField();
        productDescriptionArea = new TextArea();

        productNameField.setPromptText("Nome do produto");
        productPriceField.setPromptText("R$ 0,00");
        productCostField.setPromptText("R$ 0,00");
        productStockField.setPromptText("0");
        productCategoryField.setPromptText("Categoria");
        productBarcodeField.setPromptText("Código de barras");
        productSupplierField.setPromptText("Fornecedor");
        productDescriptionArea.setPromptText("Descrição");
        productDescriptionArea.setPrefRowCount(3);

        configureMoneyField(productPriceField);
        configureMoneyField(productCostField);

        if (product != null) {
            productNameField.setText(textValue(product.getName()));
            productDescriptionArea.setText(textValue(product.getDescription()));
            productPriceField.setText(formatMoney(product.getPrice()));
            productCostField.setText(formatMoney(product.getCost()));
            productStockField.setText(String.valueOf(product.getStockQuantity()));
            productCategoryField.setText(textValue(product.getCategory()));
            productBarcodeField.setText(textValue(product.getBarcode()));
            productSupplierField.setText(textValue(product.getSupplier()));
        }

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(16));
        form.setPrefWidth(560);
        form.getColumnConstraints().addAll(createLabelColumn(), createFieldColumn());

        form.add(new Label("Nome"), 0, 0);
        form.add(productNameField, 1, 0);
        form.add(new Label("Preço"), 0, 1);
        form.add(productPriceField, 1, 1);
        form.add(new Label("Custo"), 0, 2);
        form.add(productCostField, 1, 2);
        form.add(new Label("Estoque"), 0, 3);
        form.add(productStockField, 1, 3);
        form.add(new Label("Categoria"), 0, 4);
        form.add(productCategoryField, 1, 4);
        form.add(new Label("Código"), 0, 5);
        form.add(productBarcodeField, 1, 5);
        form.add(new Label("Fornecedor"), 0, 6);
        form.add(productSupplierField, 1, 6);
        form.add(new Label("Descrição"), 0, 7);
        form.add(productDescriptionArea, 1, 7);

        return form;
    }

    private BigDecimal parseMoney(String value) {
        String digits = textValue(value).replaceAll("\\D", "");

        if (digits.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(digits).movePointLeft(2);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe valores monetários válidos.");
        }
    }

    private int parseStock(String value) {
        String normalizedValue = textValue(value).trim();

        if (normalizedValue.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(normalizedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe uma quantidade válida em estoque.");
        }
    }

    private void configureMoneyField(TextField field) {
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            String formattedValue = formatDigitsAsMoney(newValue);

            if (!formattedValue.equals(newValue)) {
                field.setText(formattedValue);
                field.positionCaret(formattedValue.length());
            }
        });
    }

    private String formatDigitsAsMoney(String value) {
        String digits = textValue(value).replaceAll("\\D", "");

        if (digits.isBlank()) {
            digits = "0";
        }

        BigDecimal amount = new BigDecimal(digits).movePointLeft(2);
        return formatMoney(amount);
    }

    private ColumnConstraints createLabelColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(120);
        column.setPrefWidth(120);
        return column;
    }

    private ColumnConstraints createFieldColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(380);
        column.setPrefWidth(380);
        return column;
    }

    private String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private String textValue(String value) {
        return value == null ? "" : value;
    }

    private TextField productNameField;
    private TextField productPriceField;
    private TextField productCostField;
    private TextField productStockField;
    private TextField productCategoryField;
    private TextField productBarcodeField;
    private TextField productSupplierField;
    private TextArea productDescriptionArea;

    private record ProductFormData(
            String name,
            String description,
            BigDecimal price,
            BigDecimal cost,
            int stockQuantity,
            String category,
            String barcode,
            String supplier
    ) {
    }
}
