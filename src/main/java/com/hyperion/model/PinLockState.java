package com.hyperion.model;

import java.time.LocalDateTime;

public record PinLockState(int failedAttempts, LocalDateTime lockedUntil) {

    public boolean isLockedAt(LocalDateTime instant) {
        return lockedUntil != null && lockedUntil.isAfter(instant);
    }
}
