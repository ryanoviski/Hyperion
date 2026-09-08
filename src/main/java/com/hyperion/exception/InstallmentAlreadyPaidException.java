package com.hyperion.exception;

public class InstallmentAlreadyPaidException extends BusinessRuleException {
    public InstallmentAlreadyPaidException() { super("A parcela já foi paga ou não está mais disponível para pagamento."); }
}
