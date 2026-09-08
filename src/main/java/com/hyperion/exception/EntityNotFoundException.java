package com.hyperion.exception;

public class EntityNotFoundException extends BusinessRuleException {
    public EntityNotFoundException(String entity) { super(entity + " não foi encontrado(a)."); }
}
