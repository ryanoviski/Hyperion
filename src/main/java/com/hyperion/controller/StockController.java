package com.hyperion.controller;

import com.hyperion.model.Product;
import com.hyperion.model.StockMovement;
import com.hyperion.service.ProductService;
import com.hyperion.service.StockService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StockController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final ProductService productService = new ProductService();
    private final StockService stockService = new StockService();

    private Product selectedProduct;

    @FXML
    private TextField selectedProductField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextArea notesArea;

    @FXML
    private TableView<StockMovement> movementsTable;

    @FXML
    private TableColumn<StockMovement, String> dateColumn;

    @FXML
    private TableColumn<StockMovement, String> productColumn;

    @FXML
    private TableColumn<StockMovement, String> typeColumn;

    @FXML
    private TableColumn<StockMovement, String> quantityColumn;

    @FXML
    private TableColumn<StockMovement, String> notesColumn;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        configureTableColumns();
        loadMovements();
    }

    @FXML
    private void handleEntry() {
        registerMovement(StockService.MOVEMENT_TYPE_IN);
    }

    @FXML
    private void handleExit() {
        registerMovement(StockService.MOVEMENT_TYPE_OUT);
    }

    @FXML
    private void handleRefresh() {
        loadMovements();
        showMessage("Estoque atualizado.");
    }

    @FXML
    private void handleSearchProduct() {
        showProductSearchDialog().ifPresent(product -> {
            selectedProduct = product;
            selectedProductField.setText(formatSelectedProduct(product));
            showMessage("Produto selecionado: " + product.getName() + ".");
        });
    }

    private void registerMovement(String type) {
        if (selectedProduct == null) {
            showMessage("Selecione um produto.");
            return;
        }

        try {
            int quantity = parseQuantity(quantityField.getText());

            if (StockService.MOVEMENT_TYPE_IN.equals(type)) {
                stockService.registerEntry(selectedProduct.getId(), quantity, notesArea.getText());
                showMessage("Entrada registrada para " + selectedProduct.getName() + ".");
            } else {
                stockService.registerExit(selectedProduct.getId(), quantity, notesArea.getText());
                showMessage("Saída registrada para " + selectedProduct.getName() + ".");
            }

            clearForm();
            loadMovements();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureTableColumns() {
        movementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                cellData.getValue().getCreatedAt().format(DATE_TIME_FORMAT)
        ));
        productColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getProductName()));
        typeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(formatMovementType(cellData.getValue().getType())));
        typeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                getStyleClass().removeAll("movement-entry", "movement-exit");

                if (empty || type == null) {
                    setText(null);
                    return;
                }

                setText(type);
                getStyleClass().add("Entrada".equals(type) ? "movement-entry" : "movement-exit");
            }
        });
        quantityColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getQuantity())));
        notesColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getNotes()));
    }

    private String formatMovementType(String type) {
        return StockService.MOVEMENT_TYPE_IN.equals(type) ? "Entrada" : "Saída";
    }

    private Optional<Product> showProductSearchDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Buscar produto");
        dialog.setHeaderText(null);
        dialog.initOwner(selectedProductField.getScene().getWindow());
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
        String stylesheet = StockController.class.getResource("/css/app.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
    }

    private String formatSelectedProduct(Product product) {
        return product.getName() + " - estoque: " + product.getStockQuantity();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "R$ 0,00";
        }

        return CURRENCY_FORMAT.format(value);
    }

    private String textValue(String value) {
        return value == null ? "" : value;
    }

    private void loadMovements() {
        List<StockMovement> movements = stockService.listLatestMovements();
        movementsTable.setItems(FXCollections.observableArrayList(movements));
    }

    private int parseQuantity(String value) {
        String normalizedValue = value == null ? "" : value.trim();

        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException("Informe a quantidade.");
        }

        try {
            return Integer.parseInt(normalizedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe uma quantidade válida.");
        }
    }

    private void clearForm() {
        quantityField.clear();
        notesArea.clear();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
