package com.hyperion.controller;

import com.hyperion.model.Product;
import com.hyperion.service.ProductService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProductController {

    private static final String ACTIVE_FILTER = "Ativos";
    private static final String INACTIVE_FILTER = "Inativos";
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private static final String EDIT_ICON = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm17.71-10.04c.39-.39.39-1.02 0-1.41l-2.51-2.51a.9959.9959 0 0 0-1.41 0l-1.96 1.96L18.75 9.17l1.96-1.96z";
    private static final String HIDE_ICON = "M12 6.5c3.79 0 6.17 2.13 7.44 3.76-.45.58-1.03 1.22-1.76 1.82L19.1 13.5c1.15-.94 2.02-2.1 2.54-3.01.18-.31.18-.69 0-1C20.62 7.73 17.46 4.5 12 4.5c-1.39 0-2.61.21-3.67.55l1.62 1.62c.64-.11 1.32-.17 2.05-.17zM2.71 3.16 1.39 4.48l3.1 3.1c-.9.77-1.6 1.62-2.13 2.41-.2.3-.2.7 0 1C3.38 12.76 6.54 16 12 16c1.27 0 2.41-.18 3.42-.48l3.1 3.09 1.32-1.32L2.71 3.16zM12 14c-3.79 0-6.17-2.13-7.44-3.76.35-.45.8-.94 1.34-1.4l1.45 1.45c-.03.17-.05.34-.05.51 0 1.49 1.21 2.7 2.7 2.7.17 0 .34-.02.51-.05l1.5 1.5H12zm.67-2.07-3.6-3.6c.31-.15.66-.23 1.03-.23 1.49 0 2.7 1.21 2.7 2.7 0 .4-.05.77-.13 1.13z";
    private static final String SHOW_ICON = "M12 4.5c-5.46 0-8.62 3.23-9.64 4.99-.18.31-.18.69 0 1C3.38 12.27 6.54 15.5 12 15.5s8.62-3.23 9.64-5.01c.18-.31.18-.69 0-1C20.62 7.73 17.46 4.5 12 4.5zm0 9c-3.79 0-6.17-2.13-7.44-3.76C5.83 8.11 8.21 6.5 12 6.5s6.17 1.61 7.44 3.24C18.17 11.37 15.79 13.5 12 13.5zm0-5.5c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";

    private final ProductService productService = new ProductService();

    @FXML
    private TextField searchField;

    @FXML
    private ChoiceBox<String> statusFilterChoiceBox;

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
    private TableColumn<Product, String> statusColumn;

    @FXML
    private TableColumn<Product, Product> actionsColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureFilter();
        configureTableColumns();
        loadProducts();
    }

    @FXML
    private void handleSearch() {
        loadProducts();
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

                statusFilterChoiceBox.setValue(ACTIVE_FILTER);
                loadProducts();
                showMessage("Produto cadastrado: " + formData.name() + ".");
            });
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void handleEditProduct(Product selectedProduct) {
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

    private void handleDeactivateProduct(Product selectedProduct) {
        try {
            productService.deactivateProduct(selectedProduct.getId());
            loadProducts();
            showMessage("Produto desativado: " + selectedProduct.getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void handleReactivateProduct(Product selectedProduct) {
        try {
            productService.reactivateProduct(selectedProduct.getId());
            loadProducts();
            showMessage("Produto reativado: " + selectedProduct.getName() + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureFilter() {
        statusFilterChoiceBox.setItems(FXCollections.observableArrayList(ACTIVE_FILTER, INACTIVE_FILTER));
        statusFilterChoiceBox.setValue(ACTIVE_FILTER);
        statusFilterChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> loadProducts());
    }

    private void configureTableColumns() {
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        nameColumn.getStyleClass().add("left-aligned-column");
        categoryColumn.getStyleClass().add("left-aligned-column");
        supplierColumn.getStyleClass().add("left-aligned-column");

        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getName())));
        categoryColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getCategory())));
        priceColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getPrice())));
        costColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMoney(cellData.getValue().getCost())));
        stockColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getStockQuantity())));
        barcodeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getBarcode())));
        supplierColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(textValue(cellData.getValue().getSupplier())));
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().isActive() ? "Ativo" : "Inativo";
            return new ReadOnlyStringWrapper(status);
        });
        statusColumn.setCellFactory(column -> createStatusCell());

        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        actionsColumn.setCellFactory(column -> createActionsCell());
    }

    private TableCell<Product, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(status);
                badge.getStyleClass().addAll("status-badge", "Ativo".equals(status) ? "status-active" : "status-inactive");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private TableCell<Product, Product> createActionsCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);

                if (empty || product == null) {
                    setGraphic(null);
                    return;
                }

                Button editButton = createIconButton("Editar", EDIT_ICON, () -> handleEditProduct(product));
                Button activeToggleButton = product.isActive()
                        ? createIconButton("Desativar", HIDE_ICON, () -> handleDeactivateProduct(product))
                        : createIconButton("Reativar", SHOW_ICON, () -> handleReactivateProduct(product));

                HBox actions = new HBox(8, editButton, activeToggleButton);
                actions.setAlignment(Pos.CENTER);
                setGraphic(actions);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private Button createIconButton(String tooltipText, String svgContent, Runnable action) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        icon.getStyleClass().add("action-icon-shape");

        Button button = new Button();
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(tooltipText));
        button.setMinSize(36, 36);
        button.setPrefSize(36, 36);
        button.setMaxSize(36, 36);
        button.getStyleClass().add("action-icon-button");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void loadProducts() {
        boolean showingActive = isShowingActive();
        String searchTerm = searchField.getText();
        List<Product> products = showingActive
                ? productService.searchActiveProducts(searchTerm)
                : productService.searchInactiveProducts(searchTerm);

        productsTable.setItems(FXCollections.observableArrayList(products));
        showMessage(products.size() + (showingActive ? " produto(s) ativo(s)." : " produto(s) inativo(s)."));
    }

    private boolean isShowingActive() {
        return !INACTIVE_FILTER.equals(statusFilterChoiceBox.getValue());
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }

    private Optional<ProductFormData> showProductDialog(String title, Product product) {
        Dialog<ProductFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(productsTable.getScene().getWindow());
        addDialogStyles(dialog);

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

    private void addDialogStyles(Dialog<?> dialog) {
        String stylesheet = ProductController.class.getResource("/css/app.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
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
