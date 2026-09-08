package com.hyperion.service;

import com.hyperion.model.PinAuthenticationResult;
import com.hyperion.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsSecurityIntegrationTest extends DatabaseIntegrationTest {

    @Test
    void recordsProgressiveCooldownAfterFailedUnlock() {
        AppSettingsService settingsService = new AppSettingsService();
        settingsService.completeFirstRunWithPin("1234");

        PinAuthenticationResult firstFailure = settingsService.authenticatePin("0000");
        assertFalse(firstFailure.authenticated());
        assertTrue(firstFailure.retryAfterSeconds() >= 2);

        PinAuthenticationResult lockedAttempt = settingsService.authenticatePin("1234");
        assertFalse(lockedAttempt.authenticated());
        assertTrue(lockedAttempt.retryAfterSeconds() >= 1);
    }
}
