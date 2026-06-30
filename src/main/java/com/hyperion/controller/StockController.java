package com.hyperion.controller;

import com.hyperion.model.Product;
import com.hyperion.model.StockMovement;
import com.hyperion.service.ProductService;
import com.hyperion.service.StockService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class StockController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductService productService = new ProductService();
    private final StockService stockService = new StockService();

    @FXML
    private ChoiceBox<Product> productChoiceBox;

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
        configureProductChoiceBox();
        configureTableColumns();
        loadProducts();
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
        loadProducts();
        loadMovements();
        showMessage("Estoque atualizado.");
    }

    private void registerMovement(String type) {
        Product selectedProduct = productChoiceBox.getValue();

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
            loadProducts();
            loadMovements();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void configureProductChoiceBox() {
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
    }

    private void configureTableColumns() {
        dateColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                cellData.getValue().getCreatedAt().format(DATE_TIME_FORMAT)
        ));
        productColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getProductName()));
        typeColumn.setCellValueFactory(cellData -> {
            String type = StockService.MOVEMENT_TYPE_IN.equals(cellData.getValue().getType()) ? "Entrada" : "Saída";
            return new ReadOnlyStringWrapper(type);
        });
        quantityColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(String.valueOf(cellData.getValue().getQuantity())));
        notesColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getNotes()));
    }

    private void loadProducts() {
        List<Product> products = productService.listActiveProducts();
        productChoiceBox.setItems(FXCollections.observableArrayList(products));
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
