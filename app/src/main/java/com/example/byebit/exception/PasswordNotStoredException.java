package com.example.byebit.exception;

public class PasswordNotStoredException extends RuntimeException {
    public PasswordNotStoredException() {
        super("Password was not stored in the entity.");
    }

    public PasswordNotStoredException(String message) {
        super(message);
    }

    public PasswordNotStoredException(String message, Throwable cause) {
        super(message, cause);
    }
}
