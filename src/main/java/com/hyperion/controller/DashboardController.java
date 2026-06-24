package com.hyperion.controller;

import com.hyperion.model.Product;
import com.hyperion.service.CustomerService;
import com.hyperion.service.ProductService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;

public class DashboardController {

    private final CustomerService customerService = new CustomerService();
    private final ProductService productService = new ProductService();

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

    private void loadDashboardData() {
        int activeCustomers = customerService.listActiveCustomers().size();
        List<Product> activeProducts = productService.listActiveProducts();

        customersValueLabel.setText(String.valueOf(activeCustomers));
        productsValueLabel.setText(String.valueOf(activeProducts.size()));
        balanceValueLabel.setText("R$ 0");

        customersFootnoteLabel.setText(activeCustomers == 1 ? "1 cliente ativo" : activeCustomers + " clientes ativos");
        productsFootnoteLabel.setText(activeProducts.size() == 1 ? "1 produto ativo" : activeProducts.size() + " produtos ativos");
    }
}
