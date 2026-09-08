package com.hyperion.exception;

public class ApplicationResourceException extends InfrastructureException {
    public ApplicationResourceException(String message) { super(message); }
    public ApplicationResourceException(String message, Throwable cause) { super(message, cause); }
}
