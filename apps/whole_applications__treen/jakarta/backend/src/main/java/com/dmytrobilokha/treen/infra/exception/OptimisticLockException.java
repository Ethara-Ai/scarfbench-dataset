package com.dmytrobilokha.treen.infra.exception;

public class OptimisticLockException extends InvalidInputException {

    public OptimisticLockException(String message) {
        super(message);
    }

}
