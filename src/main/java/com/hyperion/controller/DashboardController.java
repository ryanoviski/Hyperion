package com.hyperion.controller;

import com.hyperion.model.DailySalesSummary;
import com.hyperion.model.Product;
import com.hyperion.service.CustomerService;
import com.hyperion.service.ProductService;
import com.hyperion.service.SaleService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final CustomerService customerService = new CustomerService();
    private final ProductService productService = new ProductService();
    private final SaleService saleService = new SaleService();

    @FXML
    private Label salesTodayValueLabel;

    @FXML
    private Label salesTodayFootnoteLabel;

    @FXML
    private Label customersValueLabel;

    @FXML
    private Label productsValueLabel;

    @FXML
    private Label balanceValueLabel;

    @FXML
    private Label customersFootnoteLabel;

    @FXML
    private Label productsFootnoteLabel;

    @FXML
    private void initialize() {
        loadDashboardData();
    }

    @FXML
    private void handleCustomers() {
        MainController.openCustomersView();
    }

    @FXML
    private void handleProducts() {
        MainController.openProductsView();
    }

    @FXML
    private void handleStock() {
        MainController.openStockView();
    }

    @FXML
    private void handleSales() {
        MainController.openSalesView();
    }

    private void loadDashboardData() {
        int activeCustomers = customerService.listActiveCustomers().size();
        List<Product> activeProducts = productService.listActiveProducts();
        DailySalesSummary dailySalesSummary = saleService.getTodaySummary();

        salesTodayValueLabel.setText(MONEY_FORMAT.format(dailySalesSummary.getTotal()));
        customersValueLabel.setText(String.valueOf(activeCustomers));
        productsValueLabel.setText(String.valueOf(activeProducts.size()));
        balanceValueLabel.setText("R$ 0");

        salesTodayFootnoteLabel.setText(dailySalesSummary.getSalesCount() == 1
                ? "1 venda registrada hoje"
                : dailySalesSummary.getSalesCount() + " vendas registradas hoje");
        customersFootnoteLabel.setText(activeCustomers == 1 ? "1 cliente ativo" : activeCustomers + " clientes ativos");
        productsFootnoteLabel.setText(activeProducts.size() == 1 ? "1 produto ativo" : activeProducts.size() + " produtos ativos");
    }
}
