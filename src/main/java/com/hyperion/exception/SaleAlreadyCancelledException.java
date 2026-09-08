package com.hyperion.exception;

public class SaleAlreadyCancelledException extends BusinessRuleException {
    public SaleAlreadyCancelledException() {
        super("Esta venda já foi cancelada.");
    }
}
