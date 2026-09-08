package com.hyperion.service;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.exception.BackupException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class BackupService {

    private static final int MAX_BACKUPS = 30;
    private static final DateTimeFormatter BACKUP_FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");

    public Path createDatabaseBackup() {
        Path databaseFile = DatabaseConfig.getDatabaseFile();

        if (!Files.exists(databaseFile)) {
            throw new BackupException("Banco de dados local ainda não foi criado.");
        }

        try {
            Files.createDirectories(getBackupDirectory());
            Path backupFile = getBackupDirectory().resolve(createBackupFileName());

            try (Connection connection = DatabaseConfig.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("VACUUM INTO '" + escapeSqlLiteral(backupFile) + "'");
            } catch (SQLException exception) {
                throw new BackupException("Não foi possível criar um backup consistente do banco de dados.", exception);
            }

            validateHyperionDatabase(backupFile);
            pruneOldBackups();
            return backupFile;
        } catch (IOException exception) {
            throw new BackupException("Não foi possível criar o backup.", exception);
        }
    }

    public List<Path> listBackups() {
        try {
            Files.createDirectories(getBackupDirectory());
            try (var files = Files.list(getBackupDirectory())) {
                return files
                        .filter(this::isBackupFile)
                        .sorted(Comparator.comparing(this::lastModified).reversed())
                        .toList();
            }
        } catch (IOException exception) {
            throw new BackupException("Não foi possível listar os backups.", exception);
        }
    }

    public void restoreDatabaseBackup(Path backupFile) {
        if (!isBackupFile(backupFile)) {
            throw new BackupException("Selecione um arquivo de backup válido.");
        }

        Path stagedFile = null;
        try {
            createDatabaseBackup();
            stagedFile = Files.createTempFile(DatabaseConfig.getDataDirectory(), "hyperion-restore-", ".db");
            Files.copy(backupFile, stagedFile, StandardCopyOption.REPLACE_EXISTING);
            validateHyperionDatabase(stagedFile);
            replaceDatabase(stagedFile);
        } catch (IOException exception) {
            throw new BackupException("Não foi possível restaurar o backup.", exception);
        } finally {
            if (stagedFile != null) {
                try {
                    Files.deleteIfExists(stagedFile);
                } catch (IOException ignored) {
                    // The staged file is harmless and can be removed on a later cleanup.
                }
            }
        }
    }

    public Path getBackupDirectory() {
        return DatabaseConfig.getDataDirectory().resolve("backups");
    }

    private void replaceDatabase(Path stagedFile) throws IOException {
        Path databaseFile = DatabaseConfig.getDatabaseFile();
        try {
            Files.move(stagedFile, databaseFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(stagedFile, databaseFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.deleteIfExists(Path.of(databaseFile + "-wal"));
        Files.deleteIfExists(Path.of(databaseFile + "-shm"));
    }

    private void validateHyperionDatabase(Path databaseFile) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("PRAGMA integrity_check")) {
                if (!resultSet.next() || !"ok".equalsIgnoreCase(resultSet.getString(1))) {
                    throw new BackupException("O arquivo selecionado não é um backup íntegro.");
                }
            }
            try (ResultSet resultSet = statement.executeQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'app_settings'")) {
                if (!resultSet.next()) {
                    throw new BackupException("O arquivo selecionado não pertence ao Hyperion.");
                }
            }
        } catch (SQLException exception) {
            throw new BackupException("Não foi possível validar o arquivo de backup.", exception);
        }
    }

    private void pruneOldBackups() {
        List<Path> backups = listBackups();
        for (int index = MAX_BACKUPS; index < backups.size(); index++) {
            try {
                Files.deleteIfExists(backups.get(index));
            } catch (IOException exception) {
                throw new BackupException("O backup foi criado, mas não foi possível aplicar a retenção de backups.", exception);
            }
        }
    }

    private boolean isBackupFile(Path file) {
        return file != null
                && Files.isRegularFile(file)
                && file.getFileName().toString().toLowerCase().endsWith(".db");
    }

    private String createBackupFileName() {
        return "hyperion-backup-" + LocalDateTime.now().format(BACKUP_FILE_FORMAT) + ".db";
    }

    private String escapeSqlLiteral(Path file) {
        return file.toAbsolutePath().toString().replace('\\', '/').replace("'", "''");
    }

    private java.nio.file.attribute.FileTime lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException exception) {
            throw new BackupException("Não foi possível ler a data de um backup.", exception);
        }
    }
}
