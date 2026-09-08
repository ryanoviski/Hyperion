package com.hyperion.exception;

public class InvalidDateRangeException extends ValidationException {
    public InvalidDateRangeException() { super("O período informado é inválido."); }
}
