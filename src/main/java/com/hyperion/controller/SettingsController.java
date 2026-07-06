package com.hyperion.controller;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.service.BackupService;
import com.hyperion.service.AppSettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.nio.file.Path;

public class SettingsController {

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
    private Label databasePathLabel;

    @FXML
    private Label backupPathLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        updatePinStatus();
        databasePathLabel.setText(DatabaseConfig.getDatabaseFile().toAbsolutePath().toString());
        backupPathLabel.setText(backupService.getBackupDirectory().toAbsolutePath().toString());
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
            messageLabel.setText("PIN atualizado com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleRemovePin() {
        try {
            appSettingsService.removePin(currentPinField.getText());

            clearPinFields();
            updatePinStatus();
            messageLabel.setText("PIN removido com sucesso.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleCreateBackup() {
        try {
            Path backupFile = backupService.createDatabaseBackup();
            messageLabel.setText("Backup criado em: " + backupFile.toAbsolutePath());
        } catch (IllegalStateException exception) {
            messageLabel.setText(exception.getMessage());
        }
    }

    private void updatePinStatus() {
        pinStatusLabel.setText(appSettingsService.isPinEnabled() ? "PIN ativo" : "PIN desativado");
    }

    private void clearPinFields() {
        currentPinField.clear();
        newPinField.clear();
        confirmPinField.clear();
    }
}
