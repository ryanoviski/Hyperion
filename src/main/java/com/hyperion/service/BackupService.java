package com.hyperion.service;

import com.hyperion.config.DatabaseConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private static final Path BACKUP_DIRECTORY = Path.of("backups");
    private static final DateTimeFormatter BACKUP_FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public Path createDatabaseBackup() {
        Path databaseFile = DatabaseConfig.getDatabaseFile();

        if (!Files.exists(databaseFile)) {
            throw new IllegalStateException("Banco de dados local ainda não foi criado.");
        }

        try {
            Files.createDirectories(BACKUP_DIRECTORY);

            String fileName = "hyperion-backup-" + LocalDateTime.now().format(BACKUP_FILE_FORMAT) + ".db";
            Path backupFile = BACKUP_DIRECTORY.resolve(fileName);

            Files.copy(databaseFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            return backupFile;
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível criar o backup.", exception);
        }
    }

    public Path getBackupDirectory() {
        return BACKUP_DIRECTORY;
    }
}
