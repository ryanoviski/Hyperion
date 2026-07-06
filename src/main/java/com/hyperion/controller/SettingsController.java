package com.hyperion.controller;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.service.BackupService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.nio.file.Path;

public class SettingsController {

    private final BackupService backupService = new BackupService();

    @FXML
    private Label databasePathLabel;

    @FXML
    private Label backupPathLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private void initialize() {
        databasePathLabel.setText(DatabaseConfig.getDatabaseFile().toAbsolutePath().toString());
        backupPathLabel.setText(backupService.getBackupDirectory().toAbsolutePath().toString());
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
}
