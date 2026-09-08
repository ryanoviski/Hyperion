package com.hyperion.exception;

public class InvalidPinException extends ValidationException {
    public InvalidPinException() { super("PIN atual inválido."); }
}
