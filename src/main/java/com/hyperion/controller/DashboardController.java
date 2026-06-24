package com.hyperion.controller;

import javafx.fxml.FXML;

public class DashboardController {

    @FXML
    private void handleCustomers() {
        MainController.openCustomersView();
    }

    @FXML
    private void handleProducts() {
        MainController.openProductsView();
    }
}
