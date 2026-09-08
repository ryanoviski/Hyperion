package com.hyperion.exception;

public class InvalidPaymentMethodException extends ValidationException {
    public InvalidPaymentMethodException() { super("Selecione uma forma de pagamento válida."); }
}
