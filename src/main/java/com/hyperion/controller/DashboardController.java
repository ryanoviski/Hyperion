package com.hyperion.controller;

import com.hyperion.util.SceneManager;
import javafx.fxml.FXML;

public class DashboardController {

    @FXML
    private void handleCustomers() {
        SceneManager.switchTo("/fxml/customers-view.fxml");
    }
}
