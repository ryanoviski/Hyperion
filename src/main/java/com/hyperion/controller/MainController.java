package com.hyperion.controller;

import com.hyperion.exception.ApplicationResourceException;
import com.hyperion.util.ApplicationLogger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class MainController {

    private static final String DASHBOARD_VIEW = "/fxml/dashboard-view.fxml";
    private static final String CUSTOMERS_VIEW = "/fxml/customers-view.fxml";
    private static final String PRODUCTS_VIEW = "/fxml/products-view.fxml";
    private static final String STOCK_VIEW = "/fxml/stock-view.fxml";
    private static final String SALES_VIEW = "/fxml/sales-view.fxml";
    private static final String CREDIT_VIEW = "/fxml/credit-view.fxml";
    private static final String FINANCE_VIEW = "/fxml/finance-view.fxml";
    private static final String REPORTS_VIEW = "/fxml/reports-view.fxml";
    private static final String SETTINGS_VIEW = "/fxml/settings-view.fxml";

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
    private Button creditButton;

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
    private void handleCredit() {
        showCredit();
    }

    @FXML
    private void handleFinance() {
        showFinance();
    }

    @FXML
    private void handleReports() {
        showReports();
    }

    @FXML
    private void handleSettings() {
        showSettings();
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

    public static void openCreditView() {
        if (activeController != null) {
            activeController.showCredit();
        }
    }

    public static void openReportsView() {
        if (activeController != null) {
            activeController.showReports();
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

    private void showCredit() {
        loadContent(CREDIT_VIEW);
        setActiveButton(creditButton);
    }

    private void showFinance() {
        loadContent(FINANCE_VIEW);
        setActiveButton(financeButton);
    }

    private void showReports() {
        loadContent(REPORTS_VIEW);
        setActiveButton(reportsButton);
    }

    private void showSettings() {
        loadContent(SETTINGS_VIEW);
        setActiveButton(settingsButton);
    }

    private void loadContent(String fxmlPath) {
        try {
            var resource = MainController.class.getResource(fxmlPath);
            if (resource == null) {
                throw new ApplicationResourceException("Não foi possível localizar a tela: " + fxmlPath);
            }
            Parent content = FXMLLoader.load(resource);

            if (content instanceof Region region) {
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }

            contentContainer.getChildren().setAll(content);
        } catch (IOException | RuntimeException exception) {
            ApplicationLogger.getLogger().log(Level.SEVERE, "Falha ao carregar a tela " + fxmlPath, exception);
            showContentLoadFailure();
        }
    }

    private void showContentLoadFailure() {
        Label title = new Label("Não foi possível carregar esta tela.");
        title.getStyleClass().add("page-title");
        Label message = new Label("Tente abrir novamente. Se o problema persistir, envie o arquivo de log ao suporte.");
        message.setWrapText(true);
        message.getStyleClass().add("page-subtitle");
        VBox fallback = new VBox(10, title, message);
        fallback.getStyleClass().add("content-area");
        contentContainer.getChildren().setAll(fallback);
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
                creditButton,
                financeButton,
                reportsButton,
                settingsButton
        );
    }
}
