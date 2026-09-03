package com.hyperion.controller;

import com.hyperion.service.BackupService;
import com.hyperion.service.AppSettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.nio.file.Path;

public class SettingsController {

    private static final String PIN_STATUS_ACTIVE_CLASS = "pin-status-active";
    private static final String PIN_STATUS_DISABLED_CLASS = "pin-status-disabled";

    private final AppSettingsService appSettingsService = new AppSettingsService();
    private final BackupService backupService = new BackupService();

    @FXML
    private Label pinStatusLabel;

    @FXML
    private PasswordField currentPinField;

    @FXML
    private PasswordField newPinField;

    @FXML
    private PasswordField confirmPinField;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        updatePinStatus();
    }

    @FXML
    private void handleSavePin() {
        try {
            appSettingsService.updatePin(
                    currentPinField.getText(),
                    newPinField.getText(),
                    confirmPinField.getText()
            );

            clearPinFields();
            updatePinStatus();
            showMessage("PIN atualizado com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleRemovePin() {
        try {
            appSettingsService.removePin(currentPinField.getText());

            clearPinFields();
            updatePinStatus();
            showMessage("PIN removido com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleCreateBackup() {
        try {
            Path backupFile = backupService.createDatabaseBackup();
            showMessage("Backup criado em: " + backupFile.toAbsolutePath());
        } catch (IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void updatePinStatus() {
        boolean pinEnabled = appSettingsService.isPinEnabled();
        pinStatusLabel.setText(pinEnabled ? "PIN ativo" : "PIN desativado");
        pinStatusLabel.getStyleClass().removeAll(PIN_STATUS_ACTIVE_CLASS, PIN_STATUS_DISABLED_CLASS);
        pinStatusLabel.getStyleClass().add(pinEnabled ? PIN_STATUS_ACTIVE_CLASS : PIN_STATUS_DISABLED_CLASS);
    }

    private void clearPinFields() {
        currentPinField.clear();
        newPinField.clear();
        confirmPinField.clear();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

}
