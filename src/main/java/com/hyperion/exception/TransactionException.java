package com.hyperion.exception;

public class TransactionException extends PersistenceException {
    public TransactionException(String message, Throwable cause) { super(message, cause); }
}
