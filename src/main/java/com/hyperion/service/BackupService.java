package com.hyperion.service;

import com.hyperion.config.DatabaseConfig;
import com.hyperion.exception.BackupException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Creates self-contained backups with the SQLite database and financial attachments. */
public class BackupService {

    private static final int MAX_BACKUPS = 30;
    private static final String PACKAGE_EXTENSION = ".zip";
    private static final String LEGACY_DATABASE_EXTENSION = ".db";
    private static final String MANIFEST_ENTRY = "hyperion-backup.properties";
    private static final String DATABASE_ENTRY = "database/hyperion.db";
    private static final String ATTACHMENTS_PREFIX = "attachments/";
    private static final String BACKUP_FORMAT_VERSION = "2";
    private static final int MAX_ARCHIVE_ENTRIES = 10_000;
    private static final long MAX_ARCHIVE_ENTRY_SIZE_BYTES = 512L * 1024 * 1024;
    private static final long MAX_ARCHIVE_UNCOMPRESSED_SIZE_BYTES = 2L * 1024 * 1024 * 1024;
    private static final long MAX_MANIFEST_SIZE_BYTES = 64L * 1024;
    private static final DateTimeFormatter BACKUP_FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final Set<String> REQUIRED_TABLES = Set.of(
            "app_settings", "customers", "products", "sales", "sale_items",
            "stock_movements", "credit_installments", "expenses", "attachments"
    );

    public Path createDatabaseBackup() {
        Path databaseFile = DatabaseConfig.getDatabaseFile();
        if (!Files.exists(databaseFile)) {
            throw new BackupException("Banco de dados local ainda não foi criado.");
        }

        Path snapshot = null;
        Path temporaryPackage = null;
        try {
            Files.createDirectories(getBackupDirectory());
            snapshot = createSnapshotFile();
            createDatabaseSnapshot(snapshot);
            validateHyperionDatabase(snapshot);

            Path backupFile = getBackupDirectory().resolve(createBackupFileName());
            temporaryPackage = Files.createTempFile(getBackupDirectory(), "hyperion-backup-", ".zip");
            writeBackupPackage(temporaryPackage, snapshot);
            validateBackupPackage(temporaryPackage);
            moveReplacing(temporaryPackage, backupFile);
            temporaryPackage = null;
            pruneOldBackups();
            return backupFile;
        } catch (IOException exception) {
            throw new BackupException("Não foi possível criar o backup.", exception);
        } finally {
            deleteQuietly(snapshot);
            deleteQuietly(temporaryPackage);
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

    /** Restores a self-contained .zip package or a database-only legacy .db backup. */
    public RestoreResult restoreDatabaseBackup(Path backupFile) {
        if (!isBackupFile(backupFile)) {
            throw new BackupException("Selecione um arquivo de backup válido.");
        }

        Path stagingDirectory = null;
        try {
            createDatabaseBackup();
            stagingDirectory = Files.createTempDirectory(DatabaseConfig.getDataDirectory(), "hyperion-restore-");
            RestoreCandidate candidate = prepareRestoreCandidate(backupFile, stagingDirectory);
            validateHyperionDatabase(candidate.databaseFile());
            replaceDatabaseAndAttachments(candidate);
            return new RestoreResult(candidate.includesAttachments());
        } catch (IOException exception) {
            throw new BackupException("Não foi possível restaurar o backup.", exception);
        } finally {
            deleteDirectoryQuietly(stagingDirectory);
        }
    }

    public Path getBackupDirectory() {
        return DatabaseConfig.getDataDirectory().resolve("backups");
    }

    private Path createSnapshotFile() throws IOException {
        Path snapshot = Files.createTempFile(DatabaseConfig.getDataDirectory(), "hyperion-backup-", ".db");
        Files.deleteIfExists(snapshot);
        return snapshot;
    }

    private void createDatabaseSnapshot(Path snapshot) {
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + escapeSqlLiteral(snapshot) + "'");
        } catch (SQLException exception) {
            throw new BackupException("Não foi possível criar um backup consistente do banco de dados.", exception);
        }
    }

    private void writeBackupPackage(Path packageFile, Path databaseSnapshot) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(packageFile))) {
            Properties manifest = new Properties();
            manifest.setProperty("format.version", BACKUP_FORMAT_VERSION);
            manifest.setProperty("application", "Hyperion");
            manifest.setProperty("created.at", LocalDateTime.now().toString());
            output.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
            manifest.store(output, "Hyperion backup");
            output.closeEntry();

            writeFileEntry(output, DATABASE_ENTRY, databaseSnapshot);
            writeAttachmentEntries(output);
        }
    }

    private void writeAttachmentEntries(ZipOutputStream output) throws IOException {
        Path attachmentsDirectory = getAttachmentsDirectory();
        if (!Files.isDirectory(attachmentsDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        try (var files = Files.walk(attachmentsDirectory)) {
            for (Path file : files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
                String entryName = ATTACHMENTS_PREFIX
                        + attachmentsDirectory.relativize(file).toString().replace('\\', '/');
                writeFileEntry(output, entryName, file);
            }
        }
    }

    private void writeFileEntry(ZipOutputStream output, String entryName, Path file) throws IOException {
        output.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, output);
        output.closeEntry();
    }

    private RestoreCandidate prepareRestoreCandidate(Path backupFile, Path stagingDirectory) throws IOException {
        if (isPackageFile(backupFile)) {
            return extractBackupPackage(backupFile, stagingDirectory);
        }

        Path databaseFile = stagingDirectory.resolve(DATABASE_ENTRY);
        Files.createDirectories(databaseFile.getParent());
        Files.copy(backupFile, databaseFile, StandardCopyOption.REPLACE_EXISTING);
        return new RestoreCandidate(databaseFile, null, false);
    }

    private RestoreCandidate extractBackupPackage(Path packageFile, Path stagingDirectory) throws IOException {
        boolean databaseFound = false;
        boolean manifestFound = false;
        int entryCount = 0;
        long extractedBytes = 0;
        Properties manifest = new Properties();
        Set<String> entryNames = new HashSet<>();

        try (ZipFile archive = new ZipFile(packageFile.toFile())) {
            var entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                if (++entryCount > MAX_ARCHIVE_ENTRIES
                        || !entryNames.add(entryName)
                        || entry.getSize() > MAX_ARCHIVE_ENTRY_SIZE_BYTES) {
                    throw new BackupException("O pacote de backup excede os limites de segurança permitidos.");
                }
                if (MANIFEST_ENTRY.equals(entryName)) {
                    if (entry.getSize() > MAX_MANIFEST_SIZE_BYTES) {
                        throw new BackupException("O manifesto do pacote de backup é inválido.");
                    }
                    try (InputStream input = archive.getInputStream(entry)) {
                        manifest.load(input);
                    }
                    manifestFound = true;
                    continue;
                }

                if (!DATABASE_ENTRY.equals(entryName) && !isAttachmentEntry(entryName)) {
                    throw new BackupException("O pacote de backup possui conteúdo não reconhecido.");
                }

                Path destination = resolveArchiveEntry(stagingDirectory, entryName);
                Files.createDirectories(destination.getParent());
                try (InputStream input = archive.getInputStream(entry)) {
                    extractedBytes += Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                if (extractedBytes > MAX_ARCHIVE_UNCOMPRESSED_SIZE_BYTES) {
                    throw new BackupException("O pacote de backup excede os limites de segurança permitidos.");
                }
                databaseFound |= DATABASE_ENTRY.equals(entryName);
            }
        }

        if (!manifestFound
                || !BACKUP_FORMAT_VERSION.equals(manifest.getProperty("format.version"))
                || !"Hyperion".equals(manifest.getProperty("application"))
                || !databaseFound) {
            throw new BackupException("O arquivo selecionado não é um pacote de backup válido do Hyperion.");
        }

        Path attachmentsDirectory = stagingDirectory.resolve("attachments");
        Files.createDirectories(attachmentsDirectory);
        return new RestoreCandidate(stagingDirectory.resolve(DATABASE_ENTRY), attachmentsDirectory, true);
    }

    private Path resolveArchiveEntry(Path stagingDirectory, String entryName) {
        Path destination = stagingDirectory.resolve(entryName).normalize();
        Path allowedRoot = DATABASE_ENTRY.equals(entryName)
                ? stagingDirectory.resolve("database")
                : stagingDirectory.resolve("attachments");
        if (!destination.startsWith(allowedRoot)) {
            throw new BackupException("O pacote de backup possui um caminho de arquivo inválido.");
        }
        return destination;
    }

    private boolean isAttachmentEntry(String entryName) {
        return entryName.startsWith(ATTACHMENTS_PREFIX)
                && entryName.length() > ATTACHMENTS_PREFIX.length()
                && entryName.indexOf('\\') < 0
                && !Path.of(entryName).isAbsolute()
                && !Path.of(entryName).normalize().startsWith("..");
    }

    private void replaceDatabaseAndAttachments(RestoreCandidate candidate) throws IOException {
        Path previousAttachments = null;
        Path currentAttachments = getAttachmentsDirectory();
        boolean attachmentsReplaced = false;

        try {
            if (Files.exists(currentAttachments)) {
                previousAttachments = DatabaseConfig.getDataDirectory()
                        .resolve("attachments-before-restore-" + UUID.randomUUID());
                moveReplacing(currentAttachments, previousAttachments);
            }
            if (candidate.includesAttachments()) {
                moveReplacing(candidate.attachmentsDirectory(), currentAttachments);
            } else {
                Files.createDirectories(currentAttachments);
            }
            attachmentsReplaced = true;

            replaceDatabase(candidate.databaseFile());
            deleteDirectoryQuietly(previousAttachments);
        } catch (IOException exception) {
            if (attachmentsReplaced) {
                deleteDirectoryQuietly(currentAttachments);
                if (previousAttachments != null && Files.exists(previousAttachments)) {
                    try {
                        moveReplacing(previousAttachments, currentAttachments);
                    } catch (IOException rollbackException) {
                        exception.addSuppressed(rollbackException);
                    }
                }
            }
            throw exception;
        }
    }

    private void replaceDatabase(Path stagedFile) throws IOException {
        Path databaseFile = DatabaseConfig.getDatabaseFile();
        moveReplacing(stagedFile, databaseFile);
        Files.deleteIfExists(Path.of(databaseFile + "-wal"));
        Files.deleteIfExists(Path.of(databaseFile + "-shm"));
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void validateBackupPackage(Path packageFile) {
        try (ZipFile archive = new ZipFile(packageFile.toFile())) {
            ZipEntry manifestEntry = archive.getEntry(MANIFEST_ENTRY);
            ZipEntry databaseEntry = archive.getEntry(DATABASE_ENTRY);
            if (manifestEntry == null || databaseEntry == null) {
                throw new BackupException("O pacote de backup foi criado sem os arquivos obrigatórios.");
            }
            Properties manifest = new Properties();
            try (InputStream input = archive.getInputStream(manifestEntry)) {
                manifest.load(input);
            }
            if (!BACKUP_FORMAT_VERSION.equals(manifest.getProperty("format.version"))
                    || !"Hyperion".equals(manifest.getProperty("application"))) {
                throw new BackupException("O pacote de backup foi criado em um formato inválido.");
            }
        } catch (IOException exception) {
            throw new BackupException("Não foi possível validar o pacote de backup.", exception);
        }
    }

    private void validateHyperionDatabase(Path databaseFile) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("PRAGMA integrity_check")) {
                if (!resultSet.next() || !"ok".equalsIgnoreCase(resultSet.getString(1))) {
                    throw new BackupException("O arquivo selecionado não é um backup íntegro.");
                }
            }
            try (ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
                Set<String> tables = new java.util.HashSet<>();
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
                if (!tables.containsAll(REQUIRED_TABLES)) {
                    throw new BackupException("O arquivo selecionado não pertence a uma instalação compatível do Hyperion.");
                }
            }
            try (ResultSet resultSet = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (resultSet.next()) {
                    throw new BackupException("O arquivo selecionado possui referências inválidas e não pode ser restaurado.");
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

    private Path getAttachmentsDirectory() {
        return DatabaseConfig.getDataDirectory().resolve("attachments").toAbsolutePath().normalize();
    }

    private boolean isBackupFile(Path file) {
        return file != null
                && Files.isRegularFile(file)
                && (isPackageFile(file) || file.getFileName().toString().toLowerCase().endsWith(LEGACY_DATABASE_EXTENSION));
    }

    private boolean isPackageFile(Path file) {
        return file != null && file.getFileName().toString().toLowerCase().endsWith(PACKAGE_EXTENSION);
    }

    private String createBackupFileName() {
        return "hyperion-backup-" + LocalDateTime.now().format(BACKUP_FILE_FORMAT) + PACKAGE_EXTENSION;
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

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Temporary files are retried on a later backup operation.
        }
    }

    private void deleteDirectoryQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        Path dataDirectory = DatabaseConfig.getDataDirectory().toAbsolutePath().normalize();
        Path normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!normalizedDirectory.startsWith(dataDirectory) || normalizedDirectory.equals(dataDirectory)) {
            return;
        }
        try (var paths = Files.walk(normalizedDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Staged files are harmless and can be removed by a later cleanup.
        }
    }

    private record RestoreCandidate(Path databaseFile, Path attachmentsDirectory, boolean includesAttachments) {
    }

    public record RestoreResult(boolean attachmentsRestored) {
    }
}
