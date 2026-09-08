package com.hyperion.service;

import com.hyperion.model.Attachment;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentServiceIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void storesListsResolvesAndDeletesAttachment() throws Exception {
        Path sourceFile = testDirectory.resolve("comprovante.pdf");
        Files.writeString(sourceFile, "comprovante de teste");
        Long expenseId = new FinanceService().registerExpense("Internet", "Serviços", new BigDecimal("89.90"));
        AttachmentService attachmentService = new AttachmentService();

        attachmentService.attachFile(AttachmentService.FINANCE_MODULE, expenseId, sourceFile);

        assertEquals(1, attachmentService.countAttachments(AttachmentService.FINANCE_MODULE, expenseId));
        Attachment attachment = attachmentService.listAttachments(AttachmentService.FINANCE_MODULE, expenseId).getFirst();
        Path storedFile = attachmentService.resolveAttachmentPath(attachment);
        assertTrue(Files.isRegularFile(storedFile));
        assertTrue(storedFile.startsWith(testDirectory.resolve("data").toAbsolutePath()));

        attachmentService.deleteByEntity(AttachmentService.FINANCE_MODULE, expenseId);

        assertEquals(0, attachmentService.countAttachments(AttachmentService.FINANCE_MODULE, expenseId));
        assertTrue(Files.notExists(storedFile));
    }
}
