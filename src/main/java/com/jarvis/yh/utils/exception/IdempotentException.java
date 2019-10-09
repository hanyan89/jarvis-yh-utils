package com.jarvis.yh.utils.exception;

public class IdempotentException extends RuntimeException {

    public String message;

    public IdempotentException(String message) {
        super();
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
