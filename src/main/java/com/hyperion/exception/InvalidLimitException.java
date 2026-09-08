package com.hyperion.exception;

public class InvalidLimitException extends ValidationException {
    public InvalidLimitException() { super("Informe uma quantidade de resultados maior que zero."); }
}
