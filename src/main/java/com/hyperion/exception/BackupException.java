package com.hyperion.exception;

public class BackupException extends InfrastructureException {
    public BackupException(String message, Throwable cause) { super(message, cause); }
    public BackupException(String message) { super(message); }
}
