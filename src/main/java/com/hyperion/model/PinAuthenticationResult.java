package com.hyperion.model;

public record PinAuthenticationResult(boolean authenticated, long retryAfterSeconds) {

    public static PinAuthenticationResult success() {
        return new PinAuthenticationResult(true, 0);
    }

    public static PinAuthenticationResult denied(long retryAfterSeconds) {
        return new PinAuthenticationResult(false, Math.max(0, retryAfterSeconds));
    }
}
