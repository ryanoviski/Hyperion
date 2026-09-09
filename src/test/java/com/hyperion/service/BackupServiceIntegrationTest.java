package com.hyperion.service;

import com.hyperion.model.Attachment;
import com.hyperion.exception.BackupException;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupServiceIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void restoresDatabaseAndAttachmentsToTheStateCapturedByBackup() throws Exception {
        CustomerService customerService = new CustomerService();
        customerService.createCustomer("Cliente antes do backup", "", "", "", "", "");

        Long expenseId = new FinanceService().registerExpense("Internet", "Serviços", new BigDecimal("89.90"));
        Path sourceFile = testDirectory.resolve("comprovante.pdf");
        Files.writeString(sourceFile, "%PDF-1.4\ncomprovante de backup");
        AttachmentService attachmentService = new AttachmentService();
        attachmentService.attachFile(AttachmentService.FINANCE_MODULE, expenseId, sourceFile);
        Attachment attachment = attachmentService.listAttachments(AttachmentService.FINANCE_MODULE, expenseId).getFirst();
        Path storedFile = attachmentService.resolveAttachmentPath(attachment);

        BackupService backupService = new BackupService();
        Path backupFile = backupService.createDatabaseBackup();
        assertTrue(Files.isRegularFile(backupFile));
        assertTrue(backupFile.getFileName().toString().endsWith(".zip"));
        try (ZipFile backup = new ZipFile(backupFile.toFile())) {
            assertTrue(backup.getEntry("database/hyperion.db") != null);
            assertTrue(backup.getEntry("attachments/finance/" + expenseId + "/" + attachment.getStoredName()) != null);
        }

        customerService.createCustomer("Cliente depois do backup", "", "", "", "", "");
        Files.delete(storedFile);
        assertEquals(2, customerService.listActiveCustomers().size());

        BackupService.RestoreResult result = backupService.restoreDatabaseBackup(backupFile);

        assertTrue(result.attachmentsRestored());
        assertEquals(1, new CustomerService().listActiveCustomers().size());
        assertEquals("Cliente antes do backup", new CustomerService().listActiveCustomers().getFirst().getName());
        Attachment restoredAttachment = new AttachmentService()
                .listAttachments(AttachmentService.FINANCE_MODULE, expenseId)
                .getFirst();
        assertTrue(Files.isRegularFile(new AttachmentService().resolveAttachmentPath(restoredAttachment)));
        assertTrue(backupService.listBackups().size() >= 2);
    }

    @Test
    void rejectsAttachmentEntriesThatEscapeTheAttachmentsDirectory() throws Exception {
        CustomerService customerService = new CustomerService();
        customerService.createCustomer("Cliente do backup", "", "", "", "", "");
        BackupService backupService = new BackupService();
        Path validBackup = backupService.createDatabaseBackup();
        Path tamperedBackup = testDirectory.resolve("backup-adulterado.zip");

        try (ZipFile source = new ZipFile(validBackup.toFile());
             ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(tamperedBackup))) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                output.putNextEntry(new ZipEntry(entry.getName()));
                if (!entry.isDirectory()) {
                    try (var input = source.getInputStream(entry)) {
                        input.transferTo(output);
                    }
                }
                output.closeEntry();
            }
            output.putNextEntry(new ZipEntry("attachments/../database/altered.db"));
            output.write("conteúdo adulterado".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }

        assertThrows(BackupException.class, () -> backupService.restoreDatabaseBackup(tamperedBackup));
    }

    @Test
    void clearsCurrentAttachmentsWhenRestoringALegacyDatabaseOnlyBackup() throws Exception {
        Long expenseId = new FinanceService().registerExpense("Telefone", "Serviços", new BigDecimal("50.00"));
        Path sourceFile = testDirectory.resolve("comprovante.pdf");
        Files.writeString(sourceFile, "%PDF-1.4\ncomprovante de backup legado");
        AttachmentService attachmentService = new AttachmentService();
        attachmentService.attachFile(AttachmentService.FINANCE_MODULE, expenseId, sourceFile);

        Path legacyBackup = testDirectory.resolve("backup-legado.db");
        Files.copy(com.hyperion.config.DatabaseConfig.getDatabaseFile(), legacyBackup);

        BackupService.RestoreResult result = new BackupService().restoreDatabaseBackup(legacyBackup);

        assertTrue(!result.attachmentsRestored());
        try (var files = Files.walk(new AttachmentService().getAttachmentsDirectory())) {
            assertEquals(0, files.filter(Files::isRegularFile).count());
        }
    }
}
