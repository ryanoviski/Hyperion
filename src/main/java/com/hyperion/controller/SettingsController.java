package com.hyperion.controller;

import com.hyperion.exception.HyperionException;

import com.hyperion.model.AppTheme;
import com.hyperion.service.BackupService;
import com.hyperion.service.AppSettingsService;
import com.hyperion.util.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.FileChooser;

import java.io.File;
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
    private ChoiceBox<AppTheme> themeChoiceBox;

    @FXML
    private void initialize() {
        updatePinStatus();
        configureThemeChoice();
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
        } catch (HyperionException | IllegalArgumentException | IllegalStateException exception) {
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
        } catch (HyperionException | IllegalArgumentException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleCreateBackup() {
        try {
            Path backupFile = backupService.createDatabaseBackup();
            showMessage("Backup criado em: " + backupFile.toAbsolutePath());
        } catch (HyperionException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void handleRestoreBackup() {
        try {
            backupService.listBackups();
        } catch (HyperionException exception) {
            showMessage(exception.getMessage());
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar backup do Hyperion");
        fileChooser.setInitialDirectory(backupService.getBackupDirectory().toFile());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backups do Hyperion", "*.db"));

        File selectedFile = fileChooser.showOpenDialog(messageLabel.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Restaurar backup");
        confirmation.setHeaderText("Todos os dados atuais serão substituídos.");
        confirmation.setContentText("Um backup de segurança será criado antes da restauração. Deseja continuar?");
        confirmation.initOwner(messageLabel.getScene().getWindow());
        ThemeManager.applyTo(confirmation.getDialogPane());

        if (confirmation.showAndWait().filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        try {
            backupService.restoreDatabaseBackup(selectedFile.toPath());
            showMessage("Backup restaurado. Reinicie o Hyperion para carregar todos os dados restaurados.");
        } catch (HyperionException | IllegalStateException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void updatePinStatus() {
        boolean pinEnabled = appSettingsService.isPinEnabled();
        pinStatusLabel.setText(pinEnabled ? "PIN ativo" : "PIN desativado");
        pinStatusLabel.getStyleClass().removeAll(PIN_STATUS_ACTIVE_CLASS, PIN_STATUS_DISABLED_CLASS);
        pinStatusLabel.getStyleClass().add(pinEnabled ? PIN_STATUS_ACTIVE_CLASS : PIN_STATUS_DISABLED_CLASS);
    }

    private void configureThemeChoice() {
        AppTheme savedTheme = appSettingsService.getTheme();
        ThemeManager.setCurrentTheme(savedTheme);

        themeChoiceBox.getItems().setAll(AppTheme.DARK, AppTheme.LIGHT);
        themeChoiceBox.setValue(savedTheme);
        themeChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldTheme, newTheme) -> {
            if (newTheme == null || newTheme == oldTheme) {
                return;
            }

            try {
                appSettingsService.updateTheme(newTheme);
                ThemeManager.setCurrentTheme(newTheme);
                ThemeManager.applyTo(themeChoiceBox.getScene());
                showMessage("Tema alterado para " + newTheme + ".");
            } catch (HyperionException exception) {
                themeChoiceBox.setValue(oldTheme);
                showMessage(exception.getMessage());
            }
        });
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
