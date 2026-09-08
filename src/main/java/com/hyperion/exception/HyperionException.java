package com.hyperion.exception;

/** Base unchecked exception for failures that can be shown by the application UI. */
public abstract class HyperionException extends RuntimeException {

    protected HyperionException(String message) {
        super(message);
    }

    protected HyperionException(String message, Throwable cause) {
        super(message, cause);
    }
}
