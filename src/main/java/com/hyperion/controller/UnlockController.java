package com.hyperion.controller;

import com.hyperion.service.AppSettingsService;
import com.hyperion.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class UnlockController {

    private final AppSettingsService appSettingsService = new AppSettingsService();

    @FXML
    private PasswordField pinField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleUnlock() {
        String pin = pinField.getText().trim();

        if (pin.isBlank()) {
            showError("Informe o PIN.");
            pinField.requestFocus();
            return;
        }

        if (!appSettingsService.verifyPin(pin)) {
            showError("PIN inválido.");
            pinField.clear();
            pinField.requestFocus();
            return;
        }

        clearError();
        SceneManager.switchTo("/fxml/main-view.fxml");
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
