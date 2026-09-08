package com.hyperion.exception;

public class PinNotEnabledException extends BusinessRuleException {
    public PinNotEnabledException() { super("Nenhum PIN está ativo."); }
}
