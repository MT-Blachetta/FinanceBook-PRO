package com.financebook.exception;

/**
 * Exception thrown when business logic validation fails.
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
}
