package com.hyperion.exception;

public class EntityInactiveException extends BusinessRuleException {
    public EntityInactiveException(String entity) { super(entity + " está desativado(a)."); }
}
