package com.hyperion.service;

import com.hyperion.repository.AppSettingsRepository;
import com.hyperion.model.AppTheme;
import com.hyperion.model.PinAuthenticationResult;
import com.hyperion.model.PinLockState;
import com.hyperion.util.PinHashUtil;
import com.hyperion.exception.InvalidPinException;
import com.hyperion.exception.PinNotEnabledException;
import com.hyperion.exception.ValidationException;

import java.time.Duration;
import java.time.LocalDateTime;

public class AppSettingsService {

    private static final int MIN_PIN_LENGTH = 4;

    private final AppSettingsRepository appSettingsRepository = new AppSettingsRepository();

    public boolean isFirstRunCompleted() {
        return appSettingsRepository.isFirstRunCompleted();
    }

    public boolean isPinEnabled() {
        return appSettingsRepository.isPinEnabled();
    }

    public AppTheme getTheme() {
        return AppTheme.fromStorageValue(appSettingsRepository.findTheme());
    }

    public void updateTheme(AppTheme theme) {
        if (theme == null) {
            throw new ValidationException("Selecione um tema válido.");
        }

        appSettingsRepository.updateTheme(theme.getStorageValue());
    }

    public void completeFirstRunWithoutPin() {
        appSettingsRepository.completeFirstRunWithoutPin();
    }

    public void completeFirstRunWithPin(String pin) {
        validateNewPin(pin, pin);
        appSettingsRepository.completeFirstRunWithPin(PinHashUtil.hash(pin));
    }

    public boolean verifyPin(String pin) {
        return PinHashUtil.verify(pin, appSettingsRepository.findPinHash());
    }

    /**
     * Authenticates the unlock screen PIN and applies a persisted progressive
     * cooldown after each failed attempt. Administrative PIN changes keep
     * using {@link #verifyPin(String)} so a mistyped current PIN does not lock
     * the operator out of the settings screen.
     */
    public PinAuthenticationResult authenticatePin(String pin) {
        LocalDateTime now = LocalDateTime.now();
        PinLockState lockState = appSettingsRepository.findPinLockState();

        if (lockState.isLockedAt(now)) {
            return PinAuthenticationResult.denied(secondsUntil(lockState.lockedUntil(), now));
        }

        if (verifyPin(pin)) {
            appSettingsRepository.resetPinProtection();
            return PinAuthenticationResult.success();
        }

        int failures = lockState.failedAttempts() + 1;
        long delaySeconds = cooldownInSeconds(failures);
        appSettingsRepository.recordFailedPinAttempt(failures, now.plusSeconds(delaySeconds));
        return PinAuthenticationResult.denied(delaySeconds);
    }

    public void updatePin(String currentPin, String newPin, String confirmPin) {
        if (isPinEnabled() && !verifyPin(currentPin)) {
            throw new InvalidPinException();
        }

        validateNewPin(newPin, confirmPin);
        appSettingsRepository.updatePin(PinHashUtil.hash(newPin.trim()));
    }

    public void removePin(String currentPin) {
        if (!isPinEnabled()) {
            throw new PinNotEnabledException();
        }

        if (!verifyPin(currentPin)) {
            throw new InvalidPinException();
        }

        appSettingsRepository.removePin();
    }

    private void validateNewPin(String newPin, String confirmPin) {
        String normalizedPin = normalize(newPin);
        String normalizedConfirmation = normalize(confirmPin);

        if (normalizedPin.isBlank()) {
            throw new ValidationException("Informe o novo PIN.");
        }

        if (normalizedPin.length() < MIN_PIN_LENGTH) {
            throw new ValidationException("O PIN deve ter pelo menos 4 caracteres.");
        }

        if (!normalizedPin.equals(normalizedConfirmation)) {
            throw new ValidationException("Os PINs informados não são iguais.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private long cooldownInSeconds(int failures) {
        int exponent = Math.min(Math.max(failures, 1), 8);
        return Math.min(300, 1L << exponent);
    }

    private long secondsUntil(LocalDateTime lockedUntil, LocalDateTime now) {
        return Math.max(1, Duration.between(now, lockedUntil).toSeconds() + 1);
    }
}
