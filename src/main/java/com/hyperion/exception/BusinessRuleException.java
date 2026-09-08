package com.hyperion.exception;

public class BusinessRuleException extends HyperionException {
    public BusinessRuleException(String message) { super(message); }
    public BusinessRuleException(String message, Throwable cause) { super(message, cause); }
}
