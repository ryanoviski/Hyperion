package com.hyperion.exception;

public class SaleCancellationNotAllowedException extends BusinessRuleException {
    public SaleCancellationNotAllowedException() {
        super("Não é possível cancelar uma venda com parcelas já pagas.");
    }
}
