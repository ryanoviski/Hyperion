package com.hyperion.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MainController {

    private static final String DASHBOARD_VIEW = "/fxml/dashboard-view.fxml";
    private static final String CUSTOMERS_VIEW = "/fxml/customers-view.fxml";
    private static final String PRODUCTS_VIEW = "/fxml/products-view.fxml";
    private static final String STOCK_VIEW = "/fxml/stock-view.fxml";
    private static final String SALES_VIEW = "/fxml/sales-view.fxml";

    private static MainController activeController;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button customersButton;

    @FXML
    private Button productsButton;

    @FXML
    private Button stockButton;

    @FXML
    private Button salesButton;

    @FXML
    private Button financeButton;

    @FXML
    private Button reportsButton;

    @FXML
    private Button settingsButton;

    @FXML
    private StackPane contentContainer;

    @FXML
    private void initialize() {
        activeController = this;
        showDashboard();
    }

    @FXML
    private void handleDashboard() {
        showDashboard();
    }

    @FXML
    private void handleCustomers() {
        showCustomers();
    }

    @FXML
    private void handleProducts() {
        showProducts();
    }

    @FXML
    private void handleStock() {
        showStock();
    }

    @FXML
    private void handleSales() {
        showSales();
    }

    @FXML
    private void handleUnavailableSection() {
        // Temporary placeholder until these modules are implemented.
    }

    public static void openCustomersView() {
        if (activeController != null) {
            activeController.showCustomers();
        }
    }

    public static void openProductsView() {
        if (activeController != null) {
            activeController.showProducts();
        }
    }

    public static void openStockView() {
        if (activeController != null) {
            activeController.showStock();
        }
    }

    public static void openSalesView() {
        if (activeController != null) {
            activeController.showSales();
        }
    }

    private void showDashboard() {
        loadContent(DASHBOARD_VIEW);
        setActiveButton(dashboardButton);
    }

    private void showCustomers() {
        loadContent(CUSTOMERS_VIEW);
        setActiveButton(customersButton);
    }

    private void showProducts() {
        loadContent(PRODUCTS_VIEW);
        setActiveButton(productsButton);
    }

    private void showStock() {
        loadContent(STOCK_VIEW);
        setActiveButton(stockButton);
    }

    private void showSales() {
        loadContent(SALES_VIEW);
        setActiveButton(salesButton);
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent content = FXMLLoader.load(Objects.requireNonNull(
                    MainController.class.getResource(fxmlPath)
            ));

            contentContainer.getChildren().setAll(content);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load content: " + fxmlPath, exception);
        }
    }

    private void setActiveButton(Button activeButton) {
        for (Button button : getSidebarButtons()) {
            button.getStyleClass().setAll("sidebar-button");
        }

        activeButton.getStyleClass().setAll("sidebar-button-active");
    }

    private List<Button> getSidebarButtons() {
        return List.of(
                dashboardButton,
                customersButton,
                productsButton,
                stockButton,
                salesButton,
                financeButton,
                reportsButton,
                settingsButton
        );
    }
}
