package com.hyperion.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class PinSetupController {

    private static final int MIN_PIN_LENGTH = 4;

    @FXML
    private PasswordField pinField;

    @FXML
    private PasswordField confirmPinField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleCreatePin() {
        String pin = pinField.getText().trim();
        String confirmPin = confirmPinField.getText().trim();

        if (pin.isBlank()) {
            showError("Informe um PIN.");
            pinField.requestFocus();
            return;
        }

        if (pin.length() < MIN_PIN_LENGTH) {
            showError("O PIN deve ter pelo menos 4 caracteres.");
            pinField.requestFocus();
            return;
        }

        if (confirmPin.isBlank()) {
            showError("Confirme o PIN.");
            confirmPinField.requestFocus();
            return;
        }

        if (!pin.equals(confirmPin)) {
            showError("Os PINs informados não são iguais.");
            confirmPinField.requestFocus();
            return;
        }

        clearError();
        System.out.println("PIN configurado com sucesso.");
    }

    @FXML
    private void handleSkip() {
        clearError();
        System.out.println("Configuração de PIN ignorada.");
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void clearError() {
        errorLabel.setText("");
    }
}
