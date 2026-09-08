package com.hyperion.service;

import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupServiceIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void restoresDatabaseToTheStateCapturedByBackup() {
        CustomerService customerService = new CustomerService();
        customerService.createCustomer("Cliente antes do backup", "", "", "", "", "");

        BackupService backupService = new BackupService();
        Path backupFile = backupService.createDatabaseBackup();
        assertTrue(Files.isRegularFile(backupFile));

        customerService.createCustomer("Cliente depois do backup", "", "", "", "", "");
        assertEquals(2, customerService.listActiveCustomers().size());

        backupService.restoreDatabaseBackup(backupFile);

        assertEquals(1, new CustomerService().listActiveCustomers().size());
        assertEquals("Cliente antes do backup", new CustomerService().listActiveCustomers().getFirst().getName());
        assertTrue(backupService.listBackups().size() >= 2);
    }
}
