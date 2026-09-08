package com.hyperion.exception;

public class CompanyAlreadyRegisteredException extends BusinessRuleException {
    public CompanyAlreadyRegisteredException() { super("A empresa inicial já foi cadastrada."); }
}
