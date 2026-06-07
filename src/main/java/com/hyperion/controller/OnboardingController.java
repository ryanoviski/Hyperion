package com.hyperion.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class OnboardingController {

    @FXML
    private TextField companyNameField;

    @FXML
    private TextField ownerNameField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleStart() {
        String companyName = companyNameField.getText().trim();
        String ownerName = ownerNameField.getText().trim();

        if (companyName.isBlank()) {
            showError("Informe o nome da empresa.");
            companyNameField.requestFocus();
            return;
        }

        if (ownerName.isBlank()) {
            showError("Informe o seu nome.");
            ownerNameField.requestFocus();
            return;
        }

        clearError();
        System.out.printf("Empresa: %s | Responsável: %s%n", companyName, ownerName);
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
