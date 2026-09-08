package com.hyperion.service;

import com.hyperion.model.Attachment;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
